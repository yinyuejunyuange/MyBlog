package org.oyyj.studyservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.studyservice.dto.ai.KnowledgeBaseInterviewStatsDTO;
import org.oyyj.studyservice.dto.ai.KnowledgePointInterviewStatsDTO;
import org.oyyj.studyservice.pojo.ChatMessage;
import org.oyyj.studyservice.vo.chatMessage.ChatMessageVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    List<ChatMessageVO> listUserHistory(@Param("userId") Long userId);

    /**
     * 分页查询面试记录
     */
    Page<ChatMessageVO> listInterviewRecords(
            Page<ChatMessageVO> page,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("userId") Long userId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    /**
     * 统计知识点的提问次数和得分情况
     */
    List<KnowledgePointInterviewStatsDTO> getKnowledgePointStats(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
     * 统计知识库的面试次数和得分情况
     */
    List<KnowledgeBaseInterviewStatsDTO> getKnowledgeBaseStats();

}
