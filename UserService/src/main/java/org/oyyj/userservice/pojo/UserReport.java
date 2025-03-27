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
@TableName("user_report")
public class UserReport {
    @TableId("id")
    private Long id;
    @TableField("user_report_id")
    private Long userReportId;
    @TableField("user_report_name")
    private String userReportName;
    @TableField("user_id")
    private Long userId;
    @TableField("user_name")
    private String userName;
    @TableField("content")
    private String content;
    @TableField("create_time")
    private Date createTime;
    @TableField("finish_time")
    private Date finishTime;
    @TableField("finish_id")
    private Long finishId;
    @TableField("finish_name")
    private String finishName;
    @TableField("status")
    private Integer status;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}
