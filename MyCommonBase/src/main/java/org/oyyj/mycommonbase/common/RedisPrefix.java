package org.oyyj.mycommonbase.common;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class RedisPrefix {

    /**
     * 初始化以及评分矩阵相关
     */
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

    /**
     * 博客相关
     */
    public static final String BLOG_READ_COUNT= "blog:read:count:"; // 博客阅读次数

    public static final String BLOG_INGO = "blog:info:"; // 博客文章详情

    public static final String BLOG_VIEW_COUNT = "blog:view:count:"; // 博客短时间内的阅读次数

    public static final String LOCK_BLOG_INGO="lock:blog:info:"; // 博客文章详情 分布式锁

    public static final String BLOG_USER_READ="blog:user:read:"; // 一个小时内是否阅读了

    public static final String BLOG_READ_UPDATE="blog:info:update"; // 博客是否更新 需要设置存货时间

    public static final String BLOG_READ_LOCK="blog:read:lock"; // 更新热门博客信息的锁

    public static final String BLOG_START_LOCK="blog:start:lock:"; // 博客收藏数增加的锁

    public static final String BLOG_KUDOS_LOCK="blog:kudos:lock:"; // 博客点赞数增加的锁

    public static final String BLOG_COMMENT_LOCK =  "blog:comment:lock:"; // 博客评论数量

    public static final String BLOG_COMMENT_KUDOS_LOCK = "blog:comment:kudos:lock:"; // 博客评论点赞数修改

    public static final String BLOG_REPLY_KUDOS_LOCK = "blog:reply:kudos:lock:"; // 博客恢复点赞数修改

    /**
     * 未博客延时发布 Zset 队列
     */
    public static final String BLOG_PUBLISH_ZSET = "blog:publish:zset:";

    /**
     * Ai对话面试
     */
    public static final String AI_INTERVIEW_PREFIX = "interview:session:";
}
