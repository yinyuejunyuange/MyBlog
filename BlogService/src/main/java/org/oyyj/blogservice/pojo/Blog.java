package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("blog")
public class Blog {
    @TableId("id")
    private Long id;
    @TableField("title")
    private String title;
    @TableField("context")
    private String context;
    @TableField("user_id")
    private Long userId;
    @TableField("author")
    private String author;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("status")
    private Integer status;
    @TableField("is_delete")
    private Integer isDelete;
    @TableField(exist = false)
    private List<String> typeList=new ArrayList<>();
    @TableField("introduce")
    private String introduce;
    @TableField("kudos")
    private Long kudos; // 点赞数
    @TableField("star")
    private Long star; // 收藏数
    @TableField("watch")
    private Long watch; // 阅读量
    @TableField("comment_num")
    private Long commentNum; // 博客总评论数
    @TableField("publish_time")
    private Date publishTime;
    @TableField(exist = false)
    private BigDecimal commentPrice; //评论情绪得分

}
