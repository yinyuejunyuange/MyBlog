package org.oyyj.chatservice.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatDialogDTO {

    private String id;

    private String userId;

    private String userName;

    private String headHead;

    private String lastMessage;

    private Date lastMessageTime;

    private Integer count;

    /**
     * 用户是否免打扰
     */
    private Integer isUserMute;

}
