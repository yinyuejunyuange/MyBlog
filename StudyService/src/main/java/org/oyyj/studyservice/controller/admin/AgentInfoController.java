package org.oyyj.studyservice.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.ai.KnowledgeBaseInterviewStatsDTO;
import org.oyyj.studyservice.dto.ai.KnowledgePointInterviewStatsDTO;
import org.oyyj.studyservice.service.ChatMessageService;
import org.oyyj.studyservice.vo.chatMessage.ChatMessageVO;
import org.oyyj.studyservice.vo.chatMessage.InterviewEvaluationVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.AuthenticationException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/myBlog/agentInfo/admin")
@Slf4j
public class AgentInfoController {


    private final ChatMessageService chatMessageService;

    public AgentInfoController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * 分页查询所有模拟面试记录
     */
    @GetMapping("/listInterviewRecords")
    public ResultUtil<Page<ChatMessageVO>> listInterviewRecords(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime) throws AuthenticationException {


        return chatMessageService.listInterviewRecords(page, pageSize, knowledgeBaseId, userId, startTime, endTime);
    }

    /**
     * 其他地方调用 功能判断
     * @param type 评论类别
     * @param id
     * @param comment
     */
    @GetMapping("/isCommentToxic")
    public Boolean isCommentToxic(Integer type, Long id, String comment) {
        return chatMessageService.isCommentToxic(type, id, comment);
    }

    /**
     * 查看面试记录详情
     */
    @GetMapping("/getInterviewDetail")
    public ResultUtil<List<InterviewEvaluationVO>> getInterviewDetail(
            @RequestParam("sessionId") String sessionId) throws AuthenticationException {
        return chatMessageService.getInterviewDetail(sessionId);
    }

    /**
     * 统计每个知识点的提问次数以及用户的得分情况
     */
    @GetMapping("/getKnowledgePointStats")
    public ResultUtil<List<KnowledgePointInterviewStatsDTO>> getKnowledgePointStats(
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId) throws AuthenticationException {
        return chatMessageService.getKnowledgePointStats(knowledgeBaseId);
    }

    /**
     * 统计每个知识库的面试次数以及用户的得分情况
     */
    @GetMapping("/getKnowledgeBaseStats")
    public ResultUtil<List<KnowledgeBaseInterviewStatsDTO>> getKnowledgeBaseStats() throws AuthenticationException {

        return chatMessageService.getKnowledgeBaseStats();
    }
}
