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
@TableName("role_menu")
public class RoleMenu {
    @MppMultiId
    @TableField("role_id")
    private Long roleId;
    @MppMultiId
    @TableField("menu_id")
    private Long menuId;
    @TableField("is_valid")
    private Integer isValid;
}
