package org.oyyj.userservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 权限类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_permissions")
public class SysPermissions {
    @TableId("id")
    private Long id;
    @TableField("permissions_name")
    private String permissionName;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("is_delete")
    private Integer isDelete;
    @TableField("is_stop")
    private Integer isStop;
}
