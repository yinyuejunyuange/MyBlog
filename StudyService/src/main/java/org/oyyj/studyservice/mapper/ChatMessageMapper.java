package org.oyyj.studyservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.vo.chatMessage.ChatMessageVO;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    List<ChatMessageVO> listUserHistory(@Param("userId") Long userId);

}
