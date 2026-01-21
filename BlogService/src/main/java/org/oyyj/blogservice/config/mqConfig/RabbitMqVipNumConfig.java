package org.oyyj.blogservice.config.mqConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

import static org.oyyj.mycommon.common.mq.MqPrefix.*;

@Configuration
@Slf4j
public class RabbitMqVipNumConfig {
    /**
     * VIP数量队列
     */
    @Bean
    public Queue vipNumQueue() {
        return new Queue(VIP_NUM_QUEUE);
    }

    /**
     * VIP数量Topic交换机
     */
    @Bean
    public TopicExchange vipNumTopicExchange() {
        return new TopicExchange(VIP_NUM_EXCHANGE);
    }

    /**
     * VIP数量队列-交换机绑定
     */
    @Bean
    public Binding vipNumBinding(Queue vipNumQueue, TopicExchange vipNumTopicExchange) {
        return BindingBuilder.bind(vipNumQueue)
                .to(vipNumTopicExchange)
                .with(VIP_NUM_ROUTING_KEY);
    }
}
