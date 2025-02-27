package org.oyyj.userservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.IMppService;
import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.userservice.mapper.UserStarMppMapper;
import org.oyyj.userservice.pojo.UserStar;
import org.oyyj.userservice.service.IUserStarService;
import org.springframework.stereotype.Service;

@Service
public class UserStarServiceImpl extends MppServiceImpl<UserStarMppMapper, UserStar>
        implements IUserStarService {
}
