package org.oyyj.taskservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.taskservice.mapper.AnnouncementMapper;
import org.oyyj.taskservice.pojo.Announcement;
import org.oyyj.taskservice.service.IAnnouncementService;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements IAnnouncementService  {
}
