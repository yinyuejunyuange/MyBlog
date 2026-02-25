package org.oyyj.studyservice.controller;

import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.studyservice.config.ai.agent.assistant.InterviewerAssistant;
import org.oyyj.studyservice.config.ai.agent.assistant.ChatAssistant;
import org.oyyj.studyservice.pojo.model.interview.InterviewSession;
import org.oyyj.studyservice.service.InterviewFlowEngine;
import org.oyyj.studyservice.service.InterviewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/myBlog/agent")
public class AgentController {


    @Autowired
    private ChatAssistant chatAssistant;


    @Autowired
    private InterviewSessionService interviewSessionService;

    @Autowired
    private InterviewFlowEngine  interviewFlowEngine;

    /**
     * 基于知识库模拟你面试
     * @param loginUser
     * @param knowledgeBaseId
     * @param message
     * @return
     */
    @GetMapping(value = "/sseInterview", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 流式数据
    public SseEmitter sseInterview(
            @RequestUser LoginUser loginUser,
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @RequestParam("message") String message) {

        SseEmitter emitter = new SseEmitter(0L);

        String redisSession = RedisPrefix.AI_INTERVIEW_PREFIX + loginUser.getUserId() + knowledgeBaseId;

        InterviewSession session = interviewSessionService.load(redisSession);
        if(session == null){
            session = interviewSessionService.createSession(loginUser, knowledgeBaseId);
            interviewSessionService.save(redisSession,session);
        }

        if(YesOrNoEnum.NO.getCode().equals(session.getCurrentQuestionIndex())){
            String firstQuestion = """
                    面试开始。
                    第一题: %s
                    """
                    .formatted(session.getQuestions().get(session.getCurrentQuestionIndex()).getTitle());
            try {
                emitter.send(firstQuestion);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        if(message==null || message.isEmpty()){
            try {
                emitter.send("请输入回答");
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        InterviewSession finalSession = session;
        interviewFlowEngine.handleUserAnswer(
                session,
                message,
                new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            emitter.send(token);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                    @Override
                    public void onComplete(Response<String> response) {
                        // 每轮结束都需要保存session
                        interviewSessionService.save(redisSession, finalSession);
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        emitter.completeWithError(throwable);
                    }
                }
        );


        return emitter;
    }

    @GetMapping(value = "/sseChat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseChat(
            @RequestUser LoginUser loginUser,   // 假设已存在获取当前用户的注解
            @RequestParam("message") String message) {

        SseEmitter emitter = new SseEmitter(0L);
        String memoryId = String.valueOf(loginUser.getUserId());  // 每个用户独立记忆

        chatAssistant.chat(memoryId, message)
                .onNext(token -> {
                    try {
                        emitter.send(token);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> emitter.complete())
                .onError(emitter::completeWithError)
                .start();

        return emitter;
    }
}
