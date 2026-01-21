package org.oyyj.mycommon.mq.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.common.mq.MqCommon;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.mq.pojo.DelayMessage;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 延时队列发送消息
 */
@Component
@Slf4j
public class RabbitMqDelaySender {


    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private MqCommon mqCommon;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送消息到延时队列
     * @param delayTime
     */
    public void sendDelayMessage( DelayMessage delayMessage, int delayTime){
        String snowflakeId = snowflakeUtil.getSnowflakeId();
        delayMessage.setMessageId(snowflakeId);
        // 生成信息载荷
        String mapStr = null;
        try {
            mapStr = objectMapper.writeValueAsString(delayMessage);

            mqCommon.sendMessageToMq(snowflakeId, mapStr);
            log.info("发送延迟队列成功，msgId:{}, blogID:{}",snowflakeId,mapStr);

        } catch (JsonProcessingException e) {
            log.error("MAP转换成 str错误 message：{} 错误：{}",delayMessage,e.getMessage(),e);
            throw new RuntimeException(e);
        }catch (RuntimeException e) {
            // 最终兜底
            log.error("消息发送失败，原因如下：{}",e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

}
