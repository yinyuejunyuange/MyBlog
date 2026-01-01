package org.oyyj.blogservice.service;

import org.oyyj.blogservice.pojo.Blog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 博客相似度得分计算 --- 基于博客内容的推荐
 */

public class BlogSimilarityService {

    // 维度权重配置（根据业务调整，总和为1）
    private static final BigDecimal CATEGORY_WEIGHT = new BigDecimal("0.6"); // 类别权重
    private static final BigDecimal VIEW_WEIGHT = new BigDecimal("0.1"); // 观看权重
    private static final BigDecimal LIKE_WEIGHT = new BigDecimal("0.1"); // 点赞权重
    private static final BigDecimal COMMENT_WEIGHT = new BigDecimal("0.1"); // 评论量权重
    private static final BigDecimal SENTIMENT_WEIGHT = new BigDecimal("0.1"); // 评论情绪权重

    private List<Blog> allBlogs;

    // 全局统计值
    private Long minViewCount;
    private Long maxViewCount;
    private Long minLikeCount;
    private Long maxLikeCount;
    private Long minCommentCount;
    private Long maxCommentCount;

    /**
     * 初始化全局统计值
     */
    private void initGlobalStats(){
        // 观看量统计
        minViewCount = allBlogs.stream().mapToLong(Blog::getWatch).min().orElse(0);
        maxViewCount = allBlogs.stream().mapToLong(Blog::getWatch).max().orElse(1);
        // 点赞数统计
        minLikeCount = allBlogs.stream().mapToLong(Blog::getKudos).min().orElse(0);
        maxLikeCount = allBlogs.stream().mapToLong(Blog::getKudos).max().orElse(1);
        // 评论量统计
        minCommentCount = allBlogs.stream().mapToLong(Blog::getCommentNum).min().orElse(0);
        maxCommentCount = allBlogs.stream().mapToLong(Blog::getCommentNum).max().orElse(1);
    }

    public BlogSimilarityService(List<Blog> allBlogs){
        if(allBlogs == null || allBlogs.isEmpty()){
            throw new IllegalArgumentException("博客列表不可为空！");
        }
        this.allBlogs = allBlogs;
        initGlobalStats();
    }

    /**
     * min - max标准化处理  将连续数据都处理到 0-1区间
     * @param value
     * @param max
     * @param min
     * @return
     */
    private BigDecimal minMaxNormalize(Long value, Long max,Long min){
        if (max.equals(min)) {
            return new BigDecimal("0.5");
        }

        return new BigDecimal(value - min).divide(new BigDecimal(max - min), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算两个博客类别相似度 （杰卡德相似度 交集大小/并集大小）
     *
     * @param one
     * @param two
     * @return
     */
    private BigDecimal calculateCategorySimilarity(Blog one, Blog two){
        HashSet<String> oneTypes = new HashSet<>(Optional.ofNullable(one.getTypeList()).orElse(Collections.emptyList()));
        HashSet<String> twoTypes = new HashSet<>(Optional.ofNullable(two.getTypeList()).orElse(Collections.emptyList()));

        if(oneTypes.isEmpty() && twoTypes.isEmpty()){
            return new BigDecimal("0.2");
        }

        // 交集大小
        long interSize = oneTypes.stream().filter(twoTypes::contains).count();
        long unionSize = oneTypes.size() + twoTypes.size() - interSize;

        return new BigDecimal(interSize).divide(new BigDecimal(unionSize), 2, RoundingMode.HALF_UP);

    }

    /**
     * 构建加权向量 用于计算余弦相似度
     *
     * @param one
     * @return
     */
    private double[] buildWeightFeatureVector(Blog one){
        // 特征标准化
        double normalizedView = minMaxNormalize(one.getWatch(), maxViewCount, minViewCount).doubleValue();
        double normalizedKudos = minMaxNormalize(one.getKudos(), maxLikeCount, minLikeCount).doubleValue();
        double normalizedComment = minMaxNormalize(one.getCommentNum(), maxCommentCount, minCommentCount).doubleValue();

        // 计算情绪得分 确保处于0-1区间
        double commentPrice = Math.max(0.0, Math.min(1.0, one.getCommentPrice().doubleValue()));

        return new double[]{
                0.0,
                normalizedView * VIEW_WEIGHT.doubleValue(),
                normalizedKudos * LIKE_WEIGHT.doubleValue(),
                normalizedComment * COMMENT_WEIGHT.doubleValue(),
                commentPrice * SENTIMENT_WEIGHT.doubleValue()
        };
    }

    /**
     * 计算余弦相似度
     *
     * @param vOne 向量1
     * @param vTwo 向量2
     * @return
     */
    private double calculateCosSimilarity(double[] vOne, double[] vTwo ){
        if(vOne.length != vTwo.length){
            throw new IllegalArgumentException("两向量维度不一致!");
        }

        double dot = 0.0 ; // 点积
        double normOne = 0.0; // 向量1的模
        double normTwo = 0.0; // 向量2的模

        for (int i = 0; i < vOne.length; i++) {
            dot += vOne[i] * vTwo[i];
            normOne += Math.pow(vOne[i], 2);
            normTwo += Math.pow(vTwo[i], 2);
        }

        // 避免除以0报错
        if(normOne == 0 || normTwo == 0){
            return 0.0;
        }

        return dot/(Math.sqrt(normOne) * Math.sqrt(normTwo));
    }

    /**
     * 得出相似度信息
     *
     * @param one
     * @param two
     * @return
     */
    public double calculateBlogSimilarity(Blog one, Blog two){
        // 计算类别相似度
        double categorySim = calculateCategorySimilarity(one, two).doubleValue();

        // 构建两个博客的加权特征向量
        double[] vOne = buildWeightFeatureVector(one);
        double[] vTwo = buildWeightFeatureVector(two);

        vOne[0] = categorySim * CATEGORY_WEIGHT.doubleValue();
        vTwo[0] = categorySim * CATEGORY_WEIGHT.doubleValue();

        return calculateCosSimilarity(vOne, vTwo);
    }

    /**
     * 查找与目标博客最相似的TopN博客
     * @param targetBlogId 目标博客ID
     * @param topN 返回数量
     * @return 排序后的相似博客列表（按相似度降序）
     */
    public List<Map.Entry<Long, Double>> findTopSimilarBlogs(Long targetBlogId, int topN) {
        // 查找目标博客
        Blog targetBlog = allBlogs.stream()
                .filter(blog -> blog.getId().equals(targetBlogId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("目标博客不存在：" + targetBlogId));

        // 计算与所有其他博客的相似度
        Map<Long, Double> similarityMap = new HashMap<>();
        for (Blog blog : allBlogs) {
            if (!blog.getId().equals(targetBlogId)) { // 排除自身
                double similarity = calculateBlogSimilarity(targetBlog, blog);
                similarityMap.put(blog.getId(), similarity);
            }
        }

        // 按相似度降序排序，取TopN
        return similarityMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topN)
                .collect(Collectors.toList());
    }
//
//    public static void main(String[] args) {
//        // 1. 构造测试博客数据（5篇博客，覆盖不同类别和数值特征）
//        List<Blog> allBlogs = List.of(
//                // 目标博客：Java+后端，高观看/点赞/评论，正面情绪
//                new Blog(1L, null,null,null,null,null,null,null, List.of("java", "后端"),null, 67L,null, 687L, 5L, new BigDecimal("0.9")),
//                // 相似博客1：Java+后端，中等数值，正面情绪（高相似）
//                new Blog(2L,null,null,null,null,null,null,null, List.of("VUE", "前端","界面美化"),null, 908L, null ,15000L, 1L, new BigDecimal("0.8")),
//                // 相似博客2：Java+人工智能，高数值，正面情绪（中相似）
//                new Blog(3L,null,null,null,null,null,null,null, List.of("C++", "游戏"),null, 90L,null, 1800L, 4L, new BigDecimal("0.9")),
//                // 相似博客3：Python+前端，低数值，中性情绪（低相似）
//                new Blog(4L, null,null,null,null,null,null,null, List.of("python", "前端"),null , 3L, null, 50L, 2L, new  BigDecimal("0.5")),
//                // 相似博客4：后端，低数值，负面情绪（极低相似）
//                new Blog(5L,null,null,null,null,null,null,null,  List.of("后端"), null ,2L, null, 30L, 5L, new BigDecimal("0.2"))
//        );
//
//        // 2. 初始化相似度服务
//        BlogSimilarityService similarityService = new BlogSimilarityService(allBlogs);
//
//        // 3. 计算目标博客（B001）与其他博客的相似度
//        System.out.println("=== 博客B001与其他博客的相似度 ===");
//        for (Blog blog : allBlogs) {
//            if (blog.getId() != 1L) {
//                double similarity = similarityService.calculateBlogSimilarity(
//                        allBlogs.get(0), blog);
//                System.out.printf("B001 vs %s：相似度=%.3f%n", blog.getId(), similarity);
//            }
//        }
//
//        // 4. 查找与B001最相似的Top2博客
//        System.out.println("\n=== 与B001最相似的Top2博客 ===");
//        List<Map.Entry<Long, Double>> topSimilar = similarityService.findTopSimilarBlogs(1L, 2);
//        for (Map.Entry<Long, Double> entry : topSimilar) {
//            System.out.printf("博客ID：%s，相似度=%.3f%n", entry.getKey(), entry.getValue());
//        }
//    }


}
