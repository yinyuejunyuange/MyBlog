package org.oyyj.studyservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointCommentDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointReplyDTO;

import java.util.Date;

@Data
@TableName("knowledge_point_comment")
public class KnowledgePointComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联知识点ID
     */
    private Long knowledgeId;

    /**
     * 祖评论ID（一级评论ID）
     */
    private Long rootId;

    /**
     * 直接父ID---回复的ID
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 被回复的用户ID
     */
    private Long replyUserId;

    private Long userId;

    private String userName;

    private String replyUserName;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除 1已删除 0未删除
     */
    @TableLogic
    private Integer isDelete;


    public KnowledgePointCommentDTO toDTO() {

        KnowledgePointCommentDTO dto = new KnowledgePointCommentDTO();

        if (this.id != null) {
            dto.setId(String.valueOf(this.id));
        }

        if (this.knowledgeId != null) {
            dto.setKnowledgeId(String.valueOf(this.knowledgeId));
        }

        if (this.rootId != null) {
            dto.setRootId(String.valueOf(this.rootId));
        }

        if (this.parentId != null) {
            dto.setParentId(String.valueOf(this.parentId));
        }

        if (this.replyUserId != null) {
            dto.setReplyUserId(String.valueOf(this.replyUserId));
        }

        if (this.userId != null) {
            dto.setUserId(String.valueOf(this.userId));
        }

        dto.setContent(this.content);
        dto.setUserName(this.userName);
        dto.setReplyUserName(this.replyUserName);

        // createTime → publishTime
        dto.setPublishTime(this.createTime);

        return dto;
    }

    public KnowledgePointReplyDTO toReplyDTO() {

        KnowledgePointReplyDTO dto = new KnowledgePointReplyDTO();

        if (this.id != null) {
            dto.setId(String.valueOf(this.id));
        }

        if (this.knowledgeId != null) {
            dto.setKnowledgeId(String.valueOf(this.knowledgeId));
        }

        if (this.rootId != null) {
            dto.setRootId(String.valueOf(this.rootId));
        }

        if (this.parentId != null) {
            dto.setParentId(String.valueOf(this.parentId));
        }

        if (this.replyUserId != null) {
            dto.setReplyUserId(String.valueOf(this.replyUserId));
        }

        if (this.userId != null) {
            dto.setUserId(String.valueOf(this.userId));
        }

        dto.setContent(this.content);
        dto.setUserName(this.userName);
        dto.setReplyUserName(this.replyUserName);

        // createTime → publishTime
        dto.setPublishTime(this.createTime);

        return dto;
    }
}