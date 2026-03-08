package org.oyyj.mycommon.common.mq;

import lombok.Data;

@Data
public class MqPrefix {
    /**
     * 缓存相关
     */
    public static final String CACHE_INVALIDATION_EXCHANGE="cache.invalidation.exchange";  // 交换机
    public static final String CACHE_INVALIDATION_QUEUE="cache.invalidation.queue:";  // 队列前缀
    public static final String CACHE_INVALIDATION_ROUTING_KEY="cache.invalidation.routing.key"; // 消息路由

    /**
     * 死信队列
     */
    public static final String DXL_INVALIDATION_EXCHANGE="dxl.invalidation.exchange";  // 交换机
    public static final String DXL_INVALIDATION_QUEUE="dxl.invalidation.queue";  // 队列前缀
    public static final String DXL_INVALIDATION_ROUTING_KEY="dxl.invalidation.routing.key"; // 消息路由

    /**
     * 新增扣除库存的消息队列
     */
    public static final String VIP_NUM_EXCHANGE="vip.num.exchange";  // 交换机
    public static final String VIP_NUM_QUEUE="vip.num.queue";  // 队列前缀
    public static final String VIP_NUM_ROUTING_KEY="vip.num.routing.key"; // 消息路由

    /**
     * 添加延时队列
     */
    public static final String DELAY_EXCHANGE="delay.exchange";  // 交换机
    public static final String DELAY_QUEUE="delay.queue";  // 队列前缀
    public static final String DELAY_ROUTING_KEY="delay.routing.key"; // 消息路由

    /**
     * 用户行为 以及对应记录的重试机制
     */
    public static final String USER_BEHAVIOR_EXCHANGE="user.behavior.exchange"; // 交换机
    public static final String USER_BEHAVIOR_QUEUE="user.behavior.queue"; // 队列前缀
    public static final String USER_BEHAVIOR_ROUTING_KEY="user.behavior.routing.key"; // 消息路由

    /**
     * ES数据库同步队列
     */
    public static final String ES_BLOG_EXCHANGE="es.blog.exchange"; // 交换机
    public static final String ES_BLOG_QUEUE="es.blog.queue"; // 队列
    public static final String ES_BLOG_ROUTING_KEY="es.blog.routing.key"; // 消息路由

    /**
     * Blog  延时 发布队列
     */
    public static final String BLOG_PUBLISH_EXCHANGE="blog.publish.exchange"; // 交换机
    public static final String BLOG_PUBLISH_QUEUE="blog.publish.queue"; // 队列
    public static final String BLOG_PUBLISH_ROUTING_KEY="blog.publish.routing.key"; // 消息路由


    /**
     * 实时交流队列信息  需要获取相关的配置信息
     */
    public static final String MESSAGE_PUBLISH_EXCHANGE="message.publish.exchange:";
    public static final String MESSAGE_PUBLISH_QUEUE="message.publish.queue:";
    public static final String MESSAGE_PUBLISH_ROUTING_KEY="message.publish.routing.key:";


    /**
     * 将实时交流的消磁存储到es数据库中
     */
    public static final String MESSAGE_ES_EXCHANGE="message.es.exchange";
    public static final String MESSAGE_ES_QUEUE="message.es.queue";
    public static final String MESSAGE_ES_ROUTING_KEY="message.es.routing.key";




}
