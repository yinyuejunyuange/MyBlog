package org.oyyj.chatservice.pojo;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 聊天消息实体类
 * 对应数据库表 chat_message
 */
@Data
@Accessors(chain = true)
@TableName("chat_message")
public class ChatMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;                     // 自增主键

    @TableField("msg_id")
    private String msgId;                // 全局唯一消息ID

    @TableField("from_user_id")
    private String fromUserId;            // 发送者ID

    @TableField("to_user_id")
    private String toUserId;              // 接收者ID（单聊）或群聊ID（群聊）

    @TableField("chat_type")
    private Integer chatType;             // 聊天类型：1-单聊，2-群聊

    @TableField(exist = false)
    private String type;                  // 心跳：heartbeat 对话：chat

    @TableField("content")
    private String content;               // 消息内容

    @TableField("dialog_id")
    private Long dialogId;                // 对话记录ID

    @TableField("content_type")
    private Integer contentType;          // 内容类型：1-文本，2-图片...

    @TableField("status")
    private Integer status;               // 消息状态：1-已发送，2-已送达...

    @TableField("timestamp")
    private Long timestamp;               // 客户端消息时间戳（毫秒）

    @TableField(value = "created_at")
    private Date createdAt;       // 创建时间

    @TableField(value = "updated_at")
    private Date updatedAt;       // 更新时间

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;              // 是否删除：0-未删除，1-已删除

    @Getter
    public enum ChatMessageStatus {
        SEND(0,"发送"),
        READ(1,"已读"),
        WITHDRAWAL(2,"撤回");

        private final Integer code;
        private final String msg;
        ChatMessageStatus(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static ChatMessageStatus getChatMessageStatus(Integer code){
            for (ChatMessageStatus status : ChatMessageStatus.values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }

}
