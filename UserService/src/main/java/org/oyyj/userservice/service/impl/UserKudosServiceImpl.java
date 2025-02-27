package org.oyyj.userservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.IMppService;
import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserKudosMppMapper;
import org.oyyj.userservice.pojo.UserKudos;
import org.oyyj.userservice.service.IUserKudosService;
import org.springframework.stereotype.Service;

@Service
public class UserKudosServiceImpl extends MppServiceImpl<UserKudosMppMapper,UserKudos> implements IUserKudosService {

}
