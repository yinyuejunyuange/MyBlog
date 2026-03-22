package org.oyyj.studyservice.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点面试统计信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgePointInterviewStatsDTO {

    /**
     * 知识点ID
     */
    private Long knowledgePointId;

    /**
     * 知识点名称
     */
    private String knowledgePointName;

    /**
     * 面试次数
     */
    private Long interviewCount;

    /**
     * 提问次数
     */
    private Long questionCount;

    /**
     * 平均得分
     */
    private Double avgScore;
}
