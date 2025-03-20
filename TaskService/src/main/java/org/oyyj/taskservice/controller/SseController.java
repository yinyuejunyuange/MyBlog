package org.oyyj.taskservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.taskservice.feign.UserFeign;
import org.oyyj.taskservice.job.SseJob;
import org.oyyj.taskservice.pojo.Announcement;
import org.oyyj.taskservice.pojo.AnnouncementUser;
import org.oyyj.taskservice.service.IAnnouncementService;
import org.oyyj.taskservice.service.IAnnouncementUserService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

@RestController
@RequestMapping("/task/sse")
public class SseController {

    @Autowired
    private IAnnouncementService announcementService;

    @Autowired
    private IAnnouncementUserService announcementUserService;

    @Autowired
    private UserFeign userFeign;

    @GetMapping("/connect")
    public SseEmitter handleSse(@RequestParam("userId") String userId) {
        SseEmitter emitter = new SseEmitter();
        Date userExist = userFeign.isUserExist(Long.valueOf(userId));
        if (userExist == null) {
            throw new RuntimeException("用户不存在");
        }

        // 将 ScheduledThreadPoolExecutor 移出 try-with-resources  否则无法实现心跳 内部语句执行完 自动 close
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);

        executor.scheduleAtFixedRate(() -> {
            try {
                // 查询数据库 获取当前用户已经读过的公告
                List<Long> list = announcementUserService.list(Wrappers.<AnnouncementUser>lambdaQuery()
                                .eq(AnnouncementUser::getUserId, userId)).stream()
                        .map(AnnouncementUser::getAnnouncementId)
                        .toList();

                if (!list.isEmpty()) {
                    // 查询 用户没有读过的公告
                    int size = announcementService.list(Wrappers.<Announcement>lambdaQuery()
                            .notIn(Announcement::getId, list)
                            .ge(Announcement::getCreateTime, userExist)
                    ).size();

                    if (size != 0) {
                        emitter.send(size); // 发送公告数量
                        System.out.println("发送消息");
                    } else {
                        emitter.send(true); // 心跳机制
                        System.out.println("心跳机制");
                    }
                } else {
                    // 用户一条信息没读取
                    int size = announcementService.list(Wrappers.<Announcement>lambdaQuery()
                            .ge(Announcement::getCreateTime, userExist)
                    ).size();

                    if (size != 0) {
                        emitter.send(size); // 发送公告数量
                        System.out.println("发送消息");
                    } else {
                        emitter.send(true); // 心跳机制
                        System.out.println("心跳机制");
                    }
                }

            } catch (Exception e) {
                emitter.completeWithError(e);
                executor.shutdown(); // 停止 executor
            }
        }, 0, 2, TimeUnit.SECONDS); // 每 2 秒执行一次

        emitter.onCompletion(() -> {
            System.out.println("SSE连接已完成");
            executor.shutdown(); // 关闭 executor
        });

        emitter.onTimeout(() -> {
            System.out.println("SSE连接超时");
            emitter.complete();
            executor.shutdown(); // 关闭 executor
        });

        return emitter;
    }

}
