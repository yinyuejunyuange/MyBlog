package org.oyyj.studyservice.service;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.oyyj.mycommonbase.common.RedisPrefix;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgeBaseRelationDTO;
import org.oyyj.studyservice.mapper.ChatMessageMapper;
import org.oyyj.studyservice.mapper.KnowledgeBaseMapper;
import org.oyyj.studyservice.mapper.KnowledgePointMapper;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.pojo.KnowledgePoint;
import org.oyyj.studyservice.pojo.model.interview.InterviewSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class InterviewSessionService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private Snowflake snowflake;

    @Autowired
    private ChatMemoryStore chatMemoryStore;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterviewSession createSession(LoginUser loginUser, String knowledgeBaseId) {

        List<KnowledgeBaseRelationDTO> knowledgeBaseRelationDTOS = knowledgePointMapper.selectKnowledgeBaseRelationByKnowledgeBaseId(Long.parseLong(knowledgeBaseId));
        List<Long> pointIds = knowledgeBaseRelationDTOS.stream().map(KnowledgeBaseRelationDTO::getKnowledgePointId).toList();
        if(pointIds.isEmpty()){
            throw new RuntimeException("知识点不可为空");
        }
        List<KnowledgePoint> all = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
                .in(KnowledgePoint::getId, pointIds)
        );

        Collections.shuffle(all);  // 随机打乱

        InterviewSession session = new InterviewSession();
        List<KnowledgePoint> list = all.stream().limit(10).toList();
        session.setQuestions(list);
        session.setId(snowflake.nextId());
        session.setCurrentQuestionIndex(0);
        session.setCurrentRound(0);
        session.setMaxRound(3);
        return session;
    }

    /**
     * 在redis中记录信息
     * @param candidateId
     * @param session
     */
    public void save(String candidateId, InterviewSession session) {
        try {
            redisUtil.set(RedisPrefix.AI_INTERVIEW_PREFIX + candidateId, objectMapper.writeValueAsString(session));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 恢复信息
     * @param candidateId
     * @return
     */
    public InterviewSession load(String candidateId) {
        Object o = redisUtil.get(RedisPrefix.AI_INTERVIEW_PREFIX + candidateId);
        if(o==null){
            return null;
        }
        String json = o.toString();
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, InterviewSession.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除信息
     * @param candidateId
     */
    public void clear(String candidateId) {

        InterviewSession load = load(candidateId);
        if (load == null) return;

        redisUtil.delete(RedisPrefix.AI_INTERVIEW_PREFIX+ candidateId);
        // 清空记忆信息
        chatMemoryStore.deleteMessages(candidateId);

        // 设置消息对话完成
        chatMessageMapper.update(Wrappers.<ChatMessage>lambdaUpdate()
                .eq(ChatMessage::getSessionId, load.getId())
                .set(ChatMessage::getIsFinish, YesOrNoEnum.YES.getCode())
        );
    }

}
