package org.oyyj.taskservice.pojo;

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
@TableName("announcement_user")
public class AnnouncementUser {
    @MppMultiId
    @TableField("announcement_id")
    private Long announcementId;
    @MppMultiId
    @TableField("user_id")
    private Long userId;
}
