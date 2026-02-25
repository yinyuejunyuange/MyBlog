package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("blog_comment")
public class Comment {
    @TableId(value = "id", type = IdType.AUTO)  // 手动设置 避免mybatisplus的默认雪花算法
    private Long id; // 评论id
    @TableField("user_id")
    private Long userId;
    @TableField("user_name")
    private String userName; // 回复者名称
    @TableField("user_image")
    private String userImage; // 回复者头像
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

    /**
     * 是否具有攻击性
     */
    @TableField("is_toxic")
    private Integer isToxic;

    /**
     * 攻击性概率
     */
    @TableField("p_toxic")
    private BigDecimal pToxic;

    /**
     * 攻击类别 (list 的字符串)
     */
    @TableField("mul_type")
    private String mulType;

    /**
     * 各个类别对应的攻击性概率（map转成字符串）
     */
    @TableField("p_topic")
    private String pTopic;

}
