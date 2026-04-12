package org.oyyj.chatservice.service.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.apache.logging.log4j.util.Strings;
import org.oyyj.chatservice.feign.UserFeign;
import org.oyyj.chatservice.mapper.BlacklistMapper;
import org.oyyj.chatservice.mapper.ChatDialogMapper;
import org.oyyj.chatservice.mapper.ChatMessageMapper;
import org.oyyj.chatservice.mapper.UserChatInfoMapper;
import org.oyyj.chatservice.pojo.Blacklist;
import org.oyyj.chatservice.pojo.ChatDialog;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.UserChatInfo;
import org.oyyj.chatservice.pojo.dto.ChatDialogDTO;
import org.oyyj.chatservice.service.ChatDialogService;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatDialogServiceImpl
        extends ServiceImpl<ChatDialogMapper, ChatDialog>
        implements ChatDialogService {


    @Autowired
    private UserChatInfoMapper userChatInfoMapper;

    @Autowired
    private BlacklistMapper blacklistMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserFeign userFeign;

    @Override
    @Transactional
    public Long getOrCreateDialog(Long userA, Long userB) {

        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);

        String conversationKey = min + "_" + max;


        // 先查询
        ChatDialog dialog = getOne(
                Wrappers.<ChatDialog>lambdaQuery()
                        .eq(ChatDialog::getConversationKey, conversationKey)
        );

        if (dialog != null) {
            return dialog.getId();
        }


        ChatDialog newDialog = new ChatDialog();
        newDialog.setConversationKey(conversationKey);
        newDialog.setDialogType(1);
        try {
            save(newDialog);

        } catch (DuplicateKeyException e) {

            // 并发情况下再次查询
            dialog = getOne(
                    Wrappers.<ChatDialog>lambdaQuery()
                            .eq(ChatDialog::getConversationKey, conversationKey)
            );

            return dialog.getId();
        }

        return newDialog.getId();
    }

    @Override
    public ResultUtil<List<ChatDialogDTO>>  dialogList(Long userId) {
        List<UserChatInfo> userChatInfos = userChatInfoMapper.selectList(Wrappers.<UserChatInfo>lambdaQuery()
                .eq(UserChatInfo::getUserId, userId)
        );

        // 查询用户的黑名单列表
        List<Long> blackUserIds = blacklistMapper.selectList(Wrappers.<Blacklist>lambdaQuery()
                .eq(Blacklist::getUserId, userId)
                .select(Blacklist::getBlackUserId)
        ).stream().map(Blacklist::getBlackUserId).toList();



        List<Long> list = userChatInfos.stream().map(UserChatInfo::getDialogId).toList();
        if (list.isEmpty()) {
            return ResultUtil.success(List.of());
        }

        List<UserChatInfo> userChatInfosList = userChatInfoMapper.selectList(Wrappers.<UserChatInfo>lambdaQuery()
                .ne(UserChatInfo::getUserId, userId)
                .in(UserChatInfo::getDialogId, list)
                .notIn(!blackUserIds.isEmpty(), UserChatInfo::getUserId, blackUserIds) // 排除黑名单中的人
        );

        List<ChatMessage> chatMessages = chatMessageMapper.selectLastMessageByDialogId(list);

        List<ChatMessage> chatMessagesByUserId = chatMessageMapper.selectSendMessageByToUserId(String.valueOf(userId));
        Map<Long, List<ChatMessage>> collect = chatMessagesByUserId.stream().collect(Collectors.groupingBy(ChatMessage::getDialogId));
        Map<Long, ChatMessage> dialogInfoMap = chatMessages.stream().collect(Collectors.toMap(ChatMessage::getDialogId, Function.identity()));
        List<String> userIds = userChatInfosList.stream().map(item->String.valueOf(item.getUserId())).toList();
        Map<Long, String> imageInIds;
        if(!userIds.isEmpty()){
            imageInIds = userFeign.getImageInIds(userIds);
        }else{
            imageInIds = new HashMap<>();
        }


        List<ChatDialogDTO> result = new ArrayList<>();
        for (UserChatInfo userChatInfo : userChatInfosList) {
            ChatDialogDTO chatDialogDTO = new ChatDialogDTO();
            chatDialogDTO.setId(String.valueOf(userChatInfo.getDialogId()));
            chatDialogDTO.setUserId(String.valueOf(userChatInfo.getUserId()));
            chatDialogDTO.setUserName(userChatInfo.getUserName());
            chatDialogDTO.setIsUserMute(userChatInfo.getIsDisturb());
            if(dialogInfoMap.containsKey(userChatInfo.getDialogId())) {
                ChatMessage chatMessage = dialogInfoMap.get(userChatInfo.getDialogId());
                chatDialogDTO.setLastMessage(chatMessage.getContent());
                chatDialogDTO.setLastMessageTime(chatMessage.getCreatedAt());
            }
            if(imageInIds.containsKey(userChatInfo.getUserId())) {
                chatDialogDTO.setHeadHead(imageInIds.get(userChatInfo.getUserId()));
            }
            if(collect.containsKey(userChatInfo.getDialogId())) {
                List<ChatMessage> chatMessagesList = collect.get(userChatInfo.getDialogId());
                chatDialogDTO.setCount(chatMessagesList.size());
            }else{
                chatDialogDTO.setCount(0);
            }
            result.add(chatDialogDTO);
        }
        return ResultUtil.success(result);
    }
}