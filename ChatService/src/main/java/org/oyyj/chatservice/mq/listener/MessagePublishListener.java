package org.oyyj.chatservice.mq.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.chatservice.component.WebSocketHandler;
import org.oyyj.chatservice.mapper.ChatMessageMapper;
import org.oyyj.chatservice.mapper.MqMessageLogMapper;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.MqMessageLog;
import org.oyyj.mycommon.common.EsBlogWork;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class MessagePublishListener {

    @Autowired
    private MqMessageLogMapper  mqMessageLogMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private WebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 使用spEL表达式
    @RabbitListener(queues = "#{messagePublishQueue.name}")
    public void handleEsBlogMessage(RabbitMqMessage rabbitMqMessage,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                try {
                    log.info("接收到消息推送,payload:{},deliveryTag:{}", rabbitMqMessage, deliveryTag);
                    MqMessageLog mqMessageLog = mqMessageLogMapper.selectOne(Wrappers.<MqMessageLog>lambdaQuery()
                            .eq(MqMessageLog::getMessageId, rabbitMqMessage.getMessageId())
                    );
                    if (mqMessageLog == null) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.info("接收到的推送消息不存在：{}", rabbitMqMessage.getMessageId());
                        return true;
                    }

                    if (MqMessageLog.MqMessageLogStatus.FINISH.getCode().equals(mqMessageLog.getStatus())) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.warn("msgId:{} 数据已处理", rabbitMqMessage.getMessageId());
                        return true;
                    }

                    String payLoad = rabbitMqMessage.getPayLoad();
                    ChatMessage chatMessage = objectMapper.readValue(payLoad, ChatMessage.class);
                    if (chatMessage == null) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.warn("msgId:{} 推送消息不存在", rabbitMqMessage.getMessageId());
                        updateMqStatus(rabbitMqMessage.getMessageId(), MqMessageLog.MqMessageLogStatus.FAIL.getCode(), "推送消息不存在");
                        return true;
                    }

                    updateMqStatus(chatMessage.getMsgId(), MqMessageLog.MqMessageLogStatus.READY.getCode(), "");
                    webSocketHandler.sendToUser(chatMessage.getToUserId(), chatMessage);
                    updateMqStatus(chatMessage.getMsgId(), MqMessageLog.MqMessageLogStatus.FINISH.getCode(), "");
                    if (channel.isOpen()) {
                        channel.basicAck(deliveryTag, false);
                    }
                    return true;
                } catch (IOException e) {
                    log.error("数据处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
                    return false;
                }
            });
            if (call == null || !call) {
                log.warn("msgId:{} 重试消息处理失败", rabbitMqMessage.getMessageId());
                updateMqStatus(rabbitMqMessage.getMessageId(), MqMessageLog.MqMessageLogStatus.FAIL.getCode(), "重试消息处理后失败");
                if (channel.isOpen()) {
                    channel.basicAck(deliveryTag, false);
                }
            }
        } catch (ExecutionException | RetryException e) {
            log.error("重试消息处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
            updateMqStatus(rabbitMqMessage.getMessageId(), MqMessageLog.MqMessageLogStatus.FAIL.getCode(), "重试消息处理失败");
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            }
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateMqStatus(String snowflakeId,Integer execStatus,String errorMsg){
        int update = mqMessageLogMapper.update(Wrappers.<MqMessageLog>lambdaUpdate()
                .eq(MqMessageLog::getMessageId, snowflakeId)
                .set(MqMessageLog::getStatus, execStatus)
        );
        if(update==0){
            log.warn("消息记录信息修改该失败,msgID:{} , status:{} ",snowflakeId,execStatus);
        }
    }


}
