package org.oyyj.studyservice.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库面试统计信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseInterviewStatsDTO {

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;

    /**
     * 面试次数
     */
    private Long interviewCount;

    /**
     * 回答次数
     */
    private Long answerCount;

    /**
     * 平均得分
     */
    private Double avgScore;
}
