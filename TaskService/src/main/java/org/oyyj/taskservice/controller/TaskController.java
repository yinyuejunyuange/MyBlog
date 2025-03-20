package org.oyyj.taskservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.taskservice.dto.AnnouncementTaskDTO;
import org.oyyj.taskservice.dto.PageDTO;
import org.oyyj.taskservice.dto.TaskAdminDTO;
import org.oyyj.taskservice.dto.TaskDTO;
import org.oyyj.taskservice.job.AnnouncementJob;
import org.oyyj.taskservice.pojo.AnnouncementTask;
import org.oyyj.taskservice.pojo.JobBean;
import org.oyyj.taskservice.service.IAnnouncementService;
import org.oyyj.taskservice.service.IAnnouncementTaskService;
import org.oyyj.taskservice.utils.JobUtils;
import org.oyyj.taskservice.utils.ResultUtil;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.stream.Task;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/task")
public class TaskController {


    @Autowired
    private Scheduler scheduler;

    @Autowired
    private IAnnouncementService announcementService;

    @Autowired
    private IAnnouncementTaskService announcementTaskService;

    // 管理端 通过调用此接口 实现执行任务以生成公告
    @PostMapping("/createAnnouncementTask")
    public Map<String,Object> createAnnouncement(@RequestBody AnnouncementTaskDTO announcementTaskDTO) {

        Date date = new Date();
        // todo 在数据库中存储 任务信息
        AnnouncementTask builds = AnnouncementTask.builder()
                .adminId(Long.valueOf(announcementTaskDTO.getAdminId()))
                .createTime(date)
                .updateTime(date)
                .frequency(announcementTaskDTO.getFrequency())
                .taskName(announcementTaskDTO.getTaskName())
                .title(announcementTaskDTO.getTitle())
                .content(announcementTaskDTO.getContent())
                .build();
        announcementTaskService.save(builds);
        announcementTaskDTO.setTaskId(String.valueOf(builds.getId()));

        try {
            JobBean build = JobBean.builder()
                    .jobName(String.valueOf(announcementTaskDTO.getTaskId())) // 以任务id作为任务的标识
                    .jobClass(AnnouncementJob.class.getName())
                    .cornExpression(announcementTaskDTO.getFrequency())
                    .build();

            JobUtils.createJobAnnouncement(scheduler,build, announcementTaskDTO);
            return ResultUtil.successMap(null,"任务创建成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.failMap("任务创建失败");
        }
    }

    // 管理员查询 任务
    @GetMapping("/getAllTask")
    public PageDTO<TaskAdminDTO> getAllTask(@RequestParam(value = "taskName",required = false) String taskName,
                                            @RequestParam(value = "startTime",required = false) Date startTime,
                                            @RequestParam(value = "endTime",required = false) Date endTime,
                                            @RequestParam(value = "currentPage") Integer currentPage) {
        try {
            if(Objects.isNull(currentPage)){
                currentPage=1;
            }
            IPage<AnnouncementTask> page=new Page<>(currentPage,10);
            LambdaQueryWrapper<AnnouncementTask> lambdaQueryWrapper = new LambdaQueryWrapper<>();

            if(Objects.nonNull(taskName)){
                lambdaQueryWrapper.like(AnnouncementTask::getTaskName,taskName);
            }
            if(Objects.nonNull(startTime)){
                lambdaQueryWrapper.ge(AnnouncementTask::getCreateTime,startTime);
            }
            if(Objects.nonNull(endTime)){
                lambdaQueryWrapper.le(AnnouncementTask::getCreateTime,endTime);
            }
//            if(Objects.nonNull(status)){
//                lambdaQueryWrapper.eq(AnnouncementTask::getStatus,status);
//            }

            List<TaskAdminDTO> list = announcementTaskService.list(page, lambdaQueryWrapper).stream()
                    .map(i -> {
                        try {
                            return TaskAdminDTO.builder()
                                    .id(String.valueOf(i.getId()))
                                    .taskName(i.getTaskName())
                                    .title(i.getTitle())
                                    .content(i.getContent())
                                    .adminId(String.valueOf(i.getAdminId()))
                                    .createTime(i.getCreateTime())
                                    .updateTime(i.getUpdateTime())
                                    .frequency(i.getFrequency())
                                    .status(JobUtils.getTriggerStatus(i.getId(), scheduler))
                                    .build();
                        } catch (SchedulerException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();

            PageDTO<TaskAdminDTO> pageDTO=new PageDTO<>();
            pageDTO.setTotal((int)page.getTotal());
            pageDTO.setPageNow(currentPage);
            pageDTO.setPageSize((int)page.getSize());
            pageDTO.setPageList(list);

            return pageDTO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PutMapping("/stopTask")
    public Map<String,Object> stopTask(@RequestParam("taskId") String taskId) {
        try {
            JobUtils.pauseJob(scheduler, taskId);
            return ResultUtil.successMap(null,"暂停成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.failMap("暂停失败"+e.getMessage());
        }
    }

    @PutMapping("/resumeTask")
    public Map<String,Object> resumeTask(@RequestParam("taskId") String taskId) {
        try {
            JobUtils.resumeJob(scheduler, taskId);
            return ResultUtil.successMap(null,"恢复成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.failMap("恢复失败"+e.getMessage());
        }
    }

    /**
     * 清空任务
     * @param taskId
     * @return
     */
    @PutMapping("/deleteTask")
    public Map<String,Object> deleteTask(@RequestParam("taskId") String taskId) {
        try {
            JobUtils.deleteJob(scheduler, taskId);
            return ResultUtil.successMap(null,"清空成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.failMap("清空失败"+e.getMessage());
        }
    }

    @PostMapping("/updateTask")
    public Map<String,Object> updateTask(@RequestBody AnnouncementTaskDTO announcementTaskDTO) throws SQLException {

        Date date = new Date();
        // todo 在数据库中存储 任务信息
        AnnouncementTask builds = AnnouncementTask.builder()
                .id(Long.valueOf(announcementTaskDTO.getTaskId()))
                .updateBy(Long.valueOf(announcementTaskDTO.getAdminId()))
                .updateTime(date)
                .frequency(announcementTaskDTO.getFrequency())
                .taskName(announcementTaskDTO.getTaskName())
                .title(announcementTaskDTO.getTitle())
                .content(announcementTaskDTO.getContent())
                .build();
        boolean save = announcementTaskService.updateById(builds);

        if(!save){
            throw new SQLException("数据库修改失败");
        }

        JobBean build = JobBean.builder()
                .jobName(String.valueOf(announcementTaskDTO.getTaskId())) // 以任务id作为任务的标识
                .jobClass(AnnouncementJob.class.getName())
                .cornExpression(announcementTaskDTO.getFrequency())
                .build();

        JobUtils.modifyJobAnnouncement(scheduler,build, announcementTaskDTO);
        return ResultUtil.successMap(null,"任务创建成功");
    }




}
