package org.oyyj.studyservice.controller;

import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.config.ai.agent.assistant.InterviewerAssistant;
import org.oyyj.studyservice.config.ai.agent.assistant.ChatAssistant;
import org.oyyj.studyservice.dto.ai.AiChatDTO;
import org.oyyj.studyservice.pojo.model.interview.InterviewSession;
import org.oyyj.studyservice.service.InterviewFlowEngine;
import org.oyyj.studyservice.service.InterviewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
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
     * @param aiChatDTO
     * @return
     */
    @PostMapping(value = "/sseInterview", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 流式数据
    public SseEmitter sseInterview(
            @RequestUser LoginUser loginUser,
            @RequestBody AiChatDTO aiChatDTO) {

        SseEmitter emitter = new SseEmitter(0L);

        String redisSession = RedisPrefix.AI_INTERVIEW_PREFIX + loginUser.getUserId()+":" + aiChatDTO.getKnowledgeBaseId();

        InterviewSession session = interviewSessionService.load(redisSession);
        if(session == null){
            session = interviewSessionService.createSession(loginUser, aiChatDTO.getKnowledgeBaseId());
            interviewSessionService.save(redisSession,session);

            aiChatDTO.setMessage("请告诉我题目。");

//            String firstQuestion = """
//                    面试开始。
//                    第一题: %s
//                    """
//                    .formatted(session.getQuestions().get(session.getCurrentQuestionIndex()).getTitle());
//            try {
//                emitter.send(firstQuestion);
//            } catch (IOException e) {
//                emitter.completeWithError(e);
//            }
//            return emitter;
        }


        if(aiChatDTO.getMessage()==null || aiChatDTO.getMessage().isEmpty()){
            aiChatDTO.setMessage("请再次告诉我题目是什么");
        }

        if(aiChatDTO.getMessage().length() > 5000){
            try {
                emitter.send("内容过长");
                return  emitter;
            } catch (IOException e) {
                log.error("内容过长发送信息报错 ：{}",e.getMessage());
                throw new RuntimeException(e);
            }
        }

        InterviewSession finalSession = session;
        interviewFlowEngine.handleUserAnswer(
                session,
                redisSession,
                Long.valueOf(aiChatDTO.getKnowledgeBaseId()),
                loginUser.getUserId(),
                aiChatDTO.getMessage(),
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

    @PostMapping(value = "/sseChat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseChat(
            @RequestUser LoginUser loginUser,
            @RequestBody AiChatDTO aiChatDTO) {

        SseEmitter emitter = new SseEmitter(0L);
        String memoryId = String.valueOf(loginUser.getUserId());  // 每个用户独立记忆

        String message = aiChatDTO.getMessage();
        message = message.replace("{{", "\\{{").replace("}}", "\\}}");
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

    @PutMapping("/interview/finish")
    public ResultUtil<String> finishInterview(@RequestParam("knowledgeBaseId") String knowledgeBaseId,
                                              @RequestUser LoginUser loginUser){
        String redisSession = RedisPrefix.AI_INTERVIEW_PREFIX + loginUser.getUserId()+":" +knowledgeBaseId;
        interviewSessionService.clear(redisSession);
        return ResultUtil.success("删除成功");

    }
}
