package org.oyyj.taskservice.utils;

import org.oyyj.taskservice.dto.AnnouncementTaskDTO;
import org.oyyj.taskservice.pojo.AnnouncementTask;
import org.oyyj.taskservice.pojo.JobBean;
import org.oyyj.taskservice.service.IAnnouncementTaskService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

public class JobUtils {
    /**
     * 生成新的任务
     * @param scheduler 调度器
     * @param jobBean 任务bean
     */


    public static void createJob(Scheduler scheduler, JobBean jobBean)  {

        Class<? extends Job> jobClass=null;
        JobDetail jobDetail=null;
        Trigger trigger=null;

        try {
            jobClass = (Class<? extends Job>) Class.forName(jobBean.getJobClass()); //这是一个泛型类型。Class 是 Java 中的一个类，用于表示一个类的类型。
            // ? extends Job 表示这个类可以是 Job 类或者其任何子类。换句话说，这个类型可以接受 Job 类及其所有派生类。
            // 接着通过 反射 将名称输入以此得到对应的class 最后进行强制类型转化内 即可

            jobDetail= JobBuilder.newJob(jobClass) // 绑定具体job的class
                    .storeDurably() // 任务持久化  jobDetail 是与 trigger相关联的 如果 没有关联trigger（就删除）还想让 jobDetail存在就需要要用 持久化
                    .withIdentity(jobBean.getJobName()) // 唯一标识 ("任务名","任务组名")
                    .usingJobData("count",1) // 共享数据初始化  用于时间数据间的共享
                    .build();

            trigger=TriggerBuilder.newTrigger()
                    .forJob(jobDetail) // 绑定 JobDetail实例名  为哪一个jobDetail服务 jobDetail 通过 @Bean注解 就会生成一个方法名为名称的实例
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobBean.getCornExpression())) // CronScheduleBuilder.cronSchedule(corn表达式)  // 指定时间规则
                    .withIdentity(jobBean.getJobName()+"_"+"trigger") // 触发器详情
                    .build();

            scheduler.scheduleJob(jobDetail,trigger);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }


    public static void createJobAnnouncement(Scheduler scheduler, JobBean jobBean, AnnouncementTaskDTO announcementTaskDTO)  {

        Class<? extends Job> jobClass=null;
        JobDetail jobDetail=null;
        Trigger trigger=null;



        try {
            jobClass = (Class<? extends Job>) Class.forName(jobBean.getJobClass()); //这是一个泛型类型。Class 是 Java 中的一个类，用于表示一个类的类型。
            // ? extends Job 表示这个类可以是 Job 类或者其任何子类。换句话说，这个类型可以接受 Job 类及其所有派生类。
            // 接着通过 反射 将名称输入以此得到对应的class 最后进行强制类型转化内 即可

            jobDetail= JobBuilder.newJob(jobClass) // 绑定具体job的class
                    .storeDurably() // 任务持久化  jobDetail 是与 trigger相关联的 如果 没有关联trigger（就删除）还想让 jobDetail存在就需要要用 持久化
                    .withIdentity(jobBean.getJobName()) // 唯一标识 ("任务名","任务组名")
                    .usingJobData("adminId", announcementTaskDTO.getAdminId())    // 共享数据初始化  用于时间数据间的共享
                    .usingJobData("taskId", announcementTaskDTO.getTaskId())
                    .usingJobData("title", announcementTaskDTO.getTitle())
                    .usingJobData("content", announcementTaskDTO.getContent())
                    .usingJobData("createTime",String.valueOf(announcementTaskDTO.getCreateTime()))
                    .usingJobData("updateTime",String.valueOf(announcementTaskDTO.getUpdateTime()))
                    .build();

            trigger=TriggerBuilder.newTrigger()
                    .forJob(jobDetail) // 绑定 JobDetail实例名  为哪一个jobDetail服务 jobDetail 通过 @Bean注解 就会生成一个方法名为名称的实例
                    .withSchedule(CronScheduleBuilder.cronSchedule(announcementTaskDTO.getFrequency())) // CronScheduleBuilder.cronSchedule(corn表达式)  // 指定时间规则
                    .withIdentity(jobBean.getJobName()+"_"+"trigger") // 触发器详情
                    .build();
            scheduler.scheduleJob(jobDetail,trigger);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 暂停某任务
     * @param scheduler 调度器
     * @param jobName 任务名
     */
    public static void pauseJob(Scheduler scheduler, String jobName)  {
        JobKey jobKey=JobKey.jobKey(jobName); // 得到jobKey
        try {
            scheduler.pauseJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 恢复任务
     * @param scheduler 调度器
     * @param jobName 任务名
     */
    public static void resumeJob(Scheduler scheduler, String jobName)  {
        JobKey jobKey=JobKey.jobKey(jobName); // 得到jobKey
        try {
            scheduler.resumeJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除任务
     * @param scheduler 调度器
     * @param jobName 任务名
     */
    public static void deleteJob(Scheduler scheduler, String jobName)  {
        JobKey jobKey=JobKey.jobKey(jobName); // 得到jobKey
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行一次任务   运行此方法之前要先运行 create 因为 原来此任务没有触发器 create只有才有
     * @param scheduler 调度器
     * @param jobName 任务名
     */
    public static void runJobOnce(Scheduler scheduler, String jobName)  {
        JobKey jobKey=JobKey.jobKey(jobName);
        try {
            scheduler.triggerJob(jobKey); // 执行一次定时任务
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * 修改任务
     * @param scheduler
     * @param jobBean
     */
    public static void modifyJob(Scheduler scheduler, JobBean jobBean) {
        // 获取任务触发器的唯一标识
        TriggerKey triggerKey=TriggerKey.triggerKey(jobBean.getJobName()+"_"+"trigger");
        try {
            // 通过唯一标识 获取旧的触发器对象
            CronTrigger oldTrigger = (CronTrigger)scheduler.getTrigger(triggerKey); //
            // 使用新的corn表达式构建新的触发器
            String newCron=jobBean.getCornExpression();
            CronTrigger newTrigger=oldTrigger.getTriggerBuilder()
                    .withSchedule(CronScheduleBuilder.cronSchedule(newCron))
                    .build();

            // 调度器更新任务的触发器
            scheduler.rescheduleJob(triggerKey, newTrigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }



    public static String getTriggerStatus(Long taskId,Scheduler scheduler) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey((taskId) + "_" + "trigger");
        Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);
        return getTriggerStatesCN(String.valueOf(triggerState));
    }


    /**
     * 匹配任务状态
     * @param key
     * @return
     */
    public static String getTriggerStatesCN(String key) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("BLOCKED", "阻塞");
        map.put("COMPLETE", "完成");
        map.put("ERROR", "出错");
        map.put("NONE", "不存在或者已清除");
        map.put("NORMAL", "正常");
        map.put("PAUSED", "暂停");
        map.put("WAITING", "等待执行");
        map.put("ACQUIRED", "已经获取触发器");

        map.put("4", "阻塞");
        map.put("2", "完成");
        map.put("3", "出错");
        map.put("-1", "不存在");
        map.put("0", "正常");
        map.put("1", "暂停");
    /*  **STATE_BLOCKED 4 阻塞
STATE_COMPLETE 2 完成
STATE_ERROR 3 错误
STATE_NONE -1 不存在
STATE_NORMAL 0 正常
STATE_PAUSED 1 暂停***/
        return map.get(key);
    }


    public static String getTriggerStatesByCN(String key) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("阻塞","BLOCKED");
        map.put( "完成","COMPLETE");
        map.put( "出错","ERROR");
        map.put("不存在或者已清除","NONE");
        map.put("正常","NORMAL");
        map.put( "暂停","PAUSED");
        map.put( "等待执行","WAITING");
        map.put( "已经获取触发器","ACQUIRED");
    /*  **STATE_BLOCKED 4 阻塞
STATE_COMPLETE 2 完成
STATE_ERROR 3 错误
STATE_NONE -1 不存在
STATE_NORMAL 0 正常
STATE_PAUSED 1 暂停***/
        return map.get(key);
    }

    /**
     * 修改任务
     * @param scheduler
     * @param jobBean
     */
    public static void modifyJobAnnouncement(Scheduler scheduler, JobBean jobBean,AnnouncementTaskDTO announcementTaskDTO) {
        // 获取任务触发器的唯一标识
        TriggerKey triggerKey=TriggerKey.triggerKey(jobBean.getJobName()+"_"+"trigger");
        JobKey jobKey=JobKey.jobKey(jobBean.getJobName());
        try {
            // 通过唯一标识 获取旧的触发器对象
            CronTrigger oldTrigger = (CronTrigger)scheduler.getTrigger(triggerKey); //
            // 使用新的corn表达式构建新的触发器
            String newCron=jobBean.getCornExpression();
            CronTrigger newTrigger=oldTrigger.getTriggerBuilder()
                    .withSchedule(CronScheduleBuilder.cronSchedule(newCron))
                    .build();


            // 修改具体任务
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.put("adminId", announcementTaskDTO.getAdminId());
            jobDataMap.put("taskId",announcementTaskDTO.getTaskId());
            jobDataMap.put("title",announcementTaskDTO.getTitle());
            jobDataMap.put("content",announcementTaskDTO.getContent());
            jobDataMap.put("updateTime",String.valueOf(announcementTaskDTO.getUpdateTime()));

            JobDetail build = jobDetail.getJobBuilder()
                    .usingJobData(jobDataMap)
                    .build();
            // 删除旧的任务
            scheduler.deleteJob(jobKey);

            // 执行新任务
            scheduler.scheduleJob(build,newTrigger);

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }


}
