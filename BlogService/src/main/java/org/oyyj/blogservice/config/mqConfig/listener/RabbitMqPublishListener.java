package org.oyyj.blogservice.config.mqConfig.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.mycommon.common.BehaviorEnum;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.config.RetryConfig;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class RabbitMqPublishListener {

    @Autowired
    private MqMessageRecordMapper mqMessageRecordMapper;

    @Autowired
    private IBlogService blogService;

    @Value("${spring.cloud.nacos.discovery.cluster-name}")
    private String instanceName;

    @Autowired
    private RedisUtil redisUtil;

    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = MqPrefix.BLOG_PUBLISH_QUEUE)
    public void handleBlogPublishMessage(RabbitMqMessage rabbitMqMessage,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) int deliveryTag){

        try {
            Boolean call = RetryConfig.LOCK_RETRYER.call(() -> {
                log.info("接收到博客发布行为信息,payload:{},deliveryTag:{}", rabbitMqMessage, deliveryTag);
                // 判断数据是否已经处理完毕
                MqMessageRecord mqMessageRecord = mqMessageRecordMapper.selectOne(Wrappers.<MqMessageRecord>lambdaQuery()
                        .eq(MqMessageRecord::getMsgId, rabbitMqMessage.getMessageId())
                );
                if (mqMessageRecord == null) {
                    log.warn("msgId:{} 数据不存在", rabbitMqMessage.getMessageId());
                    return true;
                }

                if (MqStatusEnum.SUCCESS.getCode().equals(mqMessageRecord.getExecStatus()) ||
                        MqStatusEnum.FAIL.getCode().equals(mqMessageRecord.getExecStatus())) {
                    log.warn("msgId:{} 数据已处理", rabbitMqMessage.getMessageId());
                    return true;
                }
                // 处理请求的逻辑
                String blogStr = rabbitMqMessage.getPayLoad();
                long blogId = Long.parseLong(blogStr);
                boolean update = blogService.update(Wrappers.<Blog>lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .set(Blog::getStatus, 2)
                );
                if(!update){
                    return  false;
                }

                // 修改MQ信息的数据库状态
                int success = mqMessageRecordMapper.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                        .eq(MqMessageRecord::getMsgId, rabbitMqMessage.getMessageId())
                        .set(MqMessageRecord::getExecStatus, MqStatusEnum.SUCCESS.getCode())
                );

                if(success == 0){
                    return  false;
                }
                // 删除redis中的数据
                redisUtil.zRem(RedisPrefix.BLOG_PUBLISH_ZSET+instanceName , String.valueOf(blogId));
                return  true;

            });
            if (channel.isOpen()) {
                if (call) {
                    channel.basicAck(deliveryTag, false);
                } else {
                    channel.basicNack(deliveryTag, false, true); // 或 false 进 DLQ
                }
            }
        } catch (ExecutionException | RetryException e) {
            log.error("重试消息处理失败 msgId:{} ", rabbitMqMessage.getMessageId(), e);
            mqMessageRecordMapper.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                    .eq(MqMessageRecord::getMsgId, rabbitMqMessage.getMessageId())
                    .set(MqMessageRecord::getExecStatus, MqStatusEnum.FAIL.getCode())
            );
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
