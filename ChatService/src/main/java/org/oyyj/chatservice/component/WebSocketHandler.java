package org.oyyj.chatservice.component;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.chatservice.mapper.ChatMessageMapper;
import org.oyyj.chatservice.mq.sender.MessagePublishSender;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.MqMessageLog;
import org.oyyj.chatservice.pojo.es.MessageDocument;
import org.oyyj.chatservice.pojo.ws.ResultInfo;
import org.oyyj.chatservice.service.es.MessageDocumentService;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理消息
 */
@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MessagePublishSender messagePublishSender; // RabbitMQ 生产者

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private MessageDocumentService messageDocumentService;


    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 本地会话池：userId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 当前实例唯一标识
    private final String instanceId = UUID.randomUUID().toString();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(userId, session);
            // 向 Redis 注册当前用户所在实例
            redisUtil.set(RedisPrefix.CHAT_MSG_INSTANCE+userId, instanceId);
            // 可推送在线状态给好友（略）
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String fromUserId = (String) session.getAttributes().get("userId");

        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
        if ("chat".equals(chatMessage.getType())) {
            handleChatMessage(fromUserId, chatMessage,session);
        } else if ("heartbeat".equals(chatMessage.getType())) {
            // 处理心跳，可回复 pong
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    /**
     * 消息处理器
     * @param fromUserId
     * @param chatMessage
     * @param session
     * @throws IOException
     */
    private void handleChatMessage(String fromUserId, ChatMessage chatMessage, WebSocketSession session ) throws IOException {



        if(chatMessage == null){
            log.error("消息不可为空");
            ResultInfo result = new ResultInfo("RESULT",null, ResultInfo.ResultInfoEnum.DATA_WRONG.getValue(), null);
            //发送确认消息给客户端 使得消息是否发送成功
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
            return;
        }
        chatMessage.setFromUserId(fromUserId);
        if(chatMessage.getStatus() == null){
            log.error("消息的发送状态不可为空，msgId:{}",chatMessage.getMsgId());
            ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.DATA_WRONG.getValue(), null);
            //发送确认消息给客户端 使得消息是否发送成功
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
            return;
        }

        String toUserId = chatMessage.getToUserId();
        long timestamp = System.currentTimeMillis();
        // 存储到数据库
        chatMessage.setTimestamp(timestamp);
        // 先判断是否存在重复发送
        ChatMessage selectOne = chatMessageMapper.selectOne(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getMsgId, chatMessage.getMsgId())
        );
        if(selectOne != null && ChatMessage.ChatMessageStatus.SEND.getCode().equals(chatMessage.getStatus())){
            log.error("消息重复发送：{}",chatMessage.getMsgId());

            ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.REPEAT.getValue(), null);
            //发送确认消息给客户端 使得消息是否发送成功
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
            return;
        }
        int exeNum = 0;
        // 发送命令
        if(ChatMessage.ChatMessageStatus.SEND.getCode().equals(chatMessage.getStatus())){
            exeNum= chatMessageMapper.insert(chatMessage);
        }else{
            // 其他处理方式
            ChatMessage.ChatMessageStatus chatMessageStatus = ChatMessage.ChatMessageStatus.getChatMessageStatus(chatMessage.getStatus());
            if(chatMessageStatus == null){
                log.error("消息状态不存在{}，状态{}",chatMessage.getMsgId(),chatMessage.getStatus());
                ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.DATA_WRONG.getValue(), null);
                //发送确认消息给客户端 使得消息是否发送成功
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
                return;
            }

            exeNum = chatMessageMapper.update(Wrappers.<ChatMessage>lambdaUpdate()
                    .eq(ChatMessage::getMsgId, chatMessage.getMsgId())
                    .set(ChatMessage::getStatus,chatMessage.getStatus())
            );

        }


        if (exeNum == 0) {
            log.error("消息数据保存失败");
            //发送确认消息给客户端 使得消息是否发送成功
            ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.FAIL_SAVE.getValue(), null);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
            return;
        }
        // 1. 异步保存消息到 Elasticsearch（用于历史记录和搜索）
        MessageDocument doc = new MessageDocument();
        doc.setId(chatMessage.getMsgId());
        doc.setFromUserId(fromUserId);
        doc.setToUserId(toUserId);
        doc.setContent(chatMessage.getContent());
        doc.setTimestamp(timestamp);
        doc.setStatus(chatMessage.getStatus());

        messageDocumentService.save(doc);

        // 2. 判断目标用户在线实例
        String targetInstance = redisUtil.getString(RedisPrefix.CHAT_MSG_INSTANCE+ toUserId);
        if (targetInstance == null) {
            // 用户不在线，消息已存入 ES，等待上线拉取离线消息
            ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.SUCCESS.getValue(), null);
            //发送确认消息给客户端 使得消息是否发送成功
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
            return;
        }

        if (instanceId.equals(targetInstance)) {
            // 目标在本实例，直接推送
            WebSocketSession targetSession = sessions.get(toUserId);
            if (targetSession != null && targetSession.isOpen()) {
                try {
                    ResultInfo result = new ResultInfo("SEND", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.SUCCESS.getValue(), chatMessage);
                    targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
                } catch (IOException e) {
                    // 发送失败，可考虑重试或记录日志
                    log.error("消息发送到本地实例失败：{}",targetSession.getId(),e);
                    // 消息处理结果
                    ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.FAIL_SEND.getValue(), null);
                    //发送确认消息给客户端 使得消息是否发送成功
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(result)));
                }
            }
        } else {
            // 目标在其他实例，通过 RabbitMQ 转发
            messagePublishSender.sendMessage(chatMessage,targetInstance);
        }
        ResultInfo result = new ResultInfo("RESULT", chatMessage.getMsgId(), ResultInfo.ResultInfoEnum.SUCCESS.getValue(), null);
        //发送确认消息给客户端 使得消息是否发送成功
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            redisUtil.delete(RedisPrefix.CHAT_MSG_INSTANCE+userId);
        }
    }

    // 提供给 RabbitMQ 消费者调用的推送方法
    public void sendToUser(String userId, ChatMessage message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setType("SEND");
                resultInfo.setMsgId(message.getMsgId());
                resultInfo.setResult("success");
                resultInfo.setData(message);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resultInfo)));
            } catch (IOException e) {
                // 处理异常
                log.error("收到需要发送给:{}的消息：{}，推送失败", userId, message, e);
            }
        }
    }


}
