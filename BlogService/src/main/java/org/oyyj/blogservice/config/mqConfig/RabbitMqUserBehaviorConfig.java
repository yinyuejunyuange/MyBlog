package org.oyyj.blogservice.config.mqConfig;

import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.common.MqPrefix;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMqUserBehaviorConfig {

    @Bean
    public Queue userBehaviorQueue() {
        return new Queue(MqPrefix.USER_BEHAVIOR_QUEUE);
    }

    @Bean
    public TopicExchange userBehaviorTopicExchange() {
        return new TopicExchange(MqPrefix.USER_BEHAVIOR_EXCHANGE);
    }

    @Bean
    public Binding userBehaviorBinding(Queue userBehaviorQueue, TopicExchange userBehaviorTopicExchange) {
        return BindingBuilder.bind(userBehaviorQueue)
                .to(userBehaviorTopicExchange)
                .with(MqPrefix.USER_BEHAVIOR_ROUTING_KEY);
    }

}
