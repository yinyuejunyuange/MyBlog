package org.oyyj.chatservice.pojo;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;


@Data
@TableName("user_like_start_info")
public class UserLikeStartInfo {

    @TableId(value="id",  type = IdType.AUTO)
    private Long id;

    /**
     * 操作者
     */
    private Long userId;

    /**
     * 被行为者
     */
    private Long behaviorId;

    private Long targetId;

    /**
     * 点赞类型 1文章 2评论 3.回复
     */
    private Integer targetType;

    /**
     * 行为 1点赞 2收藏
     */
    private Integer behaviour;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String behaviorUserName;

    @TableField(exist = false)
    private String behaviorUserHead;

    @TableField(exist = false)
    private String targetContent;

}
