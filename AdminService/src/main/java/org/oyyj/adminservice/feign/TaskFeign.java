package org.oyyj.adminservice.feign;

import org.oyyj.adminservice.config.FeignRequestConfig;
import org.oyyj.adminservice.dto.AnnouncementTaskDTO;
import org.oyyj.adminservice.vo.AnnouncementAdminVO;
import org.oyyj.adminservice.vo.AnnouncementTaskVO;
import org.oyyj.adminservice.dto.PageDTO;
import org.oyyj.adminservice.vo.AnnouncementUpdateVO;
import org.oyyj.adminservice.vo.AnnouncementVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@FeignClient(value = "TaskService",configuration = FeignRequestConfig.class)
public interface TaskFeign {
    @GetMapping("/task/getAllTask")
    PageDTO<AnnouncementTaskVO> getAllTask(@RequestParam(value = "taskName", required = false) String taskName,
                                           @RequestParam(value = "startTime", required = false) Date startTime,
                                           @RequestParam(value = "endTime", required = false) Date endTime,
                                           @RequestParam(value = "status",required = false)String status,
                                           @RequestParam(value = "admin",required = false) String admin,
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

    @DeleteMapping("/task/deleteTaskRecord")
    Map<String,Object> deleteTaskRecord(@RequestParam("taskId") Long taskId,@RequestParam("adminId") Long adminId);

    @PutMapping("/task/stopTaskBatch")
    Map<String, Object> stopTaskBatch(@RequestParam("taskIds") List<String> taskIds) ;

    @PutMapping("/task/deleteTaskBatch")
    Map<String, Object> deleteTaskBatch(@RequestParam("taskIds") List<String> taskIds) ;

    @DeleteMapping("/task/deleteTaskRecordBatch")
    Map<String, Object> deleteTaskRecordBatch(@RequestParam("taskIds") List<Long> taskIds, @RequestParam("adminId") Long adminId) ;

    @GetMapping("/task/announce/getAnnouncementAdmin")
    PageDTO<AnnouncementAdminVO> getAnnouncementAdmin(@RequestParam(value = "title",required = false)String title ,
                                                             @RequestParam(value = "admin",required = false) String admin,
                                                             @RequestParam(value = "taskName",required = false) String taskName,
                                                             @RequestParam(value = "startTime",required = false) Date startTime,
                                                             @RequestParam(value = "endTime",required = false) Date endTime,
                                                             @RequestParam("currentPage") Integer currentPage);
    @PostMapping("/task/announce/addAnnouncement")
    Map<String,Object> addAnnouncement(@RequestBody AnnouncementVO announcementVO);

    @PostMapping("/task/announce/updateAnnouncement")
    Map<String,Object> updateAnnouncement(@RequestBody AnnouncementUpdateVO announcementUpdateVO);

    @DeleteMapping("/task/announce/deleteAnnouncement")
    Map<String,Object> deleteAnnouncement(@RequestParam("announcementIds")List<Long> announcementIds );

}
