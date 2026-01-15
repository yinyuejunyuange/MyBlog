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
public class RabbitMqEsConfig {

    /**
     * 队列
     */
    @Bean
    public Queue esQueue(){
        return new Queue(MqPrefix.ES_BLOG_QUEUE);
    }

    /**
     * 交换机
     */
    @Bean
    public TopicExchange esBlogTopicExchange(){
        return new TopicExchange(MqPrefix.ES_BLOG_EXCHANGE);
    }

    /**
     * 将队列和交换机绑定起来
     * @param esQueue
     * @param esBlogTopicExchange
     * @return
     */
    @Bean
    public Binding esBlogBinding(Queue esQueue, TopicExchange esBlogTopicExchange){
        return BindingBuilder.bind(esQueue)
                .to(esBlogTopicExchange)
                .with(MqPrefix.ES_BLOG_ROUTING_KEY);
    }

}
