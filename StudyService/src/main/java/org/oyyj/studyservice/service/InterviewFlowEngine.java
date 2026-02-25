package org.oyyj.studyservice.service;

import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.oyyj.studyservice.config.ai.agent.assistant.InterviewerAssistant;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.pojo.model.interview.InterviewLog;
import org.oyyj.studyservice.pojo.model.interview.InterviewSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 模拟面试时处理的逻辑引擎
 */
@Service
public class InterviewFlowEngine {

    @Autowired
    private InterviewerAssistant interviewAssistant;  // 注入有状态助手

    // 不再需要 EmbeddingStore 和 EmbeddingModel

    public void handleUserAnswer(
            InterviewSession session,
            String userAnswer,
            StreamingResponseHandler<String> handler
    ) {
        KnowledgePoint kp = session.currentQuestion();

        // 1. 记录结构化日志（业务代码不变）
        InterviewLog log = new InterviewLog();
        log.setKnowledgePointId(kp.getId());
        log.setKnowledgePointTitle(kp.getTitle());
        log.setQuestionIndex(session.getCurrentQuestionIndex() + 1);
        log.setRoundIndex(session.getCurrentRound() + 1);
        log.setCandidateAnswer(userAnswer);
        session.getInterviewLogs().add(log);
        // TODO 存储到数据库

        // 2. 构造用户消息（包含当前状态）
        String userMessage = String.format("""
                当前题目：第 %d / %d 题，第 %d / %d 轮
                知识点标题：%s
                候选人回答：%s
                """,
                session.getCurrentQuestionIndex() + 1,
                session.getQuestions().size(),
                session.getCurrentRound() + 1,
                session.getMaxRound(),
                kp.getTitle(),
                userAnswer
        );

        // 3. 调用有状态助手（自动带上历史记忆，自动检索相关知识）
        interviewAssistant.interview(String.valueOf(session.getId()), userMessage)
                .onNext(handler::onNext)
                .onComplete(response -> {
                    String text = response.content().text();

                    // 4. 流程判断（与之前相同）
                    if (text.contains("面试结束")) {
                        handler.onComplete(Response.from(text));
                        return;
                    }

                    if (text.contains("进入下一题")) {
                        if (session.hasNextQuestion()) {
                            session.nextQuestion();
                        }
                    } else {
                        if (session.hasNextRound()) {
                            session.nextRound();
                        } else if (session.hasNextQuestion()) {
                            session.nextQuestion();
                        }
                    }

                    handler.onComplete(Response.from(text));
                })
                .onError(handler::onError)
                .start();
    }
}
