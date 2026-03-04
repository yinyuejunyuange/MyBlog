package org.oyyj.studyservice.config.ai;

import cn.hutool.json.JSONUtil;
import com.alibaba.nacos.shaded.io.grpc.internal.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.studyservice.dto.ai.RedisChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用redis存储 用户的对话memory信息
 */
@Component
@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {
    private static final String KEY_PREFIX = RedisPrefix.AI_MEMORY;

    @Autowired
    private RedisUtil redisUtil;

    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        String json = redisUtil.getString(key);

        if (json == null) {
            return new ArrayList<>();
        }

        List<RedisChatMessageDTO> list = JSONUtil.toList(json, RedisChatMessageDTO.class);
        return  list.stream().map(this::fromDto).toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = KEY_PREFIX + memoryId;

        List<RedisChatMessageDTO> dtoList = messages.stream()
                .map(this::toDto)
                .toList();

        try {
            redisUtil.set(
                    key,
                    mapper.writeValueAsString(dtoList),
                    120L // 过期时间
            );
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisUtil.delete(KEY_PREFIX + memoryId);
    }


    private RedisChatMessageDTO toDto(ChatMessage message) {

        if (message instanceof SystemMessage sm) {
            return new RedisChatMessageDTO("system", sm.text());
        }

        if (message instanceof UserMessage um) {
            return new RedisChatMessageDTO("user", um.singleText());
        }

        if (message instanceof AiMessage am) {
            return new RedisChatMessageDTO("ai", am.text());
        }

        throw new IllegalArgumentException("Unknown message type");
    }

    private ChatMessage fromDto(RedisChatMessageDTO dto) {

        return switch (dto.getRole()) {
            case "system" -> SystemMessage.from(dto.getContent());
            case "user"   -> UserMessage.from(dto.getContent());
            case "ai"     -> AiMessage.from(dto.getContent());
            default -> throw new IllegalArgumentException("Unknown type");
        };
    }
}
