package org.oyyj.mycommonbase.common;

import lombok.Data;

@Data
public class RedisPrefix {

    public static final String INIT_RATING = "init:rating:"; // redis加载的 初始化 分布式锁

    public static final String INIT_LOCK = "init:lock:"; // redis加载的 初始化加锁 分布式锁

    public static final String INIT_FINISH = "1";

    public static final String RATING_MATRIX_USER="rating:Matrix:user:";  // redis 前缀 用户博客评分Map

    public static final String AVG_RATING_USER= "avg:rating:user:"; // redis 前缀 用户平均物品评分

    public static final String RATING_MATRIX_ITEM="rating:Matrix:item:";  // redis 前缀 博客用户评分Map

    public static final String AVG_RATING_ITEM="avg:rating:item:"; // redis 前缀 博客平均得分。

    public static final String ITEM_TYPE="item:type:"; // redis 前缀 存储博客类别

    public static final String ITEM_SIMILARITY="item:similarity:"; // redis 前缀 存储博客相似度

    public static final String RECOMMEND_USER="recommend:user:"; // redis为用户推荐 的博客列表
}
