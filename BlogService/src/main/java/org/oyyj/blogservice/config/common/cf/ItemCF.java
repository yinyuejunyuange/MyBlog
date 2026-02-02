package org.oyyj.blogservice.config.common.cf;

import jakarta.annotation.PostConstruct;
import org.oyyj.blogservice.dto.BlogTypeDTO;
import org.oyyj.blogservice.dto.Recommendation;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        Map<Long, List<String>> blogTypeMap = getBlogTypeMap();

        calculateItemSimilarity(blogTypeMap);
    }

    /**
     * 批量获取博客类型映射
     */
    private Map<Long, List<String>> getBlogTypeMap() {
        // 批量查询博客和类型，减少数据库查询次数
        List<BlogTypeDTO> allBlogTypes =blogMapper.getBlogTypeList(null);

        Map<Long, List<String>> blogTypeMap = allBlogTypes.stream()
                .collect(Collectors.groupingBy(
                        BlogTypeDTO::getBlogId,
                        Collectors.mapping(BlogTypeDTO::getBlogType, Collectors.toList())
                ));
        // 批量存储到redis中
        blogTypeMap.forEach((blogId, blogTypeDTOs) -> {
            redisUtil.setList(RedisPrefix.ITEM_TYPE+blogId,blogTypeDTOs);
        });

        return blogTypeMap;
    }



    /**
     * 计算两两物品之间的相似度  使用倒排索引优化
     */
    public void calculateItemSimilarity(Map<Long, List<String>> blogTypeMap){
       // 获取倒排索引
        Map<String,Set<Long>> typeToBlogs = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : blogTypeMap.entrySet()) {
            Long blogId = entry.getKey();
            for (String s : entry.getValue()) {
                // 生成倒排索引
                typeToBlogs.computeIfAbsent(s, k -> new HashSet<>()).add(blogId);
            }
        }
        // 向量化表示 创建每个博客的类型向量
        List<String> allTypes = new ArrayList<>(typeToBlogs.keySet());
        Map<Long,int[]> blogVectors = new HashMap<>(); // 存储每一个博客的向量信息
        for (Map.Entry<Long, List<String>> entry : blogTypeMap.entrySet()) {
            Long blogId = entry.getKey();
            int[] vector = new int[allTypes.size()];
            for (String s : entry.getValue()) {
                int i = allTypes.indexOf(s);
                if(i>=0){
                    vector[i] = 1; // 设置向量为1；
                }
            }
            blogVectors.put(blogId,vector);
        }

        int batch  = 1000;
        List<Long> blogIds = new ArrayList<>(blogTypeMap.keySet());
        // 使用并行流处理
        IntStream.range(0,(batch+blogIds.size()-1)/batch)  // 整数向上取整
                .parallel()
                .forEach(batchIndex -> {
                    // 分批次计算
                    int start = batch * batchIndex;
                    int end = Math.min(start + batch, allTypes.size());
                    for (int i = start; i < end; i++) {
                        Long blogId_i = blogIds.get(i);
                        int[] vector_i = blogVectors.get(blogId_i);

                        // 只计算有共同类型的博客
                        Set<Long> candidateBlogs = getCandidateBlogs(blogId_i, blogTypeMap, typeToBlogs);
                        // 计算相似度
                        Map<Long,Double> candidateSimilarities = new  HashMap<>();
                        for (Long blogId_j : candidateBlogs) {
                            if(blogId_i.equals(blogId_j)){
                                continue;
                            }
                            double similarity = calculateCategorySimilarity(vector_i, blogVectors.get(blogId_j));
                            if(similarity>0.1){
                                candidateSimilarities.put(blogId_j,similarity);
                            }
                        }
                        // 获取相似度最高的1000位
                        candidateSimilarities = getTopNSimilarities(candidateSimilarities);
                        // 存储到redis当中
                        redisUtil.setHashWithLongDouble(
                                RedisPrefix.ITEM_SIMILARITY + blogId_i,
                                candidateSimilarities
                        );
                    }

                });
    }

    /**
     * 获取候选博客（只获取有共同类型的博客）
     */
    private Set<Long> getCandidateBlogs(Long blogId, Map<Long, List<String>> blogTypeMap,
                                        Map<String, Set<Long>> typeToBlogs) {
        Set<Long> candidates = new HashSet<>();
        for (String type : blogTypeMap.get(blogId)) {
            candidates.addAll(typeToBlogs.getOrDefault(type, new HashSet<>()));
        }
        return candidates;
    }

    /**
     * 计算两个博客的类别相似度 修改同用户一样 都是使用余弦相似度
     *
     * @param vectorA
     * @param vectorB
     * @return
     */
    private double calculateCategorySimilarity(int[] vectorA, int[] vectorB ){
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if(normA == 0 || normB == 0){
            return 0.0;
        }

        return dotProduct / ( Math.sqrt(normA) * Math.sqrt(normB) );
    }

    // 在存储到Redis前，只保留相似度最高的N个
    private Map<Long, Double> getTopNSimilarities(Map<Long, Double> similarities) {
        // 使用优先队列（最小堆）维护Top-N
        PriorityQueue<Map.Entry<Long, Double>> minHeap = new PriorityQueue<>(
                similartyBlogMaxItemNum,
                Comparator.comparingDouble(Map.Entry::getValue)
        );

        for (Map.Entry<Long, Double> entry : similarities.entrySet()) {
            if (minHeap.size() < similartyBlogMaxItemNum) {
                minHeap.offer(entry);
            } else {
                assert minHeap.peek() != null;
                if (entry.getValue() > minHeap.peek().getValue()) {
                    minHeap.poll();
                    minHeap.offer(entry);
                }
            }
        }

        // 转换为Map
        Map<Long, Double> result = new LinkedHashMap<>();
        while (!minHeap.isEmpty()) {
            Map.Entry<Long, Double> entry = minHeap.poll();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
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
            Map<Long, Double> similarityItems = getSimilarityItems(k, 200);
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
    private Map<Long, Double> getSimilarityItems(Long itemId, int k ){
        Map<Long, Double> similarityItems = redisUtil.getHashWithLongDouble(RedisPrefix.ITEM_SIMILARITY+itemId);
        return similarityItems.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(k)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }


}
