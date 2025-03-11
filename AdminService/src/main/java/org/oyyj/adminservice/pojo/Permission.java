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
@TableName("permission")
public class Permission {
    @TableId("id")
    private Long id;
    @TableField("permission")
    private Long permission;

    @TableField("is_using")
    private Integer isUsing;
}
