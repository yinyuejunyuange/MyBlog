package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.UserBehavior;
import org.oyyj.blogservice.service.IUserBehaviorService;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorMapper,UserBehavior> implements IUserBehaviorService {

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 增加阅读计数（使用Lua脚本保证原子性）
     */
    public boolean incrementReadCount(Long blogId, Long userId) throws Exception {
        String countKey = RedisPrefix.BLOG_INGO + blogId;
        String userReadKey = RedisPrefix.BLOG_USER_READ + blogId + ":" + userId;

        // 用户短期内重新阅读
        Integer addNum = userBehaviorMapper.addUserBehavior(userId, blogId, BehaviorEnum.VIEW);
        if(Objects.isNull(addNum) || addNum == 0){
            throw new Exception("用户行为添加失败");
        }
        // 2. 构造脚本参数
        // KEYS：阅读数key、用户阅读标记key
        String[] keys = new String[]{countKey, userReadKey};
        // ARGV：用户标记过期时间（3600秒=1小时）、阅读数key续期时间（600秒=10分钟）
        String[] args = new String[]{"3600", "600"};
        Long execute = null;
        try {
            execute = redisUtil.incrBlogReadNum(keys, args);
            if(Objects.isNull(execute)){
                throw  new Exception("执行阅读数计数Lua脚本失败");
            }
            return true;
        } catch (Exception e) {
            // 5. 异常处理（降级,日志告警）
            log.error("执行阅读数计数Lua脚本失败，blogId:{}，userId:{}", blogId, userId, e);
            throw new RuntimeException("执行阅读数计数Lua脚本失败",e);
        }
    }

    /**
     * 用户博客行为 除了阅读
     * @param blogId
     * @param userId
     * @return
     * @throws Exception
     */
    public boolean userBehaviorBlog(Long blogId, Long userId,BehaviorEnum behaviorEnum) throws Exception {
        Integer addNum = userBehaviorMapper.addUserBehavior(userId, blogId, behaviorEnum);
        if(Objects.isNull(addNum) || addNum == 0){
            log.error("用户行为添加失败 blogId:{} userId:{} behavior:{}", blogId, userId , behaviorEnum);
            throw new Exception("用户行为添加失败");
        }

        return true;
    }

}
