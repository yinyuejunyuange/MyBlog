package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.ai.InterviewEvaluationDTO;
import org.oyyj.studyservice.dto.ai.KnowledgeBaseInterviewStatsDTO;
import org.oyyj.studyservice.dto.ai.KnowledgePointInterviewStatsDTO;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.vo.chatMessage.ChatMessageVO;
import org.oyyj.studyservice.vo.chatMessage.InterviewEvaluationVO;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface ChatMessageService extends IService<ChatMessage> {
    /**
     * 异步执行 得到用户回答信息的评价
     * @param messageId
     */
    void getUserMessageComment(Long messageId);


    ResultUtil<List<ChatMessageVO>> listUserHistory(LoginUser loginUser);

    ResultUtil<List<InterviewEvaluationVO>> listBySessionId(String sessionId);

    /**
     * 分页查询所有模拟面试记录
     * @param page 页码
     * @param pageSize 每页大小
     * @param knowledgeBaseId 知识库ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 面试记录列表
     */
    ResultUtil<Page<ChatMessageVO>> listInterviewRecords(
            Integer page, Integer pageSize, Long knowledgeBaseId, Long userId, Date startTime, Date endTime);

    /**
     * 查看面试记录详情
     * @param sessionId 会话ID
     * @return 面试记录详情
     */
    ResultUtil<List<InterviewEvaluationVO>> getInterviewDetail(String sessionId);

    /**
     * 统计每个知识点的提问次数以及用户的得分情况
     * @param knowledgeBaseId 知识库 ID（可选）
     * @return 统计结果
     */
    ResultUtil<List<KnowledgePointInterviewStatsDTO>> getKnowledgePointStats(Long knowledgeBaseId);

    /**
     * 统计每个知识库的面试次数以及用户的得分情况
     * @return 统计结果
     */
    ResultUtil<List<KnowledgeBaseInterviewStatsDTO>> getKnowledgeBaseStats();


    /**
     * 对评论进行攻击性判断
     * @param type
     * @param id
     * @param comment
     */
    Boolean isCommentToxic(Integer type,Long id,String comment);

}
