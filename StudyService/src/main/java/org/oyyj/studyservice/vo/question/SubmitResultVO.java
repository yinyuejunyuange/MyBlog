package org.oyyj.studyservice.vo.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitResultVO {
    private Long recordId;                       // 答题批次ID
    private BigDecimal totalScore;                // 实际总分（如 3.5）
    private List<QuestionResultVO> details;       // 每题详情

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuestionResultVO {
        private Long questionId;
        private Boolean isCorrect;                    // 是否正确
        private BigDecimal score;                      // 该题实际得分
    }


}
