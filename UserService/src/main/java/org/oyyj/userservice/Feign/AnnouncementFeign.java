package org.oyyj.userservice.Feign;

import org.oyyj.userservice.config.FeignAIChatConfig;
import org.oyyj.userservice.vo.AnnouncementUserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@FeignClient(value = "TaskService",configuration = FeignAIChatConfig.class)
public interface AnnouncementFeign {
    @GetMapping("/task/announce/getAnnouncementByUser")
    List<AnnouncementUserVO> getAnnouncementByUser(@RequestParam("userId") Long userId,
                                                   @RequestParam("currentIndex") Integer currentIndex,
                                                   @RequestParam("createTime") Date createTime);

    @PutMapping("/task/announce/readAnnouncement")
    boolean readAnnouncement(@RequestParam("userId")Long userId,@RequestParam("announcementId") Long announcementId );

}
