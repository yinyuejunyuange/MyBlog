package org.oyyj.adminservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.annotation.security.DenyAll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_menu")
public class SysMenu {
    @TableId("id")
    private Long id;
    @TableField("name")
    private String name;
    @TableField("parent_id")
    private Long parentId;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
    @TableField("create_time")
    private Date createTime;
    @TableField("create_by")
    private Long createBy;
    @TableField("update_time")
    private Date updateTime;
    @TableField("update_by")
    private Long updateBy;
    @TableField("sort")
    private Integer sort;
    @TableField("url")
    private String url;
}
