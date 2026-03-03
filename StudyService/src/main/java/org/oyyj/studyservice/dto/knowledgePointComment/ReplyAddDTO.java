package org.oyyj.studyservice.dto.knowledgePointComment;

import lombok.Data;

@Data
public class ReplyAddDTO {
    
    private String knowledgeId;
    private String rootId;
    private String parentId;
    private String replyUserId;
    private String replyUserName;
    private String content;
}
