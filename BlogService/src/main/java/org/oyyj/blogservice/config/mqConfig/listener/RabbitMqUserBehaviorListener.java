package org.oyyj.blogservice.config.mqConfig.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.service.IUserBehaviorService;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class RabbitMqUserBehaviorListener {

    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Autowired
    private IUserBehaviorService  userBehaviorService;

    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = MqPrefix.USER_BEHAVIOR_QUEUE)
    public void handleUserBehaviorMessage(RabbitMqMessage rabbitMqMessage,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) int deliveryTag){
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
                    Map<String, Long> payload = mapper.readValue(mapStr, new TypeReference<Map<String, Long>>() {
                    });
                    if (payload == null || payload.isEmpty() || !payload.containsKey("typeId") ||
                            BehaviorEnum.isStatusLaw(Math.toIntExact(payload.get("typeId")))) {
                        if (channel.isOpen()) {
                            channel.basicAck(deliveryTag, false);
                        }
                        log.warn("msgId:{} 载荷类别不存在", rabbitMqMessage.getMessageId());
                        return true;
                    }

                    if (BehaviorEnum.VIEW.getCode().equals(Integer.parseInt(String.valueOf(payload.get("typeId"))))) {
                        userBehaviorService.incrementReadCount(payload.get("blogId"), payload.get("userId"));
                    } else {
                        userBehaviorService.userBehaviorBlog(payload.get("blogId"), payload.get("userId")
                                , BehaviorEnum.getBehaviorEnum(Math.toIntExact(payload.get("typeId"))));
                    }
                    return true;
                } catch (IOException e) {
                    log.error("数据处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
                    return false;
                }
            });
            if(call == null || !call){
                log.warn("msgId:{} 重试消息处理失败", rabbitMqMessage.getMessageId());
            }
        } catch (ExecutionException | RetryException e) {
            log.error("重试消息处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
            throw new RuntimeException(e);
        }
    }



}
