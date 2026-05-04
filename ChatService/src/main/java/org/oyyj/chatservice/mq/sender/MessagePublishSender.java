package org.oyyj.chatservice.mq.sender;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Strings;
import org.oyyj.chatservice.mapper.MqMessageLogMapper;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.MqMessageLog;
import org.oyyj.chatservice.pojo.es.MessageDocument;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;

import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 发布消息
 */
@Async("taskExecutor")
@Component
@Slf4j
public class MessagePublishSender {

    @Value("${msgChat.ID}")
    private String msgChatId;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private MqMessageLogMapper messageRecordMapper;

    @Autowired
    private MqMessageLogMapper messageLogMapper;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * 发送消息任务
     * @param chatMessage
     * @param instance 对方所在的实例
     */
    public void sendMessage(ChatMessage chatMessage,String instance) {
        
        if (Objects.isNull(chatMessage)) {
            log.info("传递的消息不可为空");
            return ;
        }

        String snowflakeId = chatMessage.getMsgId();
        if (Strings.isEmpty(snowflakeId)) {
            log.info("消息的唯一标识不可未空");
        }
        
        try {
            String esMqStr = mapper.writeValueAsString(chatMessage);
            // 存储到 抓门的消息转发日志表中
            MqMessageLog mqMessageLog = new MqMessageLog();
            mqMessageLog.setMessageId(chatMessage.getMsgId());
            mqMessageLog.setExchange(MqPrefix.MESSAGE_PUBLISH_EXCHANGE+instance);
            mqMessageLog.setRoutingKey(MqPrefix.MESSAGE_PUBLISH_ROUTING_KEY+instance);
            mqMessageLog.setQueue(MqPrefix.MESSAGE_PUBLISH_QUEUE+instance);
            mqMessageLog.setContent(chatMessage.getContent());
            mqMessageLog.setStatus(MqMessageLog.MqMessageLogStatus.WAITING.getCode());
            mqMessageLog.setCreatedAt(new Date());
            int insert = messageLogMapper.insert(mqMessageLog);
            if(insert==0){
                log.error("插入MQ消息记录失败，msgId:{}", snowflakeId);
                throw new RuntimeException("插入MQ消息记录失败");
            }
            // 发送MQ
            EnhanceCorrelationData enhanceCorrelationData = new EnhanceCorrelationData(snowflakeId, esMqStr, 0,false,"");
            RabbitMqMessage rabbitMqMessage = new RabbitMqMessage();
            rabbitMqMessage.setMessageId(snowflakeId);
            rabbitMqMessage.setRetryCount(0);
            rabbitMqMessage.setPayLoad(esMqStr);
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                try {
                    rabbitTemplate.convertAndSend(
                            MqPrefix.MESSAGE_PUBLISH_EXCHANGE+instance,
                            MqPrefix.MESSAGE_PUBLISH_ROUTING_KEY+instance,
                            rabbitMqMessage,
                            enhanceCorrelationData
                    );
                    log.info("发送es的博客消息，msgId:{},esMqStr:{}", snowflakeId, esMqStr);
                    // 修改MQ记录表中信息的状态
                    updateMqStatus(snowflakeId, MqMessageLog.MqMessageLogStatus.SEND.getCode());
                    return true;
                } catch (AmqpException e) {
                    // 消息发送失败
                    log.info("消息发送失败，msgId:{},esMqStr:{}", snowflakeId, esMqStr, e);
                    return false;
                }
            });
            if(Objects.isNull(call) || call ){
                updateMqStatus(snowflakeId, MqMessageLog.MqMessageLogStatus.SEND_FAIL.getCode());
            }
        } catch (ExecutionException e) {
            log.error("异步任务处理失败 msgId:{},ex:{}", snowflakeId,e.getMessage(),e);
            updateMqStatus(snowflakeId,MqMessageLog.MqMessageLogStatus.SEND_FAIL.getCode());
            throw new RuntimeException(e);
        } catch (RetryException e) {
            log.error("重试失败msgId:{},ex:{}", snowflakeId,e.getMessage(),e);
            updateMqStatus(snowflakeId,MqMessageLog.MqMessageLogStatus.SEND_FAIL.getCode());
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            log.error("MAP转换成 str错误 mqId:{}",snowflakeId);
            updateMqStatus(snowflakeId,MqMessageLog.MqMessageLogStatus.SEND_FAIL.getCode());
            throw new RuntimeException(e);
        } catch (Exception e){
            log.error("其他异常 mqId:{}",snowflakeId);
            updateMqStatus(snowflakeId, MqMessageLog.MqMessageLogStatus.SEND_FAIL.getCode());
            throw new RuntimeException(e);
        }
    }

    private void updateMqStatus(String snowflakeId,Integer execStatus){
        int update = messageLogMapper.update(Wrappers.<MqMessageLog>lambdaUpdate()
                .eq(MqMessageLog::getMessageId, snowflakeId)
                .set(MqMessageLog::getStatus, execStatus)
        );
        if(update==0){
            log.warn("消息记录信息修改该失败,msgID:{} , status:{} ",snowflakeId,execStatus);
        }
    }
}
