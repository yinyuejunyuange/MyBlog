package org.oyyj.taskservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.taskservice.mapper.AnnouncementUserMapper;
import org.oyyj.taskservice.pojo.AnnouncementUser;
import org.oyyj.taskservice.service.IAnnouncementUserService;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementUserServiceImpl extends MppServiceImpl<AnnouncementUserMapper, AnnouncementUser> implements IAnnouncementUserService {
}
