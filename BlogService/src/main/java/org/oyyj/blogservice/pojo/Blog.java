package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.blogservice.enums.TypeEnum;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("blog")
public class Blog {
    @TableId("id")
    private Long id;
    @TableField("title")
    private String title;
    @TableField("context")
    private String context;
    @TableField("user_id")
    private Long userId;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("status")
    private Integer status;
    @TableField("is_delete")
    private Integer isDelete;
    @TableField(exist = false)
    private List<String> typeList=new ArrayList<>();
    @TableField("introduce")
    private String introduce;
}
