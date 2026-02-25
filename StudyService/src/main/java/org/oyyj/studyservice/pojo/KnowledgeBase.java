package org.oyyj.studyservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private Date updateTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete; // 逻辑删除：1-已删除，0-未删除

    @TableField("name")
    private String name;      // 知识库名称

    @TableField("icon")
    private String icon;      // 图标URL或图标类名

    @TableField("description")
    private String description; // 知识库简介

}
