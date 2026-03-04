package org.oyyj.studyservice.config.ai;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluatorServiceConfig {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String apiUrl;

    @Value("${ai.deepseek.model}")
    private String apiModel;


    @Bean
    public OpenAiChatModel evaluatorChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(apiUrl)
                .modelName(apiModel)
                .temperature(0.7)
                .build();
    }

}
