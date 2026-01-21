package org.oyyj.mycommon.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.oyyj.mycommon.config.RabbitMqConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import static org.oyyj.mycommon.common.mq.MqPrefix.*;

@Configuration
@Slf4j
public class RabbitMqDxlInvalidationConfig {

    @Autowired
    private RabbitMqConfig baseConfig;

    // 生成带实例标识的队列名
    public String getDXLInvalidationQueue() {
        return DXL_INVALIDATION_QUEUE + baseConfig.getApplicationName() + ":" + baseConfig.getClusterName();
    }

    /**
     * DXL失效Topic交换机
     */
    @Bean
    public TopicExchange dxlInvalidationTopicExchange() {
        return new TopicExchange(DXL_INVALIDATION_EXCHANGE);
    }

    /**
     * DXL失效队列（每个实例独立队列）
     */
    @Bean
    public Queue dxlInvalidationQueue() {
        return new Queue(getDXLInvalidationQueue());
    }

    /**
     * DXL失效队列-交换机绑定
     */
    @Bean
    public Binding dxlInvalidationBinding(Queue dxlInvalidationQueue, TopicExchange dxlInvalidationTopicExchange) {
        return BindingBuilder.bind(dxlInvalidationQueue)
                .to(dxlInvalidationTopicExchange)
                .with(DXL_INVALIDATION_ROUTING_KEY);
    }

}
