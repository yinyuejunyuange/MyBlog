package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("blog_comment")
public class Comment {
    @TableId("id")
    private Long id; // 评论id
    @TableField("user_id")
    private Long userId;
    @TableField("blog_id")
    private Long blogId; // 关联的博客id
    @TableField("context")
    private String context;
    @TableField("kudos")
    private Long kudos;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
    @TableField("is_visible")
    private Integer isVisible; // 当前评论状态
}
