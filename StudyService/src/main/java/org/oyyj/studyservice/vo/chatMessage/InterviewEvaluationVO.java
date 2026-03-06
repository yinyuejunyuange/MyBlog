package org.oyyj.studyservice.vo.chatMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.studyservice.dto.ai.InterviewEvaluationDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewEvaluationVO {

    private String id;

    private String question;

    private Integer sort;

    private String userAnswer;

    private InterviewEvaluationDTO interviewEvaluationDTO;
}
