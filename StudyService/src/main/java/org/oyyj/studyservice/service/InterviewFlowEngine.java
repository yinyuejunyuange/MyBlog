package org.oyyj.studyservice.service;

import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import net.bytebuddy.utility.nullability.AlwaysNull;
import org.oyyj.studyservice.config.ai.agent.assistant.InterviewerAssistant;
import org.oyyj.studyservice.mapper.ChatMessageMapper;
import org.oyyj.studyservice.pojo.ChatMessage;
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

    @Autowired
    private ChatMemoryStore chatMemoryStore;

    @Autowired
    private InterviewSessionService interviewSessionService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatMessageService chatMessageService;

    // 不再需要 EmbeddingStore 和 EmbeddingModel

    public void handleUserAnswer(
            InterviewSession session,
            String userSessionPrefix,
            Long baseId,
            Long userId,
            String userAnswer,
            StreamingResponseHandler<String> handler
    ) {
        KnowledgePoint kp = session.currentQuestion();

        KnowledgePoint nextQuestion = session.getNextQuestion();

        // 对用户的数据进行转义 避免出现 {{ 的格式让langchain判断出错
        userAnswer = userAnswer.replace("{{", "\\{{").replace("}}", "\\}}");

        // 1. 记录结构化日志（业务代码不变）
        InterviewLog log = new InterviewLog();
        log.setKnowledgePointId(kp.getId());
        log.setKnowledgePointTitle(kp.getTitle());
        log.setQuestionIndex(session.getCurrentQuestionIndex() + 1);
        log.setRoundIndex(session.getCurrentRound() + 1);
        log.setCandidateAnswer(userAnswer);
        session.getInterviewLogs().add(log);

        if(session.getSort() == null){
            session.setSort(0);
        }else{
            session.setSort(session.getSort() + 1);
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(String.valueOf(session.getId()));
        msg.setUserId(userId);
        msg.setKnowledgeBaseId(baseId);
        msg.setKnowledgePointId(kp.getId());
        msg.setRoundNum(session.getCurrentRound());
        msg.setRole("USER");
        msg.setKnowledgePointName(kp.getTitle());
        msg.setAnswerForId(session.getLastAssistantId());
        msg.setContent(userAnswer);
        msg.setSort(session.getSort());
        chatMessageMapper.insert(msg);

        chatMessageService.getUserMessageComment(msg.getId());

        // 2. 构造用户消息（包含当前状态）
        String userMessage = String.format("""
                当前知识点：第 %d / %d 题，第 %d / %d 轮
                知识点标题：%s
                候选人回答：%s
                下一给知识点：%s
                """,
                session.getCurrentQuestionIndex() + 1,
                session.getQuestions().size(),
                session.getCurrentRound() + 1,
                session.getMaxRound(),
                kp.getTitle(),
                userAnswer,
                nextQuestion!=null ? nextQuestion.getTitle() : "无"

        );

        // 3. 调用有状态助手（自动带上历史记忆，自动检索相关知识）
        interviewAssistant.interview(String.valueOf(session.getId()), userMessage)
                .onNext(handler::onNext)
                .onComplete(response -> {
                    String text = response.content().text();

                    session.setSort(session.getSort() + 1);

                    ChatMessage aiMsg = new ChatMessage();
                    aiMsg.setSessionId(String.valueOf(session.getId()));
                    aiMsg.setUserId(userId);
                    aiMsg.setKnowledgeBaseId(baseId);
                    aiMsg.setKnowledgePointId(kp.getId());
                    aiMsg.setRoundNum(session.getCurrentRound());
                    aiMsg.setRole("ASSISTANT");
                    aiMsg.setKnowledgePointName(kp.getTitle());
                    aiMsg.setContent(text);
                    aiMsg.setFinishComment(-1);// 不需要评论
                    aiMsg.setSort(session.getSort());
                    chatMessageMapper.insert(aiMsg);

                    session.setLastAssistantId(aiMsg.getId());
                    // 4. 流程判断（与之前相同）
                    if (text.contains("面试结束")) {
                        chatMemoryStore.deleteMessages(session.getId());
                        interviewSessionService.clear(userSessionPrefix);
                        handler.onComplete(Response.from(text));
                        return;
                    }

                    if (text.contains("进入下一知识点")) {
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
