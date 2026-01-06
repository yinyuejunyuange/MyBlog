package org.oyyj.blogservice.config.mqConfig;

import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.config.RabbitMqConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.oyyj.mycommon.common.MqPrefix.*;

@Configuration
@Slf4j
public class RabbitMqCacheInvalidationConfig {


    @Autowired
    private RabbitMqConfig baseConfig;

    // 生成带实例标识的队列名
    public String getCacheInvalidationQueue() {
        return CACHE_INVALIDATION_QUEUE + baseConfig.getApplicationName() + ":" + baseConfig.getClusterName();
    }

    /**
     * 缓存失效Topic交换机
     */
    @Bean
    public TopicExchange cacheInvalidationTopicExchange() {
        return new TopicExchange(CACHE_INVALIDATION_EXCHANGE);
    }

    /**
     * 缓存失效队列（每个实例独立队列）
     */
    @Bean
    public Queue cacheInvalidationQueue() {
        return new Queue(getCacheInvalidationQueue());
    }

    /**
     * 缓存失效队列-交换机绑定
     */
    @Bean
    public Binding cacheInvalidationBinding(Queue cacheInvalidationQueue, TopicExchange cacheInvalidationTopicExchange) {
        return BindingBuilder.bind(cacheInvalidationQueue)
                .to(cacheInvalidationTopicExchange)
                .with(CACHE_INVALIDATION_ROUTING_KEY);
    }

}
