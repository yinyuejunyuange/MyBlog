package org.oyyj.taskservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.taskservice.dto.AnnouncementUserDTO;
import org.oyyj.taskservice.dto.PageDTO;
import org.oyyj.taskservice.feign.AdminFeign;
import org.oyyj.taskservice.pojo.Announcement;
import org.oyyj.taskservice.pojo.AnnouncementTask;
import org.oyyj.taskservice.pojo.AnnouncementUser;
import org.oyyj.taskservice.service.IAnnouncementService;
import org.oyyj.taskservice.service.IAnnouncementTaskService;
import org.oyyj.taskservice.service.IAnnouncementUserService;
import org.oyyj.taskservice.utils.ResultUtil;
import org.oyyj.taskservice.vo.AnnouncementAdminVO;
import org.oyyj.taskservice.vo.AnnouncementUpdateVO;
import org.oyyj.taskservice.vo.AnnouncementVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/task/announce")
public class AnnouncementController {

    @Autowired
    private IAnnouncementUserService announcementUserService;

    @Autowired
    private IAnnouncementService announcementService;

    @Autowired
    private AdminFeign adminFeign;

    @Autowired
    private IAnnouncementTaskService announcementTaskService;


    @GetMapping("/getAnnouncementByUser")
    public List<AnnouncementUserDTO> getAnnouncementByUser(@RequestParam("userId") Long userId,
                                                           @RequestParam("currentIndex") Integer currentIndex,
                                                           @RequestParam("createTime") Date createTime) {
        try {

            IPage<Announcement> page = new Page<>(currentIndex,20);
            System.out.println(currentIndex);

            List<Long> list = announcementUserService.list(Wrappers.<AnnouncementUser>lambdaQuery()
                            .eq(AnnouncementUser::getUserId, userId)
                    )
                    .stream().map(AnnouncementUser::getAnnouncementId).toList();
            return announcementService.list(page,Wrappers.<Announcement>lambdaQuery()
                    .ge(Announcement::getCreateTime,createTime)

            ).stream().map(i -> AnnouncementUserDTO.builder()
                    .isUserRead(list.contains(i.getId()))
                    .id(String.valueOf(i.getId()))
                    .adminId(i.getAdminId())
                    .title(i.getTitle())
                    .content(i.getContent())
                    .updateTime(i.getUpdateTime())
                    .build()).toList();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PutMapping("/readAnnouncement")
    public boolean readAnnouncement(@RequestParam("userId")Long userId,@RequestParam("announcementId") Long announcementId ){
        Announcement one = announcementService.getOne(Wrappers.<Announcement>lambdaQuery().eq(Announcement::getId, announcementId));
        if(Objects.nonNull(one)){

            AnnouncementUser build = AnnouncementUser.builder()
                    .announcementId(announcementId)
                    .userId(userId)
                    .build();

            boolean save = announcementUserService.save(build);
            return save;
        }else{
            throw new IllegalArgumentException("数据异常");
        }
    }


    @GetMapping("/getAnnouncementAdmin")
    public PageDTO<AnnouncementAdminVO> getAnnouncementAdmin(@RequestParam(value = "title",required = false)String title ,
                                                      @RequestParam(value = "admin",required = false) String admin,
                                                      @RequestParam(value = "taskName",required = false) String taskName,
                                                      @RequestParam(value = "startTime",required = false) Date startTime,
                                                      @RequestParam(value = "endTime",required = false) Date endTime,
                                                      @RequestParam("currentPage") Integer currentPage) {

        IPage<Announcement> iPage=new Page<>(currentPage,20);
        LambdaQueryWrapper<Announcement> lqw=new LambdaQueryWrapper<>();
        if(Objects.nonNull(title)){
            lqw.like(Announcement::getTitle,title);
        }
        if(Objects.nonNull(admin)){
            List<Long> adminIdByNameOrPhone = adminFeign.getAdminIdByNameOrPhone(admin);
            if(adminIdByNameOrPhone.isEmpty()){
                return null;
            }else{
                lqw.in(Announcement::getAdminId,adminIdByNameOrPhone);
            }
        }
        if(Objects.nonNull(startTime)){
            lqw.ge(Announcement::getCreateTime,startTime);
        }

        if(Objects.nonNull(endTime)){
            lqw.le(Announcement::getCreateTime,endTime);
        }

        if(Objects.nonNull(taskName)&&!taskName.isEmpty()){
            List<Long> list = announcementTaskService.list(Wrappers.<AnnouncementTask>lambdaQuery()
                    .like(AnnouncementTask::getTaskName, taskName)).stream().map(AnnouncementTask::getId).toList();
            if(list.isEmpty()){

                // 没有这个名字的 任务 返回空
                PageDTO<AnnouncementAdminVO> page=new PageDTO<>();
                page.setPageList(new ArrayList<>());
                page.setPageNow(currentPage);
                page.setPageSize((int)iPage.getSize());
                page.setTotal((int)iPage.getTotal());

                return page;
            }else{
                lqw.in(Announcement::getTaskId,list);
            }
        }

        lqw.orderByDesc(Announcement::getCreateTime);

        List<AnnouncementAdminVO> list = announcementService.list(iPage, lqw).stream().map(i -> {
            AnnouncementAdminVO build = AnnouncementAdminVO.builder()
                    .id(String.valueOf(i.getId()))
                    .adminId(i.getAdminId())
                    .title(i.getTitle())
                    .content(i.getContent())
                    .createTime(i.getCreateTime())
                    .updateTime(i.getUpdateTime())
                    .build();
            AnnouncementTask one = announcementTaskService.getOne(Wrappers.<AnnouncementTask>lambdaQuery().eq(AnnouncementTask::getId, i.getTaskId()));
            if(Objects.nonNull(one)){
                build.setTaskName(one.getTaskName());
            }else{
                build.setTaskName("任务不存在或已经删除");
            }
            return build;
        }).toList();

        PageDTO<AnnouncementAdminVO> page=new PageDTO<>();
        page.setPageList(list);
        page.setPageNow(currentPage);
        page.setPageSize((int)iPage.getSize());
        page.setTotal((int)iPage.getTotal());

        return page;
    }

    @PostMapping("/addAnnouncement")
    public Map<String,Object> addAnnouncement(@RequestBody AnnouncementVO announcementVO){

        Date now = new Date();

        Announcement build = Announcement.builder()
                .adminId(announcementVO.getAdminId())
                .title(announcementVO.getTitle())
                .content(announcementVO.getContent())
                .createTime(now)
                .updateTime(now)
                .isDelete(0)
                .build();

        boolean save = announcementService.save(build);
        if(save){
            return ResultUtil.successMap(null,"新增成功");
        }else{
            return ResultUtil.failMap("新增失败");
        }
    }

    @PostMapping("/updateAnnouncement")
    public Map<String,Object> updateAnnouncement(@RequestBody AnnouncementUpdateVO announcementUpdateVO){

        boolean update = announcementService.update(Wrappers.<Announcement>lambdaUpdate()
                .eq(Announcement::getId, announcementUpdateVO.getAnnouncementId())
                .set(Announcement::getTitle, announcementUpdateVO.getTitle())
                .set(Announcement::getContent, announcementUpdateVO.getContent())
                .set(Announcement::getUpdateTime, new Date())
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }

    @DeleteMapping("/deleteAnnouncement")
    public Map<String,Object> deleteAnnouncement(@RequestParam("announcementIds")List<Long> announcementIds ){
        boolean remove = announcementService.removeByIds(announcementIds);

        if(remove){
            return ResultUtil.successMap(null,"删除成功");
        }else{
            return ResultUtil.failMap("删除失败");
        }

    }


}
