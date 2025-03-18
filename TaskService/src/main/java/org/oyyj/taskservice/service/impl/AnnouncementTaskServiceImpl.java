package org.oyyj.taskservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Select;
import org.oyyj.taskservice.mapper.AnnouncementTaskMapper;
import org.oyyj.taskservice.pojo.AnnouncementTask;
import org.oyyj.taskservice.service.IAnnouncementTaskService;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementTaskServiceImpl extends ServiceImpl<AnnouncementTaskMapper, AnnouncementTask> implements IAnnouncementTaskService {
}
