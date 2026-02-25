package org.oyyj.studyservice.dto.knowledgePoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点的面试问题
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewQuestionsDTO {

    private String title;

    private String answer;

}
