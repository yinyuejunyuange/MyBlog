package org.oyyj.mycommon.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RabbitMQ业务数据载荷
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RabbitMqMessage implements Serializable {

    private String payLoad; // 载荷

    private Integer retryCount; // 重试次数

    private String messageId; // 唯一ID

}
