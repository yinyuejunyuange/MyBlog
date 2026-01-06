package org.oyyj.mycommon.config.pojo;

import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.UUID;

/**
 * 增强 MQ的消息传递的类
 */
public class EnhanceCorrelationData extends CorrelationData {
    private final String body;
    private final Integer retryCount;

    public EnhanceCorrelationData(String id, String body , Integer retryCount) {
        super(id+"_"+ UUID.randomUUID());
        this.body = body;
        this.retryCount = retryCount;
    }

    public String getBody() {
        return body;
    }

    public Integer getRetryCount() {
        return retryCount;
    }
}
