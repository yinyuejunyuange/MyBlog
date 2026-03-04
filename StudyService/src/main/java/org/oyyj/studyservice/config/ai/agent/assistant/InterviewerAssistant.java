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
            - 本场面试共 10 个知识点
            - 需要按照知识点进行提问
            - 每道题最多追问 4 次
            - 每次只能返回一个问题
            - 在用户回答后，你认为当前知识点已问清楚或者已经追问足够次数，请明确说：【进入下一知识点】  并提出下一个知识点的问题
            - 最后一题完成后，请再用户回答后说：【面试结束】
        风格要求：
            - 像真实面试官，不要教学
        结果要求：
            - 需要返回MarkDown格式的结果
""")
    TokenStream interview(
            @MemoryId String sessionId,
            @UserMessage String message
    );
}
