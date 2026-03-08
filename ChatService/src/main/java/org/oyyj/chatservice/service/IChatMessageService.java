package org.oyyj.chatservice.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.ChatMessage;
import org.oyyj.chatservice.pojo.vo.ChatMsgVO;
import org.oyyj.mycommonbase.common.auth.LoginUser;

import java.util.List;

/**
 * 聊天消息服务接口
 */
public interface IChatMessageService extends IService<ChatMessage> {

    /**
     * 保存消息（包含幂等检查）
     * @param message 消息实体
     * @return 是否保存成功
     */
    boolean saveMessage(ChatMessage message);

    /**
     * 查询两个用户之间的聊天记录（分页）
     * @param userId1 用户A
     * @param userId2 用户B
     * @param page 页码
     * @param size 每页条数
     * @return 消息列表
     */
    List<ChatMessage> getHistoryBetweenUsers(String userId1, String userId2, int page, int size);

    /**
     * 获取用户未读消息列表（status=1 且 is_delete=0）
     * @param userId 用户ID
     * @return 未读消息列表
     */
    List<ChatMessage> getUnreadMessages(String userId);

    /**
     * 将消息标记为已送达/已读
     * @param msgId 消息ID
     * @param targetStatus 目标状态（2-已送达，3-已读）
     * @return 是否更新成功
     */
    boolean updateMessageStatus(String msgId, int targetStatus);

    /**
     * 逻辑删除消息（将 is_delete 置为1）
     * @param msgId 消息ID
     * @return 是否成功
     */
    boolean softDeleteMessage(String msgId);


    /**
     * 查询历史消息记录
     * @param loginUser
     * @param dialogId
     * @param lastMsgId
     * @return
     */
    List<ChatMsgVO> messageList(LoginUser loginUser ,
                                String dialogId,
                                String lastMsgId);
}
