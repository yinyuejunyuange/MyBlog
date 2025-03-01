package org.oyyj.userservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserReplyMapper;
import org.oyyj.userservice.pojo.UserReply;
import org.oyyj.userservice.service.IUserReplyService;
import org.springframework.stereotype.Service;

@Service
public class UserReplyServiceImpl extends MppServiceImpl<UserReplyMapper, UserReply> implements IUserReplyService {
}
