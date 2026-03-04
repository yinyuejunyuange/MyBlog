package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.studyservice.pojo.ChatMessage;

public interface ChatMessageService extends IService<ChatMessage> {
    /**
     * 异步执行 得到用户回答信息的评价
     * @param messageId
     */
    void getUserMessageComment(Long messageId);

}
