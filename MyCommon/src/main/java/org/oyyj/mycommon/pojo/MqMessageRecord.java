package org.oyyj.mycommon.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MQ消息记录表 每个服务都会有所以卸载common包中 被多个服务调用
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mq_message_record")
public class MqMessageRecord {
    @TableId("id")
    private Long id;
    @TableField("msg_id")
    private String msgId;
    @TableField("target_queue")
    private String targetQueue;
    @TableField("exec_status")
    private Integer execStatus;
    @TableField("msg_content")
    private String msgContent;
    @TableField("retry_count")
    private Integer retryCount;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

}
