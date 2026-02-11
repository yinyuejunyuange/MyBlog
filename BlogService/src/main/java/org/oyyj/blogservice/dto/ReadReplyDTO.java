package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadReplyDTO {
    private String id;
    private String userId;
    private String userName;
    private String userImage;
    private Long replyId;
    private String replyName;
    private String context;
    private String updateTime;
    private Long commentId;
    private String kudos; // 点赞数
    private Boolean isUserKudos;// 判断当前用户是否点赞
    private Boolean isBelongUser; // 判断回复是否属于用户发起者
}
