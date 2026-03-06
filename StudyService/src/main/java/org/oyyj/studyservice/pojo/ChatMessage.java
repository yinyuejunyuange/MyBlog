package org.oyyj.studyservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private String sessionId;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField(value = "knowledge_point_id")
    private Long knowledgePointId;

    @TableField("knowledge_point_name")
    private String knowledgePointName;


    @TableField(value = "round_num")
    private Integer roundNum = 0;  // 默认为0

    @TableField(value = "role")
    private String role;

    @TableField(value = "content")
    private String content;

    /**
     * 是否完成评论
     */
    @TableField("finish_comment")
    private Integer finishComment;

    /**
     * 回答的问题的ID
     */
    @TableField("answer_for_id")
    private Long answerForId;

    /**
     * 调用模型后得到的评价
     */
    @TableField("comment")
    private String comment;

    @TableField("sort")
    private Integer sort;

    @TableField("is_finish")
    private Integer isFinish;

    /**
     * 创建时间，插入时自动填充当前时间
     * 需要配合 MetaObjectHandler 使用
     */
    @TableField(value = "create_time")
    private Date createTime;

}
