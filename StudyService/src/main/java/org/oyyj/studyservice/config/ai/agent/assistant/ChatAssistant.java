package org.oyyj.studyservice.config.ai.agent.assistant;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {

    @SystemMessage("""
        你是一个技术助理。
        回答必须优先基于已知知识点，
        如果不足可以合理补充，但要标明推断。
        """)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
