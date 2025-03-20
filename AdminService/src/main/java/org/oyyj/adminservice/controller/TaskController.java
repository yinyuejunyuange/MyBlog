package org.oyyj.adminservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.adminservice.dto.AnnouncementTaskDTO;
import org.oyyj.adminservice.dto.TaskAdminDTO;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.service.IAdminService;
import org.oyyj.adminservice.vo.AnnouncementTaskVO;
import org.oyyj.adminservice.dto.PageDTO;
import org.oyyj.adminservice.feign.TaskFeign;
import org.oyyj.adminservice.util.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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

            List<Long> adminIds=new ArrayList<>();
            List<Long> idsByName = adminService.list(Wrappers.<Admin>lambdaQuery().like(Admin::getName, adminName)).stream().map(Admin::getId).toList();
            List<Long> idsByPhone = adminService.list(Wrappers.<Admin>lambdaQuery().like(Admin::getPhone, adminName)).stream().map(Admin::getId).toList();
            adminIds.addAll(idsByName);
            adminIds.addAll(idsByPhone);

            PageDTO<AnnouncementTaskVO> allTask = taskFeign.getAllTask(taskName, startTime, endTime, currentPage);

            if(adminName!=null&&!adminName.isEmpty()){
                // 有值 进行筛选
                List<AnnouncementTaskVO> filter = allTask.getPageList().stream().filter(i -> adminIds.contains(Long.valueOf(i.getAdminId()))).toList();
                if(status!=null&&!status.isEmpty()){
                    filter=filter.stream().filter(i -> status.equals(i.getStatus())).toList();
                }

                List<TaskAdminDTO> list = filter.stream().map(i -> TaskAdminDTO.builder()
                        .id(i.getId())
                        .taskName(i.getTaskName())
                        .title(i.getTitle())
                        .content(i.getContent())
                        .adminName(adminService.getOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, Long.valueOf(i.getAdminId()))).getName())
                        .createTime(i.getCreateTime())
                        .updateTime(i.getUpdateTime())
                        .frequency(i.getFrequency())
                        .status(i.getStatus())
                        .build()).toList();

                PageDTO<TaskAdminDTO> taskList=new PageDTO<>();
                taskList.setTotal(allTask.getTotal());
                taskList.setPageList(list);
                taskList.setPageSize(allTask.getPageSize());
                taskList.setPageNow(allTask.getPageNow());
                return ResultUtil.successMap(taskList,"查询成功");
            }


            List<TaskAdminDTO> list = allTask.getPageList().stream().map(i -> {
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
            taskList.setPageList(list);
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

        if(!corn.matches("[0-9*? ]+")){
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
    @PutMapping("/deleteTask")
    public Map<String,Object> deleteTask(@RequestParam("taskId") String taskId) {
        return taskFeign.deleteTask(taskId);
    }
}
