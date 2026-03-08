package org.oyyj.chatservice.mq.config;

import org.oyyj.mycommon.common.mq.MqPrefix;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageMqConfig {

    @Value("${msgChat.ID}")
    private String msgId;

    /**
     * 队列 拼接实例
     */
    @Bean
    public Queue messagePublishQueue(){
        return new Queue(MqPrefix.MESSAGE_PUBLISH_EXCHANGE+ msgId);
    }

    /**
     * 交换机
     */
    @Bean
    public TopicExchange messagePublishTopicExchange(){
        return new TopicExchange(MqPrefix.MESSAGE_PUBLISH_EXCHANGE+ msgId);
    }

    /**
     * 将队列和交换机绑定起来
     * @param messagePublishQueue
     * @param messagePublishTopicExchange
     * @return
     */
    @Bean
    public Binding messagePublishBinding(Queue messagePublishQueue, TopicExchange messagePublishTopicExchange){
        return BindingBuilder.bind(messagePublishQueue)
                .to(messagePublishTopicExchange)
                .with(MqPrefix.MESSAGE_PUBLISH_ROUTING_KEY+msgId);
    }
}
