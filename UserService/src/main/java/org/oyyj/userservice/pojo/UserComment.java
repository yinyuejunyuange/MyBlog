package org.oyyj.userservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user_comment")
public class UserComment {
    @MppMultiId
    @TableField("user_id")
    private Long userId;
    @MppMultiId
    @TableField("comment_id")
    private Long commentId;
}
