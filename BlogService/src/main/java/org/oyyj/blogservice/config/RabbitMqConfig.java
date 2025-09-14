package org.oyyj.blogservice.config;


import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.ToStringPlugin;
import org.oyyj.blogservice.config.pojo.EnhancedCorrelationData;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMqConfig {
    @Value("${spring.application.name:BlogService}") // 从配置中获取数据
    private  String applicationName;

    @Value("${spring.cloud.nacos.discovery.cluster-name}")
    private  String cluster_name;
    public static final String CACHE_INVALIDATION_EXCHANGE="cache.invalidation.exchange";  // 交换机
    public static final String CACHE_INVALIDATION_QUEUE="cache.invalidation.queue:";  // 队列前缀
    public static final String CACHE_INVALIDATION_ROUTING_KEY="cache.invalidation.routing.key"; // 消息路由


    @Autowired
    private ConnectionFactory connectionFactory;

    // 提供获取完整队列名的方法，供SpEL调用
    public String getCacheInvalidationQueue() {
        return CACHE_INVALIDATION_QUEUE + applicationName+":"+cluster_name;
    }

    // 定义topic交换机
    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(CACHE_INVALIDATION_EXCHANGE);
    }

    // 定义缓存队列
    @Bean
    public Queue cacheInvalidationQueue(){
        // 每个实例都有队列 但是使用相同的 路由键 确保所有实例都可以收到消息而不是只发送给一个
        return new Queue(getCacheInvalidationQueue() );
    }

    // 绑定消息队列
    @Bean
    public Binding cacheInvalidationBinding(Queue cacheInvalidationQueue, TopicExchange topicExchange){
        return BindingBuilder.bind(cacheInvalidationQueue)
                .to(topicExchange)
                .with(CACHE_INVALIDATION_ROUTING_KEY);
    }

    // 消费者确认机制 通过redis
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter()); // 设置消息转换器
        // 设置confirm callback
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String body = "1";
            if(correlationData instanceof EnhancedCorrelationData){
                body = ((EnhancedCorrelationData) correlationData).getBody();
            }
            if (ack) {
                //消息投递到exchange
                log.debug("消息发送到exchange成功:correlationData={},message_id={} ", correlationData, body);
                System.out.println("消息发送到exchange成功:correlationData={},message_id={}"+correlationData+body);
            } else {
                log.debug("消息发送到exchange失败:cause={},message_id={}",cause, body);
                System.out.println("消息发送到exchange失败:cause={},message_id={}"+cause+body);
            }
        });
        // 设置 return callback
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            Message message = returnedMessage.getMessage();
            int replyCode = returnedMessage.getReplyCode();
            String replyText = returnedMessage.getReplyText();
            String exchange = returnedMessage.getExchange();
            String routingKey = returnedMessage.getRoutingKey();
            log.error("消息发送失败，应答码{}，原因{}，交换机{}，路由键{}，消息{}",replyCode,replyText,exchange,routingKey,message);
        });

        return rabbitTemplate;
    }

    // 死信队列

    public static final String DXL_INVALIDATION_EXCHANGE="dxl.invalidation.exchange";  // 交换机
    public static final String DXL_INVALIDATION_QUEUE="dxl.invalidation.queue";  // 队列前缀
    public static final String DXL_INVALIDATION_ROUTING_KEY="dxl.invalidation.routing.key"; // 消息路由

    // 提供获取完整队列名的方法，供SpEL调用
    public String getDXLInvalidationQueue() {
        return DXL_INVALIDATION_QUEUE + applicationName+":"+cluster_name;
    }

    // 定义topic交换机
    @Bean
    public TopicExchange topicDXLExchange(){
        return new TopicExchange(DXL_INVALIDATION_EXCHANGE);
    }

    // 定义缓存队列
    @Bean
    public Queue DXLInvalidationQueue(){
        // 每个实例都有队列 但是使用相同的 路由键 确保所有实例都可以收到消息而不是只发送给一个
        return new Queue(getDXLInvalidationQueue() );
    }

    // 绑定消息队列
    @Bean
    public Binding DXLInvalidationBinding(Queue DXLInvalidationQueue, TopicExchange topicDXLExchange){
        return BindingBuilder.bind(DXLInvalidationQueue)
                .to(topicDXLExchange)
                .with(DXL_INVALIDATION_ROUTING_KEY);
    }

    /**
     * 配置JSON消息转换器（替代默认的JDK序列化）
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }




    /**
     * 新增扣除库存的消息队列
     */
    public static final String VIP_NUM_EXCHANGE="vip.num.exchange";  // 交换机
    public static final String VIP_NUM_QUEUE="vip.num.queue";  // 队列前缀
    public static final String VIP_NUM_ROUTING_KEY="vip.num.routing.key"; // 消息路由

    @Bean
    public Queue vipNumQueue(){
        return new Queue(VIP_NUM_QUEUE);
    }

    @Bean
    public TopicExchange vipNumExchange(){
        return new TopicExchange(VIP_NUM_EXCHANGE);
    }

    @Bean
    public Binding bindingVipNumBinding(Queue vipNumQueue, TopicExchange vipNumExchange){
        return BindingBuilder.bind(vipNumQueue)
                .to(vipNumExchange)
                .with(VIP_NUM_ROUTING_KEY);
    }


    /**
     * 配置监听器容器工厂（接收消息用），使用JSON转换器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter()); // 设置消息转换器
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 再次设置手动
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setDefaultRequeueRejected(false); // 不自动重新入队

        return factory;
    }
}

