package org.oyyj.blogservice.config.mqConfig.sender;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.checkerframework.checker.units.qual.A;
import org.oyyj.blogservice.config.mqConfig.RabbitMqUserBehaviorConfig;
import org.oyyj.blogservice.mapper.UserBehaviorMapper;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommon.common.MqPrefix;
import org.oyyj.mycommon.common.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class RabbitMqUserBehaviorSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 发送用户行为消息
     * @param behaviorType
     * @param blogId
     * @param userId
     */
    public void sendUserBehaviorMessage(Integer behaviorType, Long blogId, Long userId) {
        // 兜底
        // 雪花算法生成唯一ID
        String snowflakeId = snowflakeUtil.getSnowflakeId();
        try {

            // 生成信息载荷
            Map<String , Long> readBlogMap = new HashMap<>();
            readBlogMap.put("blogId",blogId);
            readBlogMap.put("userId",userId);
            readBlogMap.put("typeId", Long.valueOf(behaviorType)); // 行为属于啥
            String mapStr = null;
            mapStr = objectMapper.writeValueAsString(readBlogMap);
            // 先存储到数据库中
            MqMessageRecord mqMessageRecord = new MqMessageRecord();
            mqMessageRecord.setMsgId(snowflakeId);
            mqMessageRecord.setExecStatus(MqStatusEnum.FAIL_SEND.getCode());
            mqMessageRecord.setMsgContent(mapStr);
            mqMessageRecord.setTargetQueue(MqPrefix.USER_BEHAVIOR_QUEUE);
            mqMessageRecordMapper.insert(mqMessageRecord);
            // 发送MQ
            EnhanceCorrelationData enhanceCorrelationData = new EnhanceCorrelationData(snowflakeId, mapStr, 0);
            RabbitMqMessage  rabbitMqMessage = new RabbitMqMessage();
            rabbitMqMessage.setMessageId(snowflakeId);
            rabbitMqMessage.setRetryCount(0);
            rabbitMqMessage.setPayLoad(mapStr);

            rabbitTemplate.convertAndSend(
                    MqPrefix.USER_BEHAVIOR_EXCHANGE,
                    MqPrefix.USER_BEHAVIOR_ROUTING_KEY,
                    rabbitMqMessage,
                    enhanceCorrelationData
            );
            log.info("发送用户行为MQ消息成功，msgId:{}, blogID:{},userId:{},behaviorType:{}",snowflakeId,mapStr,userId,behaviorType);
            // 修改MQ记录表中信息的状态
            updateMqStatus(snowflakeId,MqStatusEnum.PENDING.getCode());

        } catch (JsonProcessingException e){
            // 转换兜底
            log.error("MAP转换成 str错误 blogID:{},userId:{},behaviorType:{}",blogId,userId,behaviorType);

            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            // 最终兜底
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
