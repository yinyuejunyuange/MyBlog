package org.oyyj.studyservice.dto.knowledgePointComment;

import lombok.Data;

import java.util.Date;

/**
 * 知识点展示评论数据结构
 */
@Data
public class KnowledgePointReplyDTO {


    private String id;

    /**
     * 关联知识点ID
     */
    private String knowledgeId;

    /**
     * 祖评论ID（一级评论ID）
     */
    private String rootId;

    /**
     * 直接父ID（评论或回复）
     */
    private String parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 被回复的用户ID
     */
    private String replyUserId;

    private String userId;

    private String userName;

    private String replyUserName;

    private String userHead;

    private String replyUserHead;

    private Date publishTime;

    private Boolean isBelongUser;

    private Integer isVisible;

}
