package org.oyyj.userservice.config.mq;

import com.alibaba.druid.sql.visitor.functions.Bin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.oyyj.mycommon.common.mq.MqPrefix.*;

/**
 * 用户解冻消息队列配置
 */
@Component
@Slf4j
public class RMUnfreezeConfig {

    /**
     * 1天
     */
    @Bean
    public TopicExchange userUnfreeze1DayExchange(){
        return new TopicExchange(USER_UNFREEZE_1DAY_EXCHANGE);
    }

    @Bean
    public Queue userUnfreeze1DayQueue(){
        Map<String, Object> args = new HashMap<>();
        // args.put("x-message-ttl", 24 * 60 * 60 * 1000);          // 1天 = 86400000 ms
        args.put("x-message-ttl", 5 * 60 * 1000);     // 测试使用 5分钟
        args.put("x-dead-letter-exchange", USER_UNFREEZE_DLX_EXCHANGE);    // 死信交换器
        args.put("x-dead-letter-routing-key", USER_UNFREEZE_DLX_ROUTING_KEY); // 死信路由键
        return new Queue(USER_UNFREEZE_1DAY_QUEUE,true,false,false,args);
        /**
         * 持久化
         * 非独占
         * 非自动删除
         * 死刑配置
         */
    }

    @Bean
    public Binding userUnfreeze1DayBinding(Queue userUnfreeze1DayQueue, TopicExchange userUnfreeze1DayExchange){
        return BindingBuilder.bind(userUnfreeze1DayQueue)
                .to(userUnfreeze1DayExchange)
                .with(USER_UNFREEZE_1DAY_ROUTING_KEY);
    }

    /**
     * 1周
     */
    @Bean
    public TopicExchange userUnfreeze1WeekExchange(){
        return new TopicExchange(USER_UNFREEZE_1WEEK_EXCHANGE);
    }

    @Bean
    public Queue userUnfreeze1WeekQueue(){
        Map<String, Object> args = new HashMap<>();
        // args.put("x-message-ttl", 24 * 60 * 60 * 1000);          // 1天 = 86400000 ms
        args.put("x-message-ttl", 10 * 60 * 1000);     // 测试使用 10分钟
        args.put("x-dead-letter-exchange", USER_UNFREEZE_DLX_EXCHANGE);    // 死信交换器
        args.put("x-dead-letter-routing-key", USER_UNFREEZE_DLX_ROUTING_KEY); // 死信路由键
        return new Queue(USER_UNFREEZE_1WEEK_QUEUE,true,false,false,args);
    }

    @Bean
    public Binding userUnfreeze1WeekBinding(Queue userUnfreeze1WeekQueue, TopicExchange userUnfreeze1WeekExchange){
        return BindingBuilder.bind(userUnfreeze1WeekQueue)
                .to(userUnfreeze1WeekExchange)
                .with(USER_UNFREEZE_1WEEK_ROUTING_KEY);
    }

    /**
     * 1月
     */
    @Bean
    public TopicExchange userUnfreeze1MonthExchange(){
        return new TopicExchange(USER_UNFREEZE_1MONTH_EXCHANGE);
    }

    @Bean
    public Queue userUnfreeze1MonthQueue(){
        Map<String, Object> args = new HashMap<>();
        // args.put("x-message-ttl", 24 * 60 * 60 * 1000);          // 1天 = 86400000 ms
        args.put("x-message-ttl", 30 * 60 * 1000);     // 测试使用 30分钟
        args.put("x-dead-letter-exchange", USER_UNFREEZE_DLX_EXCHANGE);    // 死信交换器
        args.put("x-dead-letter-routing-key", USER_UNFREEZE_DLX_ROUTING_KEY); // 死信路由键
        return new Queue(USER_UNFREEZE_1MONTH_QUEUE,true,false,false,args);
    }

    @Bean
    public Binding userUnfreeze1MonthBinding(Queue userUnfreeze1MonthQueue, TopicExchange userUnfreeze1MonthExchange){
        return BindingBuilder.bind(userUnfreeze1MonthQueue)
                .to(userUnfreeze1MonthExchange)
                .with(USER_UNFREEZE_1MONTH_ROUTING_KEY);
    }

    /**
     * 死信队列配置
     */

    @Bean
    public TopicExchange userUnfreezeDlxExchange(){
        return new TopicExchange(USER_UNFREEZE_DLX_EXCHANGE);
    }

    @Bean
    public Queue userUnfreezeDlxQueue(){
        return new Queue(USER_UNFREEZE_DLX_QUEUE);
    }

    @Bean
    public Binding userUnfreezeDlxBinding(Queue userUnfreezeDlxQueue, TopicExchange userUnfreezeDlxExchange){
        return BindingBuilder.bind(userUnfreezeDlxQueue)
                .to(userUnfreezeDlxExchange)
                .with(USER_UNFREEZE_DLX_ROUTING_KEY);
    }


}
