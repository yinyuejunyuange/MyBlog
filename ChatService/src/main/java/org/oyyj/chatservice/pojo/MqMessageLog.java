package org.oyyj.chatservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Date;


/**
 * MQ消息发送记录实体类
 * 对应数据库表 mq_message_log
 */
@Data
@Accessors(chain = true)
@TableName("mq_message_log")
public class MqMessageLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;                     // 自增主键

    @TableField("message_id")
    private String messageId;             // 消息全局ID（可与业务msg_id关联）

    private String exchange;              // 交换机名称

    @TableField("routing_key")
    private String routingKey;            // 路由键（目标实例ID）

    private String queue;                 // 目标队列名称（可选）

    private String content;               // 消息内容（JSON）

    private Integer status;               // 状态：0-待发送，1-已发送，2-已到达，3-消费成功，4-消费失败，5-发送失败

    @TableField("retry_count")
    private Integer retryCount;           // 重试次数

    @TableField("error_msg")
    private String errorMsg;              // 错误信息

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;       // 创建时间

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;       // 更新时间

    /**
     * 逻辑删除字段
     * 0-未删除，1-已删除
     * 使用 @TableLogic 注解，MyBatis-Plus 会自动在查询时追加条件 is_delete=0
     */
    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;              // 是否删除：0-未删除，1-已删除


    @Getter
    public enum MqMessageLogStatus {
        WAITING(0,"待发送"),
        SEND(1,"已发送"),
        READY(2,"已到达"),
        FINISH(3,"消费成功"),
        FAIL(4,"消费失败"),
        SEND_FAIL(5,"发送失败");

        private final Integer code;
        private final String msg;
        MqMessageLogStatus(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static MqMessageLogStatus getByCode(Integer code){
            for (MqMessageLogStatus status : MqMessageLogStatus.values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return null;

        }

    }

}
