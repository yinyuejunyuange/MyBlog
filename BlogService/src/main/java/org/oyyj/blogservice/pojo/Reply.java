package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_reply")
public class Reply {
    @TableId(value = "id", type =  IdType.AUTO)
    private Long id; //回复id
    @TableField("user_id")
    private Long userId; // 回复者id
    @TableField("user_name")
    private String userName; // 回复者名称
    @TableField("user_image")
    private String userImage; // 回复者用户头像
    @TableField("replied_id")
    private Long repliedId; // 被回复者ID
    @TableField("replied_name")
    private String repliedName; // 被回复者名称
    @TableField("comment_id")
    private Long commentId;// 关联的评论id
    @TableField("context")
    private String context; // 回复内容
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
    private Integer isVisible;
}
