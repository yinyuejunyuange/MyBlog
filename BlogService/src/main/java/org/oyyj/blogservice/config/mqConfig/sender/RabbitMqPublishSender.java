package org.oyyj.blogservice.config.mqConfig.sender;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.mycommon.common.mq.MqPrefix;
import org.oyyj.mycommon.common.mq.MqStatusEnum;
import org.oyyj.mycommon.config.pojo.EnhanceCorrelationData;
import org.oyyj.mycommon.config.pojo.RabbitMqMessage;
import org.oyyj.mycommon.mapper.MqMessageRecordMapper;
import org.oyyj.mycommon.pojo.MqMessageRecord;
import org.oyyj.mycommon.service.IMqMessageRecordService;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RabbitMqPublishSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SnowflakeUtil snowflakeUtil;

    @Autowired
    private IMqMessageRecordService messageRecordService;

    private final ObjectMapper mapper = new ObjectMapper();


    /**
     * 发布博客
     * @param blogId
     */
    public Boolean  sendPublishMessage(List<Long> blogId) {
        // 检查是否存在重复信息

        List<Integer> statusList = new ArrayList<>();
        statusList.add(MqStatusEnum.NOT_SEND.getCode());
        statusList.add(MqStatusEnum.PENDING.getCode());
        statusList.add(MqStatusEnum.PROCESSING.getCode());

        List<String> blogIdStr = blogId.stream().map(String::valueOf).toList();

        List<MqMessageRecord> mqMessageRecords = messageRecordService.list(Wrappers.<MqMessageRecord>lambdaQuery()
                .in(MqMessageRecord::getMsgContent, blogIdStr)
                .in(MqMessageRecord::getExecStatus, statusList)
        );
        if(mqMessageRecords != null && !mqMessageRecords.isEmpty()){
            log.warn("用户重复延时发布,blogId:{}  ",blogId);
            List<String> list = mqMessageRecords.stream().map(MqMessageRecord::getMsgContent).toList();
            blogIdStr = blogIdStr.stream().filter(item->!list.contains(item)).toList();
        }

        // 兜底
        // 雪花算法生成唯一ID

        try {
            String blogStr = mapper.writeValueAsString(blogId);
            List<MqMessageRecord> list = blogIdStr.stream().map(item -> {
                MqMessageRecord mqMessageRecord = new MqMessageRecord();
                String snowflakeId = snowflakeUtil.getSnowflakeId();
                mqMessageRecord.setMsgId(snowflakeId);
                mqMessageRecord.setExecStatus(MqStatusEnum.NOT_SEND.getCode());
                mqMessageRecord.setMsgContent(item);
                mqMessageRecord.setTargetQueue(MqPrefix.BLOG_PUBLISH_QUEUE);
                return mqMessageRecord;
            }).toList();
            // 先存储到数据库中
            messageRecordService.saveBatch(list);
            // 发送MQ
            for (MqMessageRecord mqMessageRecord : list) {
                EnhanceCorrelationData enhanceCorrelationData = new EnhanceCorrelationData(mqMessageRecord.getMsgId(), blogStr, 0,true, mqMessageRecord.getMsgContent());
                RabbitMqMessage rabbitMqMessage = new RabbitMqMessage();
                rabbitMqMessage.setMessageId(mqMessageRecord.getMsgId());
                rabbitMqMessage.setRetryCount(0);
                rabbitMqMessage.setPayLoad(mqMessageRecord.getMsgContent());

                rabbitTemplate.convertAndSend(
                        MqPrefix.BLOG_PUBLISH_EXCHANGE,
                        MqPrefix.BLOG_PUBLISH_ROUTING_KEY,
                        rabbitMqMessage,
                        enhanceCorrelationData
                );
                log.info("发送用户行为MQ消息成功，msgId:{}, blogID:{}",mqMessageRecord.getMsgId(),blogStr);
                // 修改MQ记录表中信息的状态
            }
            updateMqStatus(list,MqStatusEnum.PENDING.getCode());
            return true;
        }  catch (JsonProcessingException e) {
            log.error("数据类型转换失败，原因如下：{}",blogId,e);
            throw new RuntimeException(e);
        }
    }

    private void updateMqStatus(List<MqMessageRecord> list,Integer execStatus){

        List<Long> recordIds = list.stream().map(MqMessageRecord::getId).toList();

        boolean update = messageRecordService.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                .in(MqMessageRecord::getMsgId, recordIds)
                .eq(MqMessageRecord::getExecStatus, MqStatusEnum.NOT_SEND.getCode())
                .set(MqMessageRecord::getExecStatus, execStatus)
        );
        if(!update){
            log.warn("消息记录信息修改该失败,msgID:{} , status:{} ",recordIds,execStatus);
        }
    }

    private void updateMqStatus(String snowflakeId,Integer execStatus){
        boolean update = messageRecordService.update(Wrappers.<MqMessageRecord>lambdaUpdate()
                .eq(MqMessageRecord::getMsgId, snowflakeId)
                .eq(MqMessageRecord::getExecStatus, MqStatusEnum.NOT_SEND.getCode())
                .set(MqMessageRecord::getExecStatus, execStatus)
        );
        if(!update){
            log.warn("消息记录信息修改该失败,msgID:{} , status:{} ",snowflakeId,execStatus);
        }
    }


}
