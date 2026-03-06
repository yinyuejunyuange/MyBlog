package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.ai.InterviewEvaluationDTO;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.vo.chatMessage.ChatMessageVO;
import org.oyyj.studyservice.vo.chatMessage.InterviewEvaluationVO;

import java.util.List;


public interface ChatMessageService extends IService<ChatMessage> {
    /**
     * 异步执行 得到用户回答信息的评价
     * @param messageId
     */
    void getUserMessageComment(Long messageId);


    ResultUtil<List<ChatMessageVO>> listUserHistory(LoginUser loginUser);

    ResultUtil<List<InterviewEvaluationVO>> listBySessionId(String sessionId);

}
