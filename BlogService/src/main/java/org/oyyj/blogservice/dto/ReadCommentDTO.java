package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadCommentDTO {
    private String id;
    private String userId;
    private String userName;
    private String UserImage;
    private String context;
    private String kudos;
    private List<ReadReplyDTO> replyList;
    private Date updateTime;
    private Boolean isUserKudos;// 判断当前用户是否点赞
}
