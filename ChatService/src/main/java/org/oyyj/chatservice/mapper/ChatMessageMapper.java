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


}