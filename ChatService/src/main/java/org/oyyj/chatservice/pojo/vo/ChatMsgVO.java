package org.oyyj.chatservice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMsgVO {

    private String id;                     // 自增主键
    private String msgId;                // 全局唯一消息ID
    private String content;               // 消息内容
    private String dialogId;                // 对话记录ID
    private Date createdAt;
    private Boolean isBelongUser;        // 是否属于当前用户的信息

}
