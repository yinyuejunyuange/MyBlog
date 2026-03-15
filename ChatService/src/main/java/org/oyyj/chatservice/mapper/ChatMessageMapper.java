package org.oyyj.chatservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.chatservice.pojo.ChatMessage;

import java.util.List;

/**
 * 聊天消息 Mapper 接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    List<ChatMessage> selectLastMessageByDialogId(@Param("dialogs") List<Long> dialogIds);

    List<ChatMessage> selectSendMessageByToUserId(@Param("toUserId")String toUserId);

    /**
     * 用户是否允许陌生人私信
     * @param userId
     * @return
     */
    Integer isUserAllowStrange(@Param("userId") Long userId);


    Boolean insertUserChatSetting(@Param("userId") Long userId,@Param("allowStranger") Integer allowStranger);

    Boolean updateUserAllowStranger(@Param("userId") Long userId,@Param("allowStranger") Integer allowStranger);

}