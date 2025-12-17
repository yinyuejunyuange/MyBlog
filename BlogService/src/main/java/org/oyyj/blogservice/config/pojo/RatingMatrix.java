package org.oyyj.blogservice.config.pojo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.blogservice.pojo.UserBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    // 用户ID ->(物品ID -> 评分)
    private Map<Long,ConcurrentHashMap<Long,Double>> userItemMatrix;

    // 物品ID ->(用户ID -> 评分)
    private Map<Long ,ConcurrentHashMap<Long,Double>> itemUserMatrix;  // 便于访问避免反复遍历

    // 用户平均评分
    private Map<Long, Double> userAvgMatrix;

    // 物品平均评分
    private Map<Long , Double> itemAvgMatrix;

    private volatile int initialized = 0;

    public RatingMatrix() {
        userItemMatrix = new ConcurrentHashMap<>();
        itemUserMatrix = new ConcurrentHashMap<>();
        userAvgMatrix = new ConcurrentHashMap<>();
        itemAvgMatrix = new ConcurrentHashMap<>();
    }


    /**
     * 项目启动时初始化
     */
    @PostConstruct
    public synchronized void init(){

        if(initialized != 0){
            log.info("ratingMatrix initialized");
        }else{
            initialized = 1;
            List<UserBehavior> behaviors = userBehaviorMapper.selectList(Wrappers.<UserBehavior>lambdaQuery());

            Map<Long, List<UserBehavior>> idBehaviorMap = behaviors.stream().collect(Collectors.groupingBy(UserBehavior::getUserId));
            Set<Long> userIds = idBehaviorMap.keySet();

            for (Long userId : userIds) {
                List<UserBehavior> userBehaviors = idBehaviorMap.get(userId);
                calculateMatrixByUserId(userId, userBehaviors);
            }

            this.calculateUserAvgRating();
            this.calculateItemAvgRating();
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
     * 添加评分记录
     *
     * @param userId
     * @param itemId
     * @param rating
     */
    public void addRating(Long userId, Long itemId ,Double rating){
        // 用户-物品矩阵
        Map<Long, Double> itemMap = userItemMatrix.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        itemMap.put(itemId,rating);
        // 物品-用户矩阵
        Map<Long, Double> userMap = itemUserMatrix.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>());
        userMap.put(userId,rating);
    }

    /**
     * 计算用户平均分 (定期更新)
     */
    @Scheduled(fixedRate = 60000)
    public void calculateUserAvgRating(){
        calculateAvgRating(userItemMatrix, userAvgMatrix);
    }

    /**
     * 计算物品平均分 （定期更新）
     */
    @Scheduled(fixedRate = 30000)
    public void calculateItemAvgRating(){
        calculateAvgRating(itemUserMatrix, itemAvgMatrix);
    }

    private void calculateAvgRating(Map<Long, ConcurrentHashMap<Long, Double>> itemUserMatrix, Map<Long, Double> itemAvgMatrix) {
        itemUserMatrix.entrySet().parallelStream().forEach(item->{
            Long userId = item.getKey();
            Map<Long, Double> rating = item.getValue();

            double sum = rating.values().stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / rating.size();
            itemAvgMatrix.put(userId, avg);
        });
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
        marginalisation.forEach((blogId,rating)->{
            addRating(userId,blogId,rating);
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
