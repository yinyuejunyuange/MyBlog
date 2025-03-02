package org.oyyj.userservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserAttentionMapper;
import org.oyyj.userservice.pojo.UserAttention;
import org.oyyj.userservice.service.IUserAttentionService;
import org.springframework.stereotype.Service;

@Service
public class UserAttentionServiceImpl
        extends MppServiceImpl<UserAttentionMapper, UserAttention> implements IUserAttentionService {
}
