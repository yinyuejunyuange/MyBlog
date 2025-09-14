package org.oyyj.blogservice.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RabbitMqMessage implements Serializable {
    private String key; // 键
    private Long version; // 版本
    private String messageId; // 消息唯一ID避免重复消费

    public RabbitMqMessage(String key, Long version) {
        this.key = key;
        this.version = version;
        messageId = UUID.randomUUID().toString();
    }

    public String toString() {
        return key + ":" + version + ":" + messageId;
    }

}
