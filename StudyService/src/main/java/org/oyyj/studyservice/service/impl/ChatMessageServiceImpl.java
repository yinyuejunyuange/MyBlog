package org.oyyj.studyservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.oyyj.studyservice.dto.ai.InterviewEvaluationDTO;
import org.oyyj.studyservice.mapper.ChatMessageMapper;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired
    private OpenAiChatModel evaluatorChatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    @Override
    public void getUserMessageComment(Long messageId) {
        // 提示词要求模型返回评分和评价

        ChatMessage byId = getById(messageId);
        if(byId==null
                || byId.getAnswerForId() == null
                || byId.getContent() == null || byId.getContent().contains("请再次告诉我题目是什么")){
            return;
        }

        ChatMessage questionMsg = getById(byId.getAnswerForId());
        String question =  questionMsg.getContent();
        String answer = byId.getContent();

        String prompt = PromptTemplate.from(
                """
                你是一个专业的面试官，请对候选人的回答进行评价。
                面试题：{{question}}
                候选人回答：{{answer}}

                请从准确性、逻辑性、深度三个方面分别评分（1-10分），并给出改进建议（评价），最后给出总体评分（1-10分）。
                以 JSON 格式返回，包含以下字段：
                {
                    "accuracy": 整数,
                    "logic": 整数,
                    "depth": 整数,
                    "suggestion": "改进建议文本（评价）",
                    "score": 整数
                }
                只返回 JSON，不要包含其他内容。
                """
        ).apply(Map.of("question", question, "answer", answer)).text();

        String response = evaluatorChatModel.generate(prompt);
        try {
            response = response.replaceAll("^```json\\s*|^```\\s*|\\s*```$", "").trim();
            // 验证是否结构正确
            InterviewEvaluationDTO interviewEvaluationDTO = objectMapper.readValue(response, InterviewEvaluationDTO.class);

            String comment = objectMapper.writeValueAsString(interviewEvaluationDTO);

            boolean update = update(Wrappers.<ChatMessage>lambdaUpdate()
                    .eq(ChatMessage::getId, messageId)
                    .set(ChatMessage::getComment, comment)
            );
        } catch (Exception e) {
            throw new RuntimeException("解析失败，原始输出：" + response, e);
        }

    }
}
