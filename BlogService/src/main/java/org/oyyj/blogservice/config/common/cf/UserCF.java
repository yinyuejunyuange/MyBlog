package org.oyyj.blogservice.config.common.cf;

import org.checkerframework.checker.units.qual.C;
import org.oyyj.blogservice.config.pojo.RatingMatrix;
import org.oyyj.blogservice.dto.Recommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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

    private Map<Long,Map<Long,Double>> userSimilarityMatrix; // 用户相似度矩阵

    /**
     * 计算用户相似度
     */
    // todo 添加从redis中获取
    public void calculateUserSimilarityMatrix(){

        Map<Long, ConcurrentHashMap<Long, Double>> userItemMatrix = ratingMatrix.getUserItemMatrix();
        List<Long> userIds = new ArrayList<>(userItemMatrix.keySet()); // 存储用户以及相似度评分

        userSimilarityMatrix = new HashMap<>();

        for (Long userId : userIds) {
            Map<Long, Double> ratingOne = userItemMatrix.get(userId);
            Map<Long, Double> userRating = new HashMap<>(ratingOne);
            for (Long userIdTwo : userIds) {
                if(userIdTwo.equals(userId)){
                    continue;
                }
                Map<Long, Double> ratingTwo = userItemMatrix.get(userIdTwo);
                double similarity = calculatePersonSimilarity(ratingOne, ratingTwo);
                userRating.put(userId, similarity);
            }

            userSimilarityMatrix.put(userId, userRating);
        }
    }

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
        commonBlog.removeAll(ratingTwo.keySet());

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

        // todo 从redis中获取短时间用户已经看过的数据

        List<Long> similarUsers = findSimilarUsers(userId, 20);

        // 获取这些用户喜欢的物品（排除目标已经接触的）
        Map<Long ,Double> candidateItems = new  HashMap<>();
        Set<Long> userInteractedItems = ratingMatrix.getUserItemMatrix()
                .getOrDefault(userId, new ConcurrentHashMap<>()).keySet();

        // 平均值
        double userAvgRating = ratingMatrix.getUserAvgMatrix().getOrDefault(userId, 0.0);

        for (Long similarUser : similarUsers) {
            double userSimilarity = getUserSimilarity(userId, similarUser);
            if(similarUser<=0){
                continue;
            }

            Map<Long, Double> userRating = ratingMatrix.getUserItemMatrix().getOrDefault(similarUser, new ConcurrentHashMap<>());
            double similarUserAvg = ratingMatrix.getUserAvgMatrix().getOrDefault(similarUser, 0.0);

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
                candidateItems.merge(itemId, predictedRating, Double::sum);

            }
        }

        return candidateItems.entrySet().stream()
                .sorted(Map.Entry.<Long,Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> new Recommendation(entry.getKey(),entry.getValue()))
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

        for (Long otherUserId : ratingMatrix.getUserItemMatrix().keySet()) {
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
        return calculatePersonSimilarity(
                ratingMatrix.getUserItemMatrix().getOrDefault(userIdOne,new ConcurrentHashMap<>()),
                ratingMatrix.getUserItemMatrix().getOrDefault(userIdTwo,new ConcurrentHashMap<>())
        );
    }

}
