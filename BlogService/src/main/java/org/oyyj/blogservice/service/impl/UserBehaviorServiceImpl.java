package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.UserBehavior;
import org.oyyj.blogservice.service.IUserBehaviorService;
import org.springframework.stereotype.Service;

@Service
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorMapper,UserBehavior> implements IUserBehaviorService {
}
