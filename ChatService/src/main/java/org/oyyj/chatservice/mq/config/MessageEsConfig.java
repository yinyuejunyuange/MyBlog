package org.oyyj.chatservice.mq.config;

import org.oyyj.mycommon.common.mq.MqPrefix;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将对话数据存储到ES数据库中
 */
@Configuration
public class MessageEsConfig {

    /**
     * 队列 拼接实例
     */
    @Bean
    public Queue messageESQueue(){
        return new Queue(MqPrefix.MESSAGE_ES_EXCHANGE);
    }

    /**
     * 交换机
     */
    @Bean
    public TopicExchange messageESExchange(){
        return new TopicExchange(MqPrefix.MESSAGE_ES_EXCHANGE);
    }

    /**
     * 将队列和交换机绑定起来
     * @param messageESQueue
     * @param messageESExchange
     * @return
     */
    @Bean
    public Binding messageESBinding(Queue messageESQueue, TopicExchange messageESExchange){
        return BindingBuilder.bind(messageESQueue)
                .to(messageESExchange)
                .with(MqPrefix.MESSAGE_ES_ROUTING_KEY);
    }

}
