package org.oyyj.chatservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_follow_info")
public class UserFollowInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer isUserKnow;

    private Long followUserId;

    private Integer isDelete;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userHead;

}