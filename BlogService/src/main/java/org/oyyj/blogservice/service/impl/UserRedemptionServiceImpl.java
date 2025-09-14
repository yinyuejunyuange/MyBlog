package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.mapper.UserRedemptionMapper;
import org.oyyj.blogservice.pojo.UserRedemption;
import org.oyyj.blogservice.service.IUserRedemptionService;
import org.springframework.stereotype.Service;

@Service
public class UserRedemptionServiceImpl extends ServiceImpl<UserRedemptionMapper, UserRedemption> implements IUserRedemptionService {
}
