package org.oyyj.blogservice.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;


public class EnhancedCorrelationData extends CorrelationData {
    private final String body;

    public EnhancedCorrelationData(String id, String body) {
        super(id);
        this.body = body;
    }

    public String getBody() {
        return body;
    }
}


