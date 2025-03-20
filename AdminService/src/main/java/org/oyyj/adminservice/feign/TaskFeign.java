package org.oyyj.adminservice.feign;

import org.oyyj.adminservice.config.FeignRequestConfig;
import org.oyyj.adminservice.dto.AnnouncementTaskDTO;
import org.oyyj.adminservice.vo.AnnouncementTaskVO;
import org.oyyj.adminservice.dto.PageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@FeignClient(value = "TaskService",configuration = FeignRequestConfig.class)
public interface TaskFeign {
    @GetMapping("/task/getAllTask")
    PageDTO<AnnouncementTaskVO> getAllTask(@RequestParam(value = "taskName",required = false) String taskName,
                                           @RequestParam(value = "startTime",required = false) Date startTime,
                                           @RequestParam(value = "endTime",required = false) Date endTime,
                                           @RequestParam(value = "currentPage") Integer currentPage);

    @PostMapping("/task/createAnnouncementTask")
    Map<String,Object> createAnnouncement(@RequestBody AnnouncementTaskDTO announcementTaskDTO);

    @PutMapping("/task/stopTask")
    Map<String,Object> stopTask(@RequestParam("taskId") String taskId) ;

    @PutMapping("/task/resumeTask")
    Map<String,Object> resumeTask(@RequestParam("taskId") String taskId);

    /**
     * 清空任务
     * @param taskId
     * @return
     */
    @PutMapping("/task/deleteTask")
    Map<String,Object> deleteTask(@RequestParam("taskId") String taskId);

    @PostMapping("/task/updateTask")
    Map<String,Object> updateTask(@RequestBody AnnouncementTaskDTO announcementTaskDTO) ;

}
