package org.oyyj.chatservice.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.oyyj.chatservice.mapper.ChatMessageMapper;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.vo.ChatMsgVO;
import org.oyyj.chatservice.service.IChatMessageService;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 聊天消息服务实现类
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements IChatMessageService {

    @Override
    public boolean saveMessage(ChatMessage message) {
        // 幂等性检查：根据 msg_id 判断是否已存在
        if (StringUtils.hasText(message.getMsgId())) {
            LambdaQueryWrapper<ChatMessage> wrapper = Wrappers.lambdaQuery(ChatMessage.class)
                    .eq(ChatMessage::getMsgId, message.getMsgId());
            if (this.baseMapper.selectCount(wrapper) > 0) {
                // 消息已存在，直接返回成功（或抛出异常）
                return true;
            }
        }
        // 设置默认值
        if (message.getIsDelete() == null) {
            message.setIsDelete(0);
        }
        return this.save(message);
    }

    @Override
    public List<ChatMessage> getHistoryBetweenUsers(String userId1, String userId2, int page, int size) {
        // 查询条件：单聊 (chat_type=1)，且两个用户分别是 from 和 to，并且未删除
        LambdaQueryWrapper<ChatMessage> wrapper = Wrappers.lambdaQuery(ChatMessage.class)
                .eq(ChatMessage::getChatType, 1)  // 单聊
                .eq(ChatMessage::getIsDelete, 0)
                .and(w -> w.and(
                                wp -> wp.eq(ChatMessage::getFromUserId, userId1)
                                        .eq(ChatMessage::getToUserId, userId2))
                        .or(wp -> wp.eq(ChatMessage::getFromUserId, userId2)
                                .eq(ChatMessage::getToUserId, userId1))
                )
                .orderByDesc(ChatMessage::getTimestamp); // 按时间倒序（最新的在前）

        Page<ChatMessage> pageResult = this.page(new Page<>(page, size), wrapper);
        return pageResult.getRecords();
    }

    @Override
    public List<ChatMessage> getUnreadMessages(String userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = Wrappers.lambdaQuery(ChatMessage.class)
                .eq(ChatMessage::getToUserId, userId)   // 接收者为该用户
                .eq(ChatMessage::getStatus, 1)          // 状态为已发送（未送达/未读）
                .eq(ChatMessage::getIsDelete, 0)
                .orderByAsc(ChatMessage::getTimestamp);  // 按时间正序
        return this.list(wrapper);
    }

    @Override
    public boolean updateMessageStatus(String msgId, int targetStatus) {
        LambdaQueryWrapper<ChatMessage> wrapper = Wrappers.lambdaQuery(ChatMessage.class)
                .eq(ChatMessage::getMsgId, msgId);
        ChatMessage updateEntity = new ChatMessage();
        updateEntity.setStatus(targetStatus);
        return this.update(updateEntity, wrapper);
    }

    @Override
    public boolean softDeleteMessage(String msgId) {
        return removeById(msgId);
    }

    @Override
    public List<ChatMsgVO> messageList(LoginUser loginUser, String dialogId, String lastMsgId) {
        List<ChatMessage> list = list(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getDialogId, Long.parseLong(dialogId))
                .and(wrapper->
                        wrapper.eq(ChatMessage::getStatus,ChatMessage.ChatMessageStatus.SEND.getCode())
                                .or()
                                .eq(ChatMessage::getStatus,ChatMessage.ChatMessageStatus.READ.getCode())
                )
                .lt(lastMsgId != null, ChatMessage::getId, lastMsgId).orderByDesc(ChatMessage::getTimestamp)
                .last("limit 20")
        );

        return list.stream().map(item -> {
            ChatMsgVO vo = new ChatMsgVO();
            vo.setId(String.valueOf(item.getId()));
            vo.setMsgId(item.getMsgId());
            vo.setContent(item.getContent());
            vo.setDialogId(String.valueOf(item.getDialogId()));
            vo.setCreatedAt(item.getCreatedAt());
            vo.setIsBelongUser(String.valueOf(loginUser.getUserId()).equals(item.getFromUserId()));
            return vo;
        }).toList();
    }
}