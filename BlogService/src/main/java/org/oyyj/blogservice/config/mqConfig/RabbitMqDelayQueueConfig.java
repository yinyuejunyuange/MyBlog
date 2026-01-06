package org.oyyj.blogservice.config.mqConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;


import java.util.LinkedHashMap;
import java.util.Map;

import static org.oyyj.mycommon.common.MqPrefix.*;

@Configuration
@Slf4j
public class RabbitMqDelayQueueConfig {
    /**
     * 延迟队列（绑定死信交换机）
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("x-dead-letter-exchange", DXL_INVALIDATION_EXCHANGE);
        args.put("x-dead-letter-routing-key", DXL_INVALIDATION_ROUTING_KEY);
        return new Queue(DELAY_QUEUE, true, false, false, args);
    }

    /**
     * 延迟队列Topic交换机
     */
    @Bean
    public TopicExchange delayTopicExchange() {
        return new TopicExchange(DELAY_EXCHANGE);
    }

    /**
     * 延迟队列-交换机绑定
     */
    @Bean
    public Binding delayQueueBinding(Queue delayQueue, TopicExchange delayTopicExchange) {
        return BindingBuilder.bind(delayQueue)
                .to(delayTopicExchange)
                .with(DELAY_ROUTING_KEY);
    }
}
