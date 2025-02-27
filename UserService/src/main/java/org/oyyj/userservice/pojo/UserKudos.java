package org.oyyj.userservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user_kudos")
/**
 * 用户点赞表 双主键
 */
public class UserKudos {
    @MppMultiId
    @TableField("user_id")
    private Long userId;
    @MppMultiId
    @TableField("blog_id")
    private Long blogId;
}
