package org.oyyj.userservice.config.mq.sender;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.oyyj.userservice.dto.UnFreezeDTO;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class RMUnfreezeSender {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Autowired
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * 发1天
     * @param unFreezeDTO
     */
    public void sendUnFreezeMessage(UnFreezeDTO unFreezeDTO , Integer type ) {

        String queue = getQueue(type);
        String exchange = getExchange(type);
        String routingKey = getRoutingKey(type);

        String snowflakeId = snowflakeUtil.getSnowflakeId();
        if(Strings.isEmpty(snowflakeId)){
            log.info("雪花算法执行失败");
            return ;
        }
        if (Objects.isNull(unFreezeDTO)) {
            log.info("传递的消息不可为空");
            return ;
        }
        try {
            String esMqStr = mapper.writeValueAsString(unFreezeDTO);
            MqMessageRecord mqMessageRecord = new MqMessageRecord();
            mqMessageRecord.setMsgId(snowflakeId);
            mqMessageRecord.setExecStatus(MqStatusEnum.NOT_SEND.getCode());
            mqMessageRecord.setMsgContent(esMqStr);
            mqMessageRecord.setTargetQueue(queue);
            int insert = mqMessageRecordMapper.insert(mqMessageRecord);
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
                            exchange,
                            routingKey,
                            rabbitMqMessage,
                            enhanceCorrelationData
                    );
                    log.info("发送es的博客消息，msgId:{},esMqStr:{}", snowflakeId, esMqStr);
                    // 修改MQ记录表中信息的状态
                    updateMqStatus(snowflakeId, MqStatusEnum.PENDING.getCode());
                    return true;
                } catch (AmqpException e) {
                    // 消息发送失败
                    log.info("消息发送失败，msgId:{},esMqStr:{}", snowflakeId, esMqStr, e);
                    return false;
                }
            });
            if(Objects.isNull(call) || !call ){
                updateMqStatus(snowflakeId, MqStatusEnum.FAIL_SEND.getCode());
            }
        } catch (ExecutionException e) {
            log.error("异步任务处理失败 msgId:{},ex:{}", snowflakeId,e.getMessage(),e);
            updateMqStatus(snowflakeId,MqStatusEnum.FAIL_SEND.getCode());
            throw new RuntimeException(e);
        } catch (RetryException e) {
            log.error("重试失败msgId:{},ex:{}", snowflakeId,e.getMessage(),e);
            updateMqStatus(snowflakeId,MqStatusEnum.FAIL_SEND.getCode());
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            log.error("MAP转换成 str错误 mqId:{}",snowflakeId);
            updateMqStatus(snowflakeId,MqStatusEnum.FAIL_SEND.getCode());
            throw new RuntimeException(e);
        } catch (Exception e){
            log.error("其他异常 mqId:{}",snowflakeId);
            updateMqStatus(snowflakeId,MqStatusEnum.FAIL_SEND.getCode());
            throw new RuntimeException(e);
        }
    }

    private String getQueue(Integer type) {
        return switch (type) {
            case 1 -> MqPrefix.USER_UNFREEZE_1DAY_QUEUE;
            case 2 -> MqPrefix.USER_UNFREEZE_1WEEK_QUEUE;
            case 3 -> MqPrefix.USER_UNFREEZE_1MONTH_QUEUE;
            default -> MqPrefix.USER_UNFREEZE_1DAY_QUEUE;
        };
    }

    private String getExchange(Integer type) {
        return switch (type) {
            case 1 -> MqPrefix.USER_UNFREEZE_1DAY_EXCHANGE;
            case 2 -> MqPrefix.USER_UNFREEZE_1WEEK_EXCHANGE;
            case 3 -> MqPrefix.USER_UNFREEZE_1MONTH_EXCHANGE;
            default -> MqPrefix.USER_UNFREEZE_1DAY_EXCHANGE;
        };
    }

    private String getRoutingKey(Integer type) {
        return switch (type) {
            case 1 -> MqPrefix.USER_UNFREEZE_1DAY_ROUTING_KEY;
            case 2 -> MqPrefix.USER_UNFREEZE_1WEEK_ROUTING_KEY;
            case 3 -> MqPrefix.USER_UNFREEZE_1MONTH_ROUTING_KEY;
            default -> MqPrefix.USER_UNFREEZE_1DAY_ROUTING_KEY;
        };
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
