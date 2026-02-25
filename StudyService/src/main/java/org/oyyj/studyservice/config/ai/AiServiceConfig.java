package org.oyyj.studyservice.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.studyservice.config.ai.agent.assistant.ChatAssistant;
import org.oyyj.studyservice.config.ai.agent.assistant.InterviewerAssistant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class AiServiceConfig {




    @Bean
    public InterviewerAssistant interviewerAssistant(
            StreamingChatLanguageModel streamingChatModel,        // 注入你的流式模型
            ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever) {


        return AiServices.builder(InterviewerAssistant.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Bean
    public ChatAssistant chatAssistant(
            StreamingChatLanguageModel streamingChatModel,        // 注入你的流式模型
            ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever) {

        return AiServices.builder(ChatAssistant.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();
    }

}
