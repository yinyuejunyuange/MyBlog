package org.oyyj.adminservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("admin")
/**
 * 分成系统管理员（管理普通管理员  管理系统日志  ）和普通管理员（管理用户 博客 评论 定时任务等）
 */
public class Admin {
    @TableId("id")
    private Long id;
    @TableField("name")
    private String name;
    @TableField("password")
    private String password;
    @TableField("image_url")
    private String imageUrl;
    @TableField("phone")
    private String phone;
    @TableField("email")
    private String email;
    @TableField("create_time")
    private Date createTime;
    @TableField("create_by")
    private Long createBy;
    @TableField("update_time")
    private Date updateTime;
    @TableField("update_by")
    private Long updateBy;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete; // 是否删除
    @TableField("is_freeze")
    private Integer isFreeze; // 是否冻结
    @TableField(exist = false)
    private Integer adminType; // 管理员类别(1:系统管理员 2：普通管理员)  数据库使用 rbac权限模型展示
}
