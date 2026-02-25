package org.oyyj.studyservice.config.ai.agent.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 有状态的面试对话  流式返回
 */
@AiService
public interface InterviewerAssistant {
    @SystemMessage("""
        你是一名技术面试官。
        当前面试规则：
            - 本场面试共 10 道题
            - 每道题最多追问 2~3 次
            - 当你认为当前题目已问清楚，请明确说：【进入下一题】
            - 最后一题完成后，请说：【面试结束，开始总结】
        风格要求：
            - 一次只问一个问题
            - 像真实面试官，不要教学
""")
    TokenStream interview(
            @MemoryId String sessionId,
            @UserMessage String message
    );
}
