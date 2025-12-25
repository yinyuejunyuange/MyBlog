package org.oyyj.blogservice.config.common.cf;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import org.oyyj.blogservice.config.pojo.RatingMatrix;
import org.oyyj.blogservice.dto.BlogTypeDTO;
import org.oyyj.blogservice.dto.Recommendation;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.mycommon.common.RedisPrefix;
import org.oyyj.mycommon.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于物品内容相似度的推荐功能实现
 */
@Component
public class ItemCF {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private BlogMapper blogMapper;

    @Value("${similarty.blog-max-item-num}")
    private int similartyBlogMaxItemNum;


    @PostConstruct
    public void init(){
        List<Long> allBlogIds = blogMapper.selectList(Wrappers.<Blog>lambdaQuery()
                .select(Blog::getId)
        ).stream().map(Blog::getId).toList();

        int batchSize = 1000;
        int order = 0;
        int allSize = allBlogIds.size();
        while(order*batchSize <= allSize){
            List<Long> blogIds = allBlogIds.subList(order*batchSize,(order+1)*batchSize);
            // 将现有的博客的类别存储到redis中
            Map<Long, List<BlogTypeDTO>> blogTypeMap = blogMapper.getBlogTypeList(blogIds)
                    .stream()
                    .collect(Collectors.groupingBy(BlogTypeDTO::getBlogId));

            blogTypeMap.forEach((blogId, blogTypeDTOs) -> {
                List<String> typeList = blogTypeDTOs.stream().map(BlogTypeDTO::getBlogType).toList();
                redisUtil.setList(RedisPrefix.ITEM_TYPE+blogId,typeList);
            });
            order++;
        }
        calculateItemSimilarity();
    }

    /**
     * 计算两两物品之间的相似度
     */
    public void calculateItemSimilarity(){
        List<Long> blogIds = blogMapper.selectList(Wrappers.<Blog>lambdaQuery()
                .select(Blog::getId)
        ).stream().map(Blog::getId).toList();

        if(blogIds.isEmpty()){
            return ;
        };

        for (Long blogId : blogIds) {
            List<String> typeListOne = redisUtil.getList(RedisPrefix.ITEM_TYPE+blogId);
            Map<Long,Double> blogTypeSimilarity = new HashMap<>();
            for (Long blogTwoId : blogIds) {
                if(blogTwoId.equals(blogId)){
                    continue;
                }
                Map<Long, Double> twoTypeList = redisUtil.getHashWithLongDouble(RedisPrefix.ITEM_SIMILARITY + blogTwoId);
                // 判断对称方向是否存在数据
                if( twoTypeList != null && twoTypeList.containsKey(blogTwoId)){
                    Double score = twoTypeList.get(blogTwoId);
                    if(Objects.nonNull(score)){
                        blogTypeSimilarity.put(blogTwoId,score);
                        continue;
                    }
                }
                List<String> typeListTwo = redisUtil.getList(RedisPrefix.ITEM_TYPE+blogTwoId);
                double rating = calculateCategorySimilarity(typeListOne, typeListTwo);
                if(blogTypeSimilarity.size() > similartyBlogMaxItemNum){
                    // 超过数量择优处理
                    Set<Map.Entry<Long, Double>> entries = blogTypeSimilarity.entrySet();

                    Long replaceId = null;
                    Double minValue = rating;
                    for (Map.Entry<Long, Double> entry : entries) {
                        if(entry.getValue()<minValue){
                            replaceId = entry.getKey();
                            minValue = entry.getValue();

                        }
                    }
                    if(replaceId != null){
                        blogTypeSimilarity.remove(replaceId);
                        blogTypeSimilarity.put(blogTwoId, rating);
                    }
                }else{
                    blogTypeSimilarity.put(blogTwoId, rating);
                }

            }
            redisUtil.setHashWithLongDouble(RedisPrefix.ITEM_SIMILARITY + blogId,blogTypeSimilarity);
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

        Map<Long, Double> userBlogMap = redisUtil.getHashWithLongDouble(RedisPrefix.RATING_MATRIX_USER + userId);

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
        Map<Long, Double> similarityItems = redisUtil.getHashWithLongDouble(RedisPrefix.ITEM_SIMILARITY+itemId);
        return similarityItems.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(k)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }


}
