package org.oyyj.studyservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommon.pojo.dto.comment.CommentToxicDTO;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.ai.InterviewEvaluationDTO;
import org.oyyj.studyservice.dto.ai.KnowledgeBaseInterviewStatsDTO;
import org.oyyj.studyservice.dto.ai.KnowledgePointInterviewStatsDTO;
import org.oyyj.studyservice.feign.BlogFeign;
import org.oyyj.studyservice.mapper.ChatMessageMapper;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.service.ChatMessageService;
import org.oyyj.studyservice.vo.chatMessage.ChatMessageVO;
import org.oyyj.studyservice.vo.chatMessage.InterviewEvaluationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired
    private OpenAiChatModel evaluatorChatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private BlogFeign blogFeign;

    @Async
    @Override
    public void getUserMessageComment(Long messageId) {
        // 提示词要求模型返回评分和评价

        ChatMessage byId = getById(messageId);
        if(byId==null
                || byId.getAnswerForId() == null
                || byId.getContent() == null || byId.getContent().contains("题目是什么")){

            boolean update = update(Wrappers.<ChatMessage>lambdaUpdate()
                    .eq(ChatMessage::getId, messageId)
                    .set(ChatMessage::getFinishComment,-1)
            );

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
                    "advantages": "用户回答的优点文本",
                    "disAdvantages": "用户回答的缺点文本",
                    "suggestion": "改进建议文本",
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
                    .set(ChatMessage::getFinishComment,1)
            );
        } catch (Exception e) {
            throw new RuntimeException("解析失败，原始输出：" + response, e);
        }

    }

    @Override
    public ResultUtil<List<ChatMessageVO>> listUserHistory(LoginUser loginUser) {
        return ResultUtil.success(baseMapper.listUserHistory(loginUser.getUserId()));
    }

    @Override
    public ResultUtil<List<InterviewEvaluationVO>> listBySessionId(String sessionId) {

        List<ChatMessage> list = list(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .ge(ChatMessage::getSort , YesOrNoEnum.YES.getCode())
                .notLike(ChatMessage::getContent,"题目是什么")
        );

        List<InterviewEvaluationVO> results = new ArrayList<>();

        Map<Long, ChatMessage> msgMap = list.stream().collect(Collectors.toMap(ChatMessage::getId, Function.identity()));
        List<ChatMessage> userMsg = list.stream().filter(item -> item.getRole().equals("USER")).toList();

        for (ChatMessage chatMessage : userMsg) {
            InterviewEvaluationVO interviewEvaluationVO = new InterviewEvaluationVO();

            interviewEvaluationVO.setSort(chatMessage.getSort());
            interviewEvaluationVO.setId(String.valueOf(chatMessage.getId()));
            interviewEvaluationVO.setUserAnswer(chatMessage.getContent());

            if(msgMap.containsKey(chatMessage.getAnswerForId())) {
                ChatMessage questionMsg = msgMap.get(chatMessage.getAnswerForId());
                interviewEvaluationVO.setQuestion(questionMsg.getContent());
            }

            if(chatMessage.getComment() != null){
                InterviewEvaluationDTO interviewEvaluationDTO = null;
                try {
                    interviewEvaluationDTO = objectMapper.readValue(chatMessage.getComment(), InterviewEvaluationDTO.class);
                } catch (JsonProcessingException e) {
                    log.error("后端数据解析失败！{}", chatMessage.getComment());
                    continue;
                }
                interviewEvaluationVO.setInterviewEvaluationDTO(interviewEvaluationDTO);
            }

            results.add(interviewEvaluationVO);

        }

        results.sort(Comparator.comparing(InterviewEvaluationVO::getSort,
                Comparator.nullsLast(Integer::compareTo)));

        return  ResultUtil.success(results);

    }

    @Override
    public ResultUtil<Page<ChatMessageVO>> listInterviewRecords(Integer page, Integer pageSize, Long knowledgeBaseId, Long userId, Date startTime, Date endTime) {

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        Page<ChatMessageVO> pageObj = new Page<>(page, pageSize);

        Page<ChatMessageVO> result = baseMapper.listInterviewRecords(pageObj, knowledgeBaseId, userId, startTime, endTime);

        return ResultUtil.success(result);
    }

    @Override
    public ResultUtil<List<InterviewEvaluationVO>> getInterviewDetail(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return ResultUtil.fail("会话 ID 不能为空");
        }

        return listBySessionId(sessionId);
    }

    @Override
    public ResultUtil<List<KnowledgePointInterviewStatsDTO>> getKnowledgePointStats(Long knowledgeBaseId) {
        List<KnowledgePointInterviewStatsDTO> stats = baseMapper.getKnowledgePointStats(knowledgeBaseId);
        return ResultUtil.success(stats);
    }

    @Override
    public ResultUtil<List<KnowledgeBaseInterviewStatsDTO>> getKnowledgeBaseStats() {
        List<KnowledgeBaseInterviewStatsDTO> stats = baseMapper.getKnowledgeBaseStats();
        return ResultUtil.success(stats);
    }

    /**
     * 判断当前评论是否具有攻击性
     * @param type
     * @param id
     * @param comment
     */
    @Override
    public Boolean isCommentToxic(Integer type, Long id, String comment) {
        if(comment==null || comment.isEmpty()){
            log.error("评论为空无法判断攻击性，评论ID：{}",id);
            return false;
        }

        String prompt = PromptTemplate.from(
                """
                        你是一个后台管理人员，需要分析用户评论是否具有攻击性。
                                          评论内容：{{comment}}
                        
                                          请从以下两个维度进行分析：
                                          1. 攻击性等级（isToxic）：
                                             - 0：没有攻击性，评论内容友好或中立。
                                             - 1：具有冒犯性，包含不尊重、贬低或轻蔑的言辞，但未达到严重攻击的程度。
                                             - 2：具有攻击性，包含明显的恶意、辱骂、威胁或煽动性言论。
                        
                                          2. 攻击对象（topicList）：如果评论具有攻击性（等级为1或2），请识别攻击的目标，可以是以下一个或多个类别（用英文逗号分隔）：
                                             - 人身攻击：针对个人的外貌、能力、品德等。
                                             - 地域歧视：针对特定地区或籍贯的贬低。
                                             - 种族歧视：针对特定种族或民族的偏见。
                                             - 其他：上述未涵盖的攻击对象。
                        
                                          请以JSON格式返回，包含两个字段：
                                          {
                                              "isToxic": 整数,
                                              "topicList": "字符串，多个类别用英文逗号分隔，若无攻击对象则返回空字符串"
                                          }
                        
                                          只返回JSON，不要包含任何解释或额外内容
                """
        ).apply(Map.of("comment", comment)).text();

        String response = evaluatorChatModel.generate(prompt);
        try {
            response = response.replaceAll("^```json\\s*|^```\\s*|\\s*```$", "").trim();
            // 验证是否结构正确
            CommentToxicDTO commentToxicDTO = objectMapper.readValue(response, CommentToxicDTO.class);
            commentToxicDTO.setComment(comment);
            commentToxicDTO.setType(type);
            commentToxicDTO.setId(id);
            blogFeign.toxicJudgement(commentToxicDTO);
        } catch (Exception e) {
            throw new RuntimeException("解析失败，原始输出：" + response, e);
        }
        return true;
    }
}
