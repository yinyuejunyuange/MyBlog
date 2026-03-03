package org.oyyj.studyservice.dto.knowledgePointComment;

import lombok.Data;

/**
 * 评论添加时字段
 */
@Data
public class CommentAddDTO {

    private String knowledgeId;

    private String content;

}
