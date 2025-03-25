package org.oyyj.adminservice.controller;

import com.alibaba.nacos.shaded.io.grpc.Grpc;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.adminservice.dto.*;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.vo.AnnouncementAdminVO;
import org.oyyj.adminservice.vo.AnnouncementTaskVO;
import org.oyyj.adminservice.feign.TaskFeign;
import org.oyyj.adminservice.util.ResultUtil;
import org.oyyj.adminservice.vo.AnnouncementUpdateVO;
import org.oyyj.adminservice.vo.AnnouncementVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import java.util.*;

@RestController
@RequestMapping("/admin/task")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    @Autowired
    private IAdminService adminService;

    @Autowired
    private TaskFeign taskFeign;



    // 查询任务 信息
    @PreAuthorize("hasAuthority('super_admin')")
    @GetMapping("/getInfoList")
    public Map<String, Object> getInfoList(@RequestParam(value = "adminName" ,required = false) String adminName,
                                           @RequestParam(value = "taskName",required = false)  String taskName,
                                           @RequestParam(value = "startTime",required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startTime,
                                           @RequestParam(value = "endTime",required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime,
                                           @RequestParam(value = "status",required = false) String status,
                                           @RequestParam(value = "currentPage") Integer currentPage) {

        try {

//            List<Long> adminIds=new ArrayList<>();
//            List<Long> idsByName = adminService.list(Wrappers.<Admin>lambdaQuery().like(Admin::getName, adminName)).stream().map(Admin::getId).toList();
//            List<Long> idsByPhone = adminService.list(Wrappers.<Admin>lambdaQuery().like(Admin::getPhone, adminName)).stream().map(Admin::getId).toList();
//            adminIds.addAll(idsByName);
//            adminIds.addAll(idsByPhone);

            PageDTO<AnnouncementTaskVO> allTask = taskFeign.getAllTask(taskName, startTime, endTime,adminName,status, currentPage);
            if(allTask.getPageList()==null||allTask.getPageList().isEmpty()){
                PageDTO<TaskAdminDTO> taskList=new PageDTO<>();
                taskList.setTotal(allTask.getTotal());
                taskList.setPageList(new ArrayList<>()); // 查询为空 set一个空值
                taskList.setPageSize(allTask.getPageSize());
                taskList.setPageNow(allTask.getPageNow());

                return ResultUtil.successMap(taskList,"查询成功");
            }
            List<TaskAdminDTO> lists = allTask.getPageList().stream().map(i -> {
                try {
                    return TaskAdminDTO.builder()
                            .id(i.getId())
                            .taskName(i.getTaskName())
                            .title(i.getTitle())
                            .content(i.getContent())
                            .adminName(adminService.getOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, Long.valueOf(i.getAdminId()))).getName())
                            .createTime(i.getCreateTime())
                            .updateTime(i.getUpdateTime())
                            .frequency(explainCorn(i.getFrequency()))
                            .status(i.getStatus())
                            .build();
                } catch (AuthenticationException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            PageDTO<TaskAdminDTO> taskList=new PageDTO<>();
            taskList.setTotal(allTask.getTotal());
            taskList.setPageList(lists);
            taskList.setPageSize(allTask.getPageSize());
            taskList.setPageNow(allTask.getPageNow());
            return ResultUtil.successMap(taskList,"查询成功");
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return ResultUtil.failMap("查询失败");
        }

    }


    // 新增设置任务信息
    @PostMapping("/setNewTask")
    public Map<String, Object> setNewTask(@RequestBody AnnouncementTaskDTO announcementTaskDTO) {
        return taskFeign.createAnnouncement(announcementTaskDTO);
    }


    @PostMapping("/updateTask")
    public Map<String, Object> updateTask(@RequestBody AnnouncementTaskDTO announcementTaskDTO) {
        return taskFeign.updateTask(announcementTaskDTO);
    }

    private String explainCorn(String corn) throws AuthenticationException {

        if(!corn.matches("[0-9*?/ ]+")){
            throw new AuthenticationException("数据库数据格式不正确存在问题");
        }

        String[] split = corn.split(" ");
        if(split.length<7){
            throw new AuthenticationException("数据库数据存在问题");
        }

        StringBuffer sb = new StringBuffer();

        if("*".equals(split[6].trim())){
            sb.append("每年");
        }else{
            sb.append(split[6]).append("年");
        }

        if(!"?".equals(split[5].trim())){
            if(!"*".equals(split[5].trim())){
                sb.append("每周");
            }else{
                sb.append("周").append(split[5]);
            }
        }

        if("*".equals(split[4].trim())){
            sb.append("每月");
        }else{
            sb.append(split[4]).append("月");
        }

        if(!"?".equals(split[3].trim())){
            if("*".equals(split[3].trim())){
                sb.append("每天");
            }else{
                sb.append(split[3]).append("天");
            }
        }

        if("*".equals(split[2].trim())){
            sb.append("每时");
        }else{
            sb.append(split[2]).append("时");
        }

        return sb.toString();
    }

    @PutMapping("/stopTask")
    public Map<String,Object> stopTask(@RequestParam("taskId") String taskId) {
        return taskFeign.stopTask(taskId);
    }

    @PutMapping("/resumeTask")
    public Map<String,Object> resumeTask(@RequestParam("taskId") String taskId) {
        return taskFeign.resumeTask(taskId);
    }

    /**
     * 清空任务
     * @param taskId
     * @return
     */
    @DeleteMapping("/deleteTask")
    public Map<String,Object> deleteTask(@RequestParam("taskId") String taskId) {
        return taskFeign.deleteTask(taskId);
    }

    /**
     * 删除 任务记录
     */
    @DeleteMapping("/deleteTaskRecord")
    public Map<String,Object> deleteTaskRecord(@RequestParam("taskId") String taskId){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        return taskFeign.deleteTaskRecord(Long.valueOf(taskId),principal.getAdmin().getId());
    }


    @PutMapping("/stopTaskBatch")
    Map<String, Object> stopTaskBatch(@RequestParam("taskIds") List<String> taskIds) {
        if(Objects.isNull(taskIds)||taskIds.isEmpty()){
            return ResultUtil.failMap("参数不可为空");
        }
        return taskFeign.stopTaskBatch(taskIds);

    }

    @PutMapping("/deleteTaskBatch")
    Map<String, Object> deleteTaskBatch(@RequestParam("taskIds") List<String> taskIds) {
        if(Objects.isNull(taskIds)||taskIds.isEmpty()){
            return ResultUtil.failMap("参数不可为空");
        }
        return taskFeign.deleteTaskBatch(taskIds);
    }

    @DeleteMapping("/deleteTaskRecordBatch")
    Map<String, Object> deleteTaskRecordBatch(@RequestParam("taskIds") List<Long> taskIds) {

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();

        if(Objects.isNull(taskIds)||taskIds.isEmpty()){
            return ResultUtil.failMap("参数不可为空");
        }
        return taskFeign.deleteTaskRecordBatch(taskIds,principal.getAdmin().getId());
    }


    @GetMapping("/getAnnouncementAdmin")
    public Map<String,Object> getAnnouncementAdmin(@RequestParam(value = "title",required = false)String title ,
                                                      @RequestParam(value = "admin",required = false) String admin,
                                                      @RequestParam(value = "taskName",required = false) String taskName,
                                                      @RequestParam(value = "startTime",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startTime,
                                                      @RequestParam(value = "endTime",required = false)@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime,
                                                      @RequestParam("currentPage") Integer currentPage){
        PageDTO<AnnouncementAdminVO> announcementAdmin = taskFeign.getAnnouncementAdmin(title, admin,taskName, startTime, endTime, currentPage);

        List<AnnouncementAdminDTO> list = announcementAdmin.getPageList().stream().map(i -> AnnouncementAdminDTO.builder()
                .id(String.valueOf(i.getId()))
                .adminName(adminService.getOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId,i.getAdminId())).getName())
                .title(String.valueOf(i.getTitle()))
                .content(String.valueOf(i.getContent()))
                .taskName(i.getTaskName())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .build()).toList();

        PageDTO<AnnouncementAdminDTO> announcementAdminDTOPageDTO=new PageDTO<>();
        announcementAdminDTOPageDTO.setTotal(announcementAdmin.getTotal());
        announcementAdminDTOPageDTO.setPageList(list);
        announcementAdminDTOPageDTO.setPageNow(currentPage);
        announcementAdminDTOPageDTO.setPageSize(announcementAdmin.getPageSize());

        return ResultUtil.successMap(announcementAdminDTOPageDTO,"查询成功");

    }

    // 新增

    @PostMapping("/addAnnouncement")
    public Map<String,Object> addAnnouncement(@RequestBody AnnouncementDTO announcementDTO){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();

        return taskFeign.addAnnouncement(AnnouncementVO.builder()
                        .adminId(principal.getAdmin().getId())
                        .title(announcementDTO.getTitle())
                        .content(announcementDTO.getContent())
                .build());
    }

    // 修改（标题 内容 修改者 修改日期）
    @PostMapping("/updateAnnouncement")
    public Map<String,Object> updateAnnouncement(@RequestBody AnnouncementUpdateDTO announcementUpdateDTO){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();

        return taskFeign.updateAnnouncement(AnnouncementUpdateVO.builder()
                        .adminId(principal.getAdmin().getId())
                        .title(announcementUpdateDTO.getTitle())
                        .announcementId(Long.valueOf(announcementUpdateDTO.getAnnouncementId()))
                        .content(announcementUpdateDTO.getContent())
                .build());

    }


    // 删除
    @DeleteMapping("/deleteAnnouncement")
    public Map<String,Object> deleteAnnouncement(@RequestParam("removeId") String removeId){

        if(Objects.isNull(removeId)||removeId.isEmpty()){
            return ResultUtil.failMap("参数不可为空");
        }
        List<String> removeIds = Collections.singletonList(removeId);
        List<Long> list = removeIds.stream().map(Long::parseLong).toList();
        return taskFeign.deleteAnnouncement(list);
    }

    // 批量删除
    @DeleteMapping("/deleteAnnouncementBatch")
    public Map<String,Object> deleteAnnouncementBatch(@RequestParam("removeIds") List<String> removeIds){

        List<Long> list = removeIds.stream().map(Long::parseLong).toList();
        return taskFeign.deleteAnnouncement(list);
    }


}
