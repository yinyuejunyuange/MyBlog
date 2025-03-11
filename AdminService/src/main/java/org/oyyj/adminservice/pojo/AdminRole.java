package org.oyyj.adminservice.pojo;

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
@TableName("admin_role")
public class AdminRole {
    @MppMultiId
    @TableField("admin_id")
    private Long adminId;
    @MppMultiId
    @TableField("role_id")
    private Long roleId;

    @TableField("is_valid")
    private Integer isValid;

}
