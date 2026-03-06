package org.oyyj.studyservice.pojo.model.interview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.oyyj.studyservice.pojo.KnowledgePoint;

import java.util.ArrayList;
import java.util.List;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewSession {

    private Long id;
    private List<KnowledgePoint> questions;

    private int currentQuestionIndex = 0;
    private int currentRound = 0;
    private int maxRound = 3;
    /**
     * 上一个问题的ID
     */
    private Long lastAssistantId;

    private Integer sort;

    private List<InterviewLog> interviewLogs = new ArrayList<>();

    public KnowledgePoint currentQuestion() {
        return questions.get(currentQuestionIndex);
    }

    public KnowledgePoint getNextQuestion() {
        if(hasNextQuestion()){
            return questions.get(currentQuestionIndex+1);
        }
        return null;
    }

    public boolean hasNextRound() {
        return currentRound + 1 < maxRound;
    }

    public boolean hasNextQuestion() {
        return currentQuestionIndex + 1 < questions.size();
    }

    public void nextRound() {
        currentRound++;
    }

    public void nextQuestion() {
        currentQuestionIndex++;
        currentRound = 0;
    }
}
