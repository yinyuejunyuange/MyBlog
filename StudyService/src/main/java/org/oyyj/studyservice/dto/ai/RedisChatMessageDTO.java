package org.oyyj.studyservice.dto.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 转换Langchain中的数据  让其可以序列化存入数据库中
 */
@Data
@AllArgsConstructor
public class RedisChatMessageDTO {
    private String role;    // system / user / ai
    private String content;

}
