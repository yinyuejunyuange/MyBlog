package org.oyyj.mycommon.config.pojo;

import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.UUID;

/**
 * 增强 MQ的消息传递的类
 */
public class EnhanceCorrelationData extends CorrelationData {
    private final String body;
    private final Integer retryCount;

    private final Boolean needCleanRedis;

    private final String needCleanBlogId;

    public EnhanceCorrelationData(String id, String body , Integer retryCount, Boolean needCleanRedis, String needCleanBlogId) {
        super(id+"_"+ UUID.randomUUID());
        this.body = body;
        this.retryCount = retryCount;
        this.needCleanRedis = needCleanRedis;
        this.needCleanBlogId = needCleanBlogId;
    }

    public String getBody() {
        return body;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Boolean getNeedCleanRedis() {
        return needCleanRedis;
    }
    public String getNeedCleanBlogId() {
        return needCleanBlogId;
    }
}
