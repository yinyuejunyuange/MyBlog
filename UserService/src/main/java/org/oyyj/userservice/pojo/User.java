package org.oyyj.userservice.pojo;

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
@TableName("user")
public class User {
    @TableId("id")
    private Long id;
    @TableField("name")
    private String name;
    @TableField("password")
    private String password;
    @TableField("image_url")
    private String imageUrl;
    @TableField("sex")
    private Integer sex;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("email")
    private String email;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
    @TableField("is_freeze")
    private Integer isFreeze;
    @TableField("star")
    private Long star;
    @TableField("introduce")
    private String introduce;
}
