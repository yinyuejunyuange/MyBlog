package org.oyyj.blogservice.config.Service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.config.Service.IApiConfigService;
import org.oyyj.blogservice.config.mapper.ApiConfigMapper;
import org.oyyj.blogservice.config.pojo.ApiConfig;
import org.oyyj.blogservice.mapper.UserRedemptionMapper;
import org.oyyj.blogservice.pojo.UserRedemption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiConfigServiceImpl extends ServiceImpl<ApiConfigMapper, ApiConfig> implements IApiConfigService {

    @Autowired
    private UserRedemptionMapper userRedemptionMapper;

    public List<ApiConfig> findByIsActiveTrue() {
        return list();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean subVipNum(Long userId){
        ApiConfig one = getOne(Wrappers.<ApiConfig>lambdaQuery()
                .eq(ApiConfig::getName, "vip:num")
        );
        if(one == null){
            throw new RuntimeException("数据库数据异常");
        }
        boolean update = update(Wrappers.<ApiConfig>lambdaUpdate()
                .eq(ApiConfig::getName, "vip:num")
                .gt(ApiConfig::getValue, 0)
                .setSql("value = value -1") // 直接在原来的基础上减一
        );

        // 用户库存关联表增加
        UserRedemption userRedemption = new UserRedemption();
        userRedemption.setUserId(userId);
        userRedemption.setRedeemTime(LocalDateTime.now());
        userRedemption.setInventoryId(one.getId());

        int insert = userRedemptionMapper.insert(userRedemption);
        if(insert ==1 && update){
            return true;
        }else{
            throw new RuntimeException("事务操作失败 insert"+insert+" update"+update);
        }

    }

}
