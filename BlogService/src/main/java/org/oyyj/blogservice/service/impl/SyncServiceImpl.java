package org.oyyj.blogservice.service.impl;

import org.oyyj.blogservice.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
@Async("taskExecutor") // 整体方法全部异步 且指明使用的线程池
public class SyncServiceImpl implements SyncService {

}
