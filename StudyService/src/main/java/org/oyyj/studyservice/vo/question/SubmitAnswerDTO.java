package org.oyyj.studyservice.vo.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitAnswerDTO {
    private Long knowledgeBaseId;               // 可选，知识库ID（记录用）
    private List<UserAnswerItem> answers;       // 用户答案列表

    @Data
    public static class UserAnswerItem {
        private Long questionId;
        private List<String> userAnswer;            // 用户答案，如 ["A"] 或 ["A","B"]
    }
}
