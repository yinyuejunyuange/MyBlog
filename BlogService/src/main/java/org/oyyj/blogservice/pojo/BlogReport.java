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
@TableName("blog_report")
public class BlogReport {
    @TableId("id")
    private Long id;
    @TableField("blog_id")
    private Long blogId;
    @TableField("blog_name")
    private String blogName;
    @TableField("user_id")
    private Long userId;
    @TableField("user_name")
    private String userName;
    @TableField("content")
    private String content;
    @TableField("create_time")
    private Date createTime;
    @TableField("finish_time")
    private Date finishTime;
    @TableField("finish_id")
    private Long finishId;
    @TableField("finish_name")
    private String finishName;
    @TableField("status")
    private Integer status;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}
