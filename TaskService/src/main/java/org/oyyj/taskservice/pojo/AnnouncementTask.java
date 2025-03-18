package org.oyyj.taskservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("announcement_task")
public class AnnouncementTask {
    @TableId("id")
    private Long id;
    @TableField("announcement_id")
    private Long announcementId;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("frequency")
    private String frequency;
    @TableField("status")
    private Integer status;
    @TableField("is_delete")
    private Integer isDelete;
}
