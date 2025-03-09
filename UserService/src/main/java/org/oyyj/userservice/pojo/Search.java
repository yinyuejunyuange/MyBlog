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
@TableName("search")
public class Search {
    @TableId("id")
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("content")
    private String content;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("lately_time")
    private Date latelyTime;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
    @TableField("is_user_delete")
    private Integer isUserDelete;
}
