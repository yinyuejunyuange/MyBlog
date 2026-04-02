package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.UserBehavior;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.IUserBehaviorService;
import org.oyyj.blogservice.vo.behavior.MonthlyBehaviorVO;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorMapper,UserBehavior> implements IUserBehaviorService {

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private BlogMapper  blogMapper;

    /**
     * 增加阅读计数（使用Lua脚本保证原子性）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementReadCount(Long blogId, Long userId) throws Exception {

        // 先检查是否存在缓存 如果 不存在缓存就直接写入数据库中
        String countKey = RedisPrefix.BLOG_READ_COUNT + blogId;
        String userReadKey = RedisPrefix.BLOG_USER_READ + blogId + ":" + userId;
        Object o = redisUtil.get(countKey);

        if(o == null){
            // 直接改数据库
            blogMapper.update(Wrappers.<Blog>lambdaUpdate()
                    .eq(Blog::getId, blogId)
                    .setSql("watch = watch + 1")
            );
            userBehaviorBlog(blogId, userId, BehaviorEnum.VIEW);
            return true;
        }else{
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
                userBehaviorBlog(blogId, userId, BehaviorEnum.VIEW);
                return true;
            } catch (Exception e) {
                // 5. 异常处理（降级,日志告警）
                log.error("执行阅读数计数Lua脚本失败，blogId:{}，userId:{}", blogId, userId, e);
                throw new RuntimeException("执行阅读数计数Lua脚本失败",e);
            }
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

    @Override
    public List<MonthlyBehaviorVO> getBlogBehaviorTrend(Long blogId) throws Exception {
        // 1 查最近12个月数据
        Date startTime = Date.from(
                LocalDate.now().minusMonths(11)
                        .withDayOfMonth(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        List<UserBehavior> list = userBehaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getBlogId, blogId)
                        .eq(UserBehavior::getIsDelete, 0)
                        .ge(UserBehavior::getCreateTime, startTime)
        );

        // 2 按 月份 + 行为类型 分组
        Map<String, Map<Integer, Long>> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            LocalDateTime time = item.getCreateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            return time.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                        },
                        Collectors.groupingBy(
                                UserBehavior::getBehaviorType,
                                Collectors.counting()
                        )
                ));

        // 3 构造最近12个月（补0🔥）
        List<MonthlyBehaviorVO> result = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            String month = LocalDate.now()
                    .minusMonths(i)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));

            Map<Integer, Long> behaviorMap = groupMap.getOrDefault(month, new HashMap<>());

            MonthlyBehaviorVO vo = new MonthlyBehaviorVO();
            vo.setMonth(month);
            vo.setViewCount(behaviorMap.getOrDefault(0, 0L));
            vo.setLikeCount(behaviorMap.getOrDefault(1, 0L));
            vo.setCommentCount(behaviorMap.getOrDefault(2, 0L));
            vo.setCollectCount(behaviorMap.getOrDefault(3, 0L));
            vo.setShareCount(behaviorMap.getOrDefault(4, 0L));

            result.add(vo);
        }

        return result;
    }


}
