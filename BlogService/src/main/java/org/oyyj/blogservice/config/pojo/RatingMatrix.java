package org.oyyj.blogservice.config.pojo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.UserBehavior;
import org.oyyj.blogservice.util.RedisUtil;
import org.oyyj.mycommon.common.RedisPrefix;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 用户 - 物品评分矩阵
 */
@Data
@Component
@Slf4j
public class RatingMatrix {

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private  RedisUtil redisUtil;

    /**
     * 项目启动时初始化
     * 逻辑： 先查询查询用户基数 如果用户基数小于等于5万 则  直接将表存入到本地 如果大于5万 则存储到redis中 本地仅存储 活跃度前20%的用户
     *
     */
    @PostConstruct
    public synchronized void init(){

        RLock lock = redissonClient.getLock(RedisPrefix.INIT_LOCK);
        try {
            boolean isLock = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if(isLock){
                String isInit = redisUtil.getString(RedisPrefix.INIT_RATING);
                if(Objects.isNull(isInit)){
                    log.debug("评分矩阵加载完成,完成时间：+{}",new Date().getTime());
                    return;
                }
                // 通过userBehavior获取所有用户Id 和 物品ID
                List<Long> userIdList = userBehaviorMapper.getUserIdList();
                // 分批次加载
                int order = 0;
                int batch = 1000;
                do {
                    order++;
                    List<Long> userIdBatch = userIdList.subList((order - 1)*batch, order * batch);
                    if(userIdBatch.isEmpty()){
                        log.warn("批次查询出现数据数量为空！");
                        break;
                    }
                    Map<Long, List<UserBehavior>> userBehaviorMap = userBehaviorMapper.selectList(Wrappers.<UserBehavior>lambdaQuery()
                            .in(UserBehavior::getUserId, userIdBatch)).stream().collect(Collectors.groupingBy(UserBehavior::getUserId));
                    Map<Long, List<UserBehavior>> finalUserBehaviorMap  = new ConcurrentHashMap<>(userBehaviorMap);

                    userIdBatch.parallelStream().forEach(id -> {
                        List<UserBehavior> behaviors = finalUserBehaviorMap.get(id);
                        Map<Long, List<UserBehavior>> idBehaviorMap = behaviors.stream().collect(Collectors.groupingBy(UserBehavior::getUserId));
                        Set<Long> userIds = idBehaviorMap.keySet();

                        for (Long userId : userIds) {
                            List<UserBehavior> userBehaviors = idBehaviorMap.get(userId);
                            calculateMatrixByUserId(userId, userBehaviors);
                        }
                    });
                }while (order * batch < userIdList.size());
                // 计算物品的平均得分
                calculateBlogAvgScore();
                redisUtil.set(RedisPrefix.INIT_RATING,RedisPrefix.INIT_FINISH);
            }else{
                // 锁获取失败 添加重试机制

            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
                log.debug("分布式锁释放+{}",new Date().getTime());
            }
        }
    }

    /**
     * 使用分段线性函数
     * @param blogValues
     * @return
     */
    private Map<Long,Double>  marginalisation(Map<Long ,Double> blogValues){
        if(blogValues == null || blogValues.isEmpty()){
            return Collections.emptyMap();
        }
        Map<Long,Double> result = new HashMap<>();
        // 排序提取原始评分
        List<Double> sortValue = blogValues.values()
                .stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if(sortValue.isEmpty()){
            // 无有效评分
            blogValues.forEach((blogId,rating)->{
                result.put(blogId,0.0);
            });
            return result;
        }

        // 计算分位数阈值
        double low = calculateQuantile(sortValue, 0.25); // 低
        double mid = calculateQuantile(sortValue, 0.5); // 中
        double high = calculateQuantile(sortValue, 0.75); // 高

        // 处理阈值相等情况
        if(low == mid){
            mid = mid+0.001;
        }
        if(high == mid){
            high = mid+0.001;
        }

        for (Map.Entry<Long, Double> entry : blogValues.entrySet()) {
            Long blogId = entry.getKey();
            Double itemValue = entry.getValue();

            double boundScore;

            if(itemValue == null || Double.isNaN(itemValue)){
                boundScore = 0.0;
            }else if(itemValue <= low){
                boundScore = (itemValue / low)* 0.25;
            }else if(itemValue <= mid){
                boundScore = 0.25 + ((itemValue - low)/(mid-low))*0.25;
            }else if(itemValue <= high){
                boundScore = 0.5+ ((itemValue - mid)/(high-low))*0.25;
            }else{
                boundScore = 1;
            }
            result.put(blogId,boundScore);
        }
        return result;
    }

    /**
     * 使用线性插值法 获取列表中的分位数
     * @param sortValue
     * @param quantile
     * @return
     */
    private double calculateQuantile(List<Double> sortValue , double quantile){
        if(quantile <= 0 ){
            return sortValue.getFirst();
        }

        if(quantile>= 1){
            return sortValue.getLast();
        }

        int n = sortValue.size();
        double index = quantile * (n - 1); // 获取对应的索引位置
        int lowIndex = (int) Math.floor(index); // 下取整数
        int upperIndex = (int) Math.ceil(index); //  上取整数
        Double lowValue = sortValue.get(lowIndex);
        Double upperValue = sortValue.get(upperIndex);

        if(Objects.equals(lowValue, upperValue)){
            return lowValue;
        }else{
            double fraction = index - lowIndex;
            return lowIndex + fraction * (upperValue - lowValue);
        }
    }

    /**
     * 计算行为评分
     * @param behaviors
     * @return
     */
    private double calculateRating(List<UserBehavior> behaviors){
        return  behaviors.stream().map(behavior -> {
            double weight = behavior.getBehaviorType().getWeight();

            double timeDecay = calculateTimeDecay(behavior.getCreateTime());
            // 计算综合评分 = 行为权重*时间衰减因子
            return weight * timeDecay;
        }).reduce(0d, Double::sum);
    }



    /**
     * 时间衰减计算
     */
    private double calculateTimeDecay(Date behaviorTime) {
        long daysBetween = ChronoUnit.DAYS.between(behaviorTime.toInstant(), Instant.now());
        return Math.exp(-0.1 * Math.max(0, daysBetween)); // 衰减因子
    }

    /**
     * 计算单个用户的 用户-物品 行为矩阵
     * @param userId
     * @param userBehaviors
     */
    private void calculateMatrixByUserId(Long userId , List<UserBehavior> userBehaviors){
        Map<Long, List<UserBehavior>> blogBehavior = userBehaviors.stream().collect(Collectors.groupingBy(UserBehavior::getBlogId));
        Map<Long ,Double> blogValues = new HashMap<>();
        blogBehavior.keySet().forEach(blogId -> {
            List<UserBehavior> userBlogBehaviors = blogBehavior.get(blogId);
            double rating = calculateRating(userBlogBehaviors);
            blogValues.put(blogId,rating);
        });
        // 计算线性分段后的结果
        Map<Long, Double> marginalisation = marginalisation(blogValues);
        // 存储到redis
        redisUtil.setHashWithLongDouble(RedisPrefix.RATING_MATRIX_USER+userId,marginalisation);
        // 计算平均值并存储到redis中
        if(!marginalisation.isEmpty()){
            Double reduce = marginalisation.entrySet().parallelStream().map(Map.Entry::getValue).reduce(0d, Double::sum);
            redisUtil.set(RedisPrefix.AVG_RATING_USER+userId,reduce/marginalisation.size());
        }
        // 计算 物品 - 用户 矩阵并存储到redis中
        calculateBlogMatrixByUserId(userId,marginalisation);
    }

    /**
     * 存储物品-用户 矩阵
     * @param userId
     * @param marginalisation
     */
    private void calculateBlogMatrixByUserId(Long userId , Map<Long, Double> marginalisation ){

        if(marginalisation.isEmpty()){
            return;
        }
        Set<Long> blogIds = marginalisation.keySet();
        blogIds.parallelStream().forEach(blogId -> {
            Map<Long, Double> hashWithLongDouble = redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_ITEM + blogId);
            if(Objects.nonNull(hashWithLongDouble)){
                hashWithLongDouble.put(userId,marginalisation.get(blogId));
            }else{
                hashWithLongDouble = new HashMap<>();
                hashWithLongDouble.put(userId,marginalisation.get(blogId));
            }

            redisUtil.setHashWithLongDouble(RedisPrefix.RATING_MATRIX_ITEM + blogId,hashWithLongDouble);
        });
    }


    /**
     * 计算物品的平均得分并上传到redis中
     *
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void calculateBlogAvgScore(){
        List<UserBehavior> userBehaviors = userBehaviorMapper.selectList(Wrappers.<UserBehavior>lambdaQuery());
        Set<Long> blogIds = userBehaviors.parallelStream().map(UserBehavior::getBlogId).collect(Collectors.toSet());
        blogIds.parallelStream().forEach(blogId -> {
            Map<Long, Double> hashWithLongDouble = redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_ITEM + blogId);
            if(Objects.nonNull(hashWithLongDouble)){
                Double totalScore = hashWithLongDouble.entrySet().parallelStream().map(Map.Entry::getValue).reduce(0d, Double::sum);
                redisUtil.set(RedisPrefix.AVG_RATING_ITEM+blogId, totalScore/hashWithLongDouble.size());
            }
        });
    }

    /**
     * 更新用户的矩阵数据
     * @param userId
     */
    public void updateMatrix(Long userId){
        List<UserBehavior> userBehaviors = userBehaviorMapper.selectList(Wrappers.<UserBehavior>lambdaQuery().eq(UserBehavior::getUserId, userId));
        calculateMatrixByUserId(userId,userBehaviors);
    }


}
