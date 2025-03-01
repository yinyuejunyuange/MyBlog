package org.oyyj.userservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserCommentMapper;
import org.oyyj.userservice.pojo.UserComment;
import org.oyyj.userservice.service.IUserCommentService;
import org.springframework.stereotype.Service;

@Service
public class UserCommentServiceImpl extends MppServiceImpl<UserCommentMapper, UserComment> implements IUserCommentService {
}
