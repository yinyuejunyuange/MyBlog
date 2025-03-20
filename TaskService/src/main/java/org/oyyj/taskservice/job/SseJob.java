package org.oyyj.taskservice.job;

import com.github.jeffreyning.mybatisplus.conf.EnableAutoFill;
import org.oyyj.taskservice.controller.SseController;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yaml.snakeyaml.emitter.Emitter;

public class SseJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    }
}
