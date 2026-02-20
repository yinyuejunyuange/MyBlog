package org.oyyj.blogservice.config.mqConfig;

import org.oyyj.mycommon.common.mq.MqPrefix;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqPublishConfig {

    @Bean
    public Queue publishBlogQueue(){
        return new Queue(MqPrefix.BLOG_PUBLISH_QUEUE);
    }

    @Bean
    public TopicExchange publishBlogExchange(){
        return new TopicExchange(MqPrefix.BLOG_PUBLISH_EXCHANGE);
    }

    @Bean
    public Binding publishBlogBinding(Queue publishBlogQueue, TopicExchange publishBlogExchange){
        return BindingBuilder.bind(publishBlogQueue)
                .to(publishBlogExchange)
                .with(MqPrefix.BLOG_PUBLISH_ROUTING_KEY);
    }

}
