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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("reply_table")
public class Reply {
    @TableId("id")
    private Long id; //回复id
    @TableField("user_id")
    private Long userId; // 回复者id
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
