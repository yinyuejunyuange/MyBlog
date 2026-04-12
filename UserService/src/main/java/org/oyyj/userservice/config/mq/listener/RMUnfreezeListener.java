package org.oyyj.userservice.config.mq.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.checkerframework.checker.units.qual.A;
import org.oyyj.mycommon.common.EsBlogWork;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.oyyj.userservice.dto.UnFreezeDTO;
import org.oyyj.userservice.mapper.UserMapper;
import org.oyyj.userservice.pojo.User;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class RMUnfreezeListener {
    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = MqPrefix.USER_UNFREEZE_DLX_QUEUE)
    public void handleEsBlogMessage(RabbitMqMessage rabbitMqMessage,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) int deliveryTag) throws IOException {
        try {
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                try {
                    log.info("接收到用户行为信息修改信息,payload:{},deliveryTag:{}", rabbitMqMessage, deliveryTag);
                    // 判断数据是否已经处理完毕
                    MqMessageRecord mqMessageRecord = mqMessageRecordMapper.selectOne(Wrappers.<MqMessageRecord>lambdaQuery()
                            .eq(MqMessageRecord::getMsgId, rabbitMqMessage.getMessageId())
                    );
                    if (mqMessageRecord == null) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.warn("msgId:{} 数据不存在", rabbitMqMessage.getMessageId());
                        return true;
                    }

                    if (MqStatusEnum.SUCCESS.getCode().equals(mqMessageRecord.getExecStatus()) ||
                            MqStatusEnum.FAIL.getCode().equals(mqMessageRecord.getExecStatus())) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.warn("msgId:{} 数据已处理", rabbitMqMessage.getMessageId());
                        return true;
                    }
                    // 处理请求的逻辑
                    String mapStr = rabbitMqMessage.getPayLoad();
                    UnFreezeDTO unFreezeDTO = mapper.readValue(mapStr, UnFreezeDTO.class);
                    if (unFreezeDTO == null) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.warn("msgId:{} ES处理消息不存在", rabbitMqMessage.getMessageId());
                        return true;
                    }
                    int update = userMapper.update(Wrappers.<User>lambdaUpdate()
                            .eq(User::getId, unFreezeDTO.getUserId())
                            .set(User::getIsFreeze, 0)
                    );
                    if(update<=0){
                        log.warn("msgId:{} 解冻任务失败 原因：数据库操作错误", rabbitMqMessage.getMessageId());
                    }
                    // 处理成功，确认消息 手动确认消息
                    if (channel.isOpen()) {
                        channel.basicAck(deliveryTag, false);
                    }

                    return true;
                } catch (IOException e) {
                    log.error("数据处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
                    return false;
                }
            });
            if(call == null || !call){
                log.warn("msgId:{} 重试消息处理失败", rabbitMqMessage.getMessageId());
            }else{
                mqMessageRecordMapper.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                        .eq(MqMessageRecord::getMsgId, rabbitMqMessage.getMessageId())
                        .set(MqMessageRecord::getExecStatus, MqStatusEnum.SUCCESS.getCode())
                );
            }
        } catch (ExecutionException | RetryException e) {
            log.error("重试消息处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
            mqMessageRecordMapper.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                    .eq(MqMessageRecord::getMsgId, rabbitMqMessage.getMessageId())
                    .set(MqMessageRecord::getExecStatus, MqStatusEnum.FAIL.getCode())
            );
            throw new RuntimeException(e);
        }finally {
            // 清除队列预防阻塞
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            }

        }
    }

}
