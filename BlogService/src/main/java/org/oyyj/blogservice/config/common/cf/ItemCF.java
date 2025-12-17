package org.oyyj.blogservice.config.common.cf;

import org.oyyj.blogservice.config.pojo.RatingMatrix;
import org.oyyj.blogservice.dto.BlogTypeDTO;
import org.oyyj.blogservice.dto.Recommendation;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于物品内容相似度的推荐功能实现
 */
@Component
public class ItemCF {

    @Autowired
    private RatingMatrix ratingMatrix;

    @Autowired
    private BlogMapper blogMapper;
    // 博客相似度举证
    private Map<Long,Map<Long,Double>> itemSimilarityMatrix;


    /**
     * 计算两两物品之间的相似度
     */
    // todo 添加到redis中
    public void calculateItemSimilarity(){
        Map<Long, ConcurrentHashMap<Long, Double>> itemUserMatrix = ratingMatrix.getItemUserMatrix();
        List<Long> blogIds = new ArrayList<>(itemUserMatrix.keySet());
        itemSimilarityMatrix = new HashMap<>();
        if(blogIds.isEmpty()){
            return ;
        }

        Map<Long, List<BlogTypeDTO>> blogTypeMap = blogMapper.getBlogTypeList(blogIds)
                .stream()
                .collect(Collectors.groupingBy(BlogTypeDTO::getBlogId));

        for (Long blogId : blogIds) {
            List<String> typeListOne = blogTypeMap.getOrDefault(blogId,new ArrayList<>()).stream().map(BlogTypeDTO::getBlogType).toList();
            Map<Long,Double> blogTypeSimilarity = new HashMap<>();
            for (Long blogTwoId : blogIds) {
                if(blogTwoId.equals(blogId)){
                    continue;
                }
                // 判断对称方向是否存在数据
                if(itemSimilarityMatrix.containsKey(blogTwoId)){
                    Map<Long, Double> otherMap = itemSimilarityMatrix.get(blogTwoId);
                    if(otherMap.containsKey(blogId)){
                        blogTypeSimilarity.put(blogTwoId,otherMap.get(blogId));
                        continue;
                    }
                }
                List<String> typeListTwo = blogTypeMap.getOrDefault(blogTwoId, new ArrayList<>()).stream().map(BlogTypeDTO::getBlogType).toList();
                double rating = calculateCategorySimilarity(typeListOne, typeListTwo);
                blogTypeSimilarity.put(blogTwoId, rating);
            }
            itemSimilarityMatrix.put(blogId, blogTypeSimilarity);
        }

    }

    /**
     * 计算两个博客的类别相似度 杰卡德相似度
     *
     * @param oneList
     * @param twoList
     * @return
     */
    private double calculateCategorySimilarity(List<String> oneList, List<String> twoList){
        if(oneList.isEmpty() && twoList.isEmpty()){
            return 0.2;
        }

        // 计算交集大小
        long interSize = oneList.stream().filter(twoList::contains).count();
        // 并集大小
        long unionSize = oneList.size() + twoList.size() - interSize;
        return (double) interSize /unionSize;
    }

    /**
     * 为用户推荐
     * @param userId
     * @param topN
     * @return
     */
    public List<Recommendation> recommendForUser(Long userId,int topN){

        // todo 从redis中获取短时间用户已经看过的数据

        Map<Long, Double> userBlogMap = ratingMatrix.getUserItemMatrix().get(userId);

        Map<Long,Double> predictRatingMap = new HashMap<>() ; // 预测评分

        // 获取用户喜欢的博客相似的博客
        userBlogMap.forEach((k,v)->{
            Map<Long, Double> similarityItems = getSimilarityItems(k, 50);
            for (Map.Entry<Long, Double> entry : similarityItems.entrySet()) {
                Long similarItemId = entry.getKey();
                Double similarValue = entry.getValue();
                // 排除用户已经接触过的
                if(userBlogMap.containsKey(similarItemId)){
                    continue;
                }
                // 预测评分
                Double predictedRating = predictRatingMap.getOrDefault(similarItemId, 0.0);
                predictedRating += v* similarValue;
                predictRatingMap.put(similarItemId, predictedRating);
            }
        });

        // 排序并返回
        return predictRatingMap.entrySet().stream()
                .sorted(Map.Entry.<Long,Double>comparingByValue().reversed())
                .limit(topN)
                .map(item ->new  Recommendation(item.getKey(), item.getValue()))
                .toList();
    }

    /**
     * 选择物品最相似的几个商品
     * @param itemId
     * @param k
     * @return
     */
    private Map<Long, Double> getSimilarityItems(Long itemId, int k){
        Map<Long, Double> similarityItems = itemSimilarityMatrix.getOrDefault(itemId, new HashMap<>());
        return similarityItems.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(k)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));


    }


}
