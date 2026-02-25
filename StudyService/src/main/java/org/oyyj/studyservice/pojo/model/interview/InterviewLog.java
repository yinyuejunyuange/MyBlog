package org.oyyj.studyservice.pojo.model.interview;

import lombok.Data;

@Data
public class InterviewLog {


    private Long knowledgePointId;
    private String knowledgePointTitle;

    private int questionIndex;   // 第几题（0-based or 1-based 你定）
    private int roundIndex;      // 第几轮

    private String interviewerQuestion;
    private String candidateAnswer;
}
