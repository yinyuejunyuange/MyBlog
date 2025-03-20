package org.oyyj.taskservice.job;

import org.oyyj.taskservice.pojo.Announcement;
import org.oyyj.taskservice.service.IAnnouncementService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@PersistJobDataAfterExecution
@DisallowConcurrentExecution  // 禁止并发的访问同一个job定义
public class AnnouncementJob implements Job {

    @Autowired
    private IAnnouncementService announcementService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String adminId = jobExecutionContext.getMergedJobDataMap().getString("adminId");// 获取任务上下文信息
        String taskId = jobExecutionContext.getMergedJobDataMap().getString("taskId");// 获取任务上下文信息
        String title = jobExecutionContext.getMergedJobDataMap().getString("title");// 获取任务上下文信息
        String content = jobExecutionContext.getMergedJobDataMap().getString("content");// 获取任务上下文信息


        Date createTime=new Date();
        Date updateTime=new Date();  // 定义在创建的当天

//        try {
//            // 定义解析格式
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            createTime = formatter.parse(createTimeStr);
//            updateTime = formatter.parse(updateTimeStr);
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }

        Announcement build = Announcement.builder()
                .title(title)
                .content(content)
                .createTime(createTime)
                .updateTime(updateTime)
                .adminId(Long.valueOf(adminId))
                .taskId(Long.valueOf(taskId))
                .build();

        announcementService.save(build);
    }
}
