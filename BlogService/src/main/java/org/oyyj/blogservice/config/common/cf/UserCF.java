package org.oyyj.blogservice.config.common.cf;

import org.oyyj.blogservice.config.pojo.RatingMatrix;
import org.oyyj.blogservice.config.pojo.UserActivityLevel;
import org.oyyj.blogservice.dto.Recommendation;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于用户的协同过滤算法
 */
@Component
public class UserCF {

    @Autowired
    private  RatingMatrix ratingMatrix;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserBehaviorMapper  userBehaviorMapper;

    private Map<Long,Map<Long,Double>> userSimilarityMatrix; // 用户相似度矩阵


    /**
     * 计算皮尔逊相关系数
     * @param ratingOne
     * @param ratingTwo
     * @return
     */
    private double calculatePersonSimilarity(Map<Long,Double> ratingOne,
                                             Map<Long,Double> ratingTwo){
        // 获取共同有评分的博客
        Set<Long> commonBlog = new HashSet<>(ratingOne.keySet());
        commonBlog.retainAll(ratingTwo.keySet());

        if(commonBlog.size()<2){
            return 0;
        }

        // 计算均值
        double sumOne = commonBlog
                .stream()
                .mapToDouble(ratingOne::get).sum();
        double sumTwo = commonBlog
                .stream()
                .mapToDouble(ratingTwo::get).sum();

        double meanOne = sumOne / commonBlog.size();
        double meanTwo = sumTwo / commonBlog.size();

        // 计算分子 分母
        double numerator = 0.0;
        double denominatorOne = 0.0;
        double denominatorTwo = 0.0;

        for (Long item : commonBlog) {
            double diffOne = ratingOne.get(item) - meanOne;
            double diffTwo = ratingTwo.get(item) - meanTwo;
            numerator += diffOne * diffTwo;
            denominatorOne += diffOne*diffOne;
            denominatorTwo += diffTwo*diffTwo;
        }

        if(denominatorOne == 0 || denominatorTwo == 0){
            return 0;
        }

        return numerator / Math.sqrt(denominatorOne )*Math.sqrt( denominatorTwo);

    }

    /**
     * 为用户推荐
     * @param userId
     * @param topN
     * @return
     */
    public List<Recommendation> recommendForUser(Long userId,int topN){

        List<Long> similarUsers = findSimilarUsers(userId, 20);

        // 获取这些用户喜欢的物品（排除目标已经接触的）
        // 3. 初始化两个Map用于加权平均
        Map<Long, Double> weightedScores = new HashMap<>(); // 加权分数和
        Map<Long, Double> weightSums = new HashMap<>();     // 权重和

        Set<Long> userInteractedItems = new ConcurrentHashMap<>(redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_USER + userId)).keySet();
        // 平均值
        double userAvgRating = Double.parseDouble(redisUtil.get(RedisPrefix.AVG_RATING_USER+userId).toString());

        for (Long similarUser : similarUsers) {
            double userSimilarity = getUserSimilarity(userId, similarUser);
            if(similarUser<=0){
                continue;
            }

            Map<Long, Double> userRating = new ConcurrentHashMap<>(redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_USER + similarUser));
            double similarUserAvg = Double.parseDouble(redisUtil.get(RedisPrefix.AVG_RATING_USER+similarUser).toString());

            // 预测计算公式：预测评分 = 用户平均分 + Σ[相似度 × (邻居评分 - 邻居平均分)] / Σ|相似度|
            for (Map.Entry<Long, Double> entry : userRating.entrySet()) {
                Long itemId = entry.getKey();
                // 排除用户已经接触过的博客
                if(userInteractedItems.contains(itemId)){
                    continue;
                }

                double rating = entry.getValue();
                // 预测分数 = 用户平均分 + 相似度 *（邻居评分 - 邻居平均分）
                double predictedRating = userAvgRating + userSimilarity * (rating - similarUserAvg);

                // 使用相加 合并结果
                // 7. 关键：使用加权平均而不是累加或覆盖
                weightedScores.merge(itemId,
                        predictedRating * Math.abs(userSimilarity),  // 加权分数
                        Double::sum);
                weightSums.merge(itemId,
                        Math.abs(userSimilarity),  // 权重
                        Double::sum);

            }
        }

        // 8. 计算最终评分（加权平均）
        Map<Long, Double> finalScores = new HashMap<>();
        for (Long itemId : weightedScores.keySet()) {
            double totalWeight = weightSums.get(itemId);
            if (totalWeight > 0) {
                finalScores.put(itemId,
                        weightedScores.get(itemId) / totalWeight);
            }
        }

        // 9. 返回结果
        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> new Recommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

    }

    /**
     * 寻找与指定用户相似K个用户
     * @param userId
     * @param k
     * @return
     */
    private List<Long> findSimilarUsers(Long userId, int k){
        Map<Long,Double> similarities = new HashMap<>();

        List<UserActivityLevel> userActivityLevel = userBehaviorMapper.getUserActivityLevel();
        List<Long> userIds = userActivityLevel.parallelStream().map(UserActivityLevel::getUserId).toList();

        for (Long otherUserId : userIds) {
            if(userId.equals(otherUserId)){
                continue;
            }
            double userSimilarity = getUserSimilarity(userId, otherUserId);
            if(userSimilarity > 0){
                similarities.put(otherUserId, userSimilarity);
            }
        }

        return similarities.entrySet().stream()
                .sorted(Map.Entry.<Long,Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

    }

    /**
     * 获取两用户的相似度
     * @param userIdOne
     * @param userIdTwo
     * @return
     */
    private double getUserSimilarity(Long userIdOne, Long userIdTwo){

        Map<Long, Double> userOneBlogMap = new ConcurrentHashMap<>(redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_USER + userIdOne));
        Map<Long, Double> userTwoBlogMap = new ConcurrentHashMap<>(redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_USER + userIdTwo));

        return calculatePersonSimilarity(userOneBlogMap,userTwoBlogMap);
    }

}
