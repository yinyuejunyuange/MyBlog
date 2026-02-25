package org.oyyj.studyservice.pojo.model.interview;

import lombok.Data;
import org.oyyj.studyservice.pojo.KnowledgePoint;

import java.util.ArrayList;
import java.util.List;
@Data
public class InterviewSession {

    private Long id;
    private List<KnowledgePoint> questions;

    private int currentQuestionIndex = 0;
    private int currentRound = 0;
    private int maxRound = 3;

    private List<InterviewLog> interviewLogs = new ArrayList<>();

    public KnowledgePoint currentQuestion() {
        return questions.get(currentQuestionIndex);
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
