package org.oyyj.mycommon.common.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class MqCommon {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public void sendMessageToMq(String snowflakeId, String mapStr){
        try {
            MqMessageRecord mqMessageRecord = new MqMessageRecord();
            mqMessageRecord.setMsgId(snowflakeId);
            mqMessageRecord.setExecStatus(MqStatusEnum.NOT_SEND.getCode());
            mqMessageRecord.setMsgContent(mapStr);
            mqMessageRecord.setTargetQueue(MqPrefix.USER_BEHAVIOR_QUEUE);
            mqMessageRecordMapper.insert(mqMessageRecord);
            // 发送MQ
            EnhanceCorrelationData enhanceCorrelationData = new EnhanceCorrelationData(snowflakeId, mapStr, 0,false,"");
            RabbitMqMessage rabbitMqMessage = new RabbitMqMessage();
            rabbitMqMessage.setMessageId(snowflakeId);
            rabbitMqMessage.setRetryCount(0);
            rabbitMqMessage.setPayLoad(mapStr);

            rabbitTemplate.convertAndSend(
                    MqPrefix.USER_BEHAVIOR_EXCHANGE,
                    MqPrefix.USER_BEHAVIOR_ROUTING_KEY,
                    rabbitMqMessage,
                    enhanceCorrelationData
            );
            // 修改MQ记录表中信息的状态
            updateMqStatus(snowflakeId,MqStatusEnum.PENDING.getCode());
        } catch (AmqpException e) {
            log.error("消息发送失败，原因如下：{}",e.getMessage(),e);
            updateMqStatus(snowflakeId,MqStatusEnum.FAIL_SEND.getCode());
            throw new RuntimeException(e);
        }
    }

    private void updateMqStatus(String snowflakeId,Integer execStatus){
        int update = mqMessageRecordMapper.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                .eq(MqMessageRecord::getMsgId, snowflakeId)
                .eq(MqMessageRecord::getExecStatus, MqStatusEnum.NOT_SEND.getCode())
                .set(MqMessageRecord::getExecStatus, execStatus)
        );
        if(update==0){
            log.warn("消息记录信息修改该失败,msgID:{} , status:{} ",snowflakeId,execStatus);
        }
    }

}
