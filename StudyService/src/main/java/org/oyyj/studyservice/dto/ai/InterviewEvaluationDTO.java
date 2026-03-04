package org.oyyj.studyservice.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewEvaluationDTO {
    private int accuracy;      // 准确性评分
    private int logic;         // 逻辑评分
    private int depth;         // 深度评分
    private String suggestion;  // 改进建议（评价）
    private int score;          // 总体评分
}
