package org.oyyj.chatservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;


@Data
@TableName("user_chat_info")
public class UserChatInfo {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 对话ID（雪花ID）
     */
    private Long dialogId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 是否显示
     * 0 不显示
     * 1 显示
     */
    private Integer isVisible;

    /**
     * 是否免打扰
     * 0 关闭
     * 1 开启
     */
    private Integer isDisturb;

    /**
     * 最近一次消息时间
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
