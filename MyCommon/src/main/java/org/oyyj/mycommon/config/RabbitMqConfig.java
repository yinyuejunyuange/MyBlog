package org.oyyj.mycommon.config;

import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMqConfig {

    // 共享属性（其他配置类可通过注入此类获取）
    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.cloud.nacos.discovery.cluster-name}")
    private String clusterName;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 配置JSON消息转换器（替代默认的JDK序列化）
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    // 消费者确认机制 通过redis
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter()); // 设置消息转换器
        // 设置confirm callback
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String body = "1";
            EnhanceCorrelationData correlationDataEn = null;
            if(correlationData instanceof EnhanceCorrelationData){
                correlationDataEn= (EnhanceCorrelationData) correlationData;
                body = correlationDataEn.getBody();

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

    // 提供getter供其他配置类获取共享属性
    public String getApplicationName() {
        return applicationName;
    }

    public String getClusterName() {
        return clusterName;
    }
}
