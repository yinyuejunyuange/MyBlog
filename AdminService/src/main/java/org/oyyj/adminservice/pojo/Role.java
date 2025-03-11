package org.oyyj.adminservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("role")
/**
 * 角色 类
 */
public class Role {
    @TableId("id")
    private Long id;
    @TableField("admin_type")
    private String adminType;

    @TableField("is_using")
    private Integer isUsing;
}
