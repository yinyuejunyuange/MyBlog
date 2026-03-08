package org.oyyj.chatservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;


@Data
@TableName("chat_dialog")
public class ChatDialog {

    /**
     * 对话ID（雪花算法）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一Key
     * 例如 1001_1002
     */
    private String conversationKey;

    /**
     * 对话类型
     * 1 私聊
     * 2 群聊
     */
    private Integer dialogType;

    /**
     * 创建人（群聊使用）
     */
    private Long creatorId;

    /**
     * 最后一条消息时间
     */
    private Date lastMessageTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}
