package org.oyyj.studyservice.dto.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.studyservice.pojo.Question;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class QuestionDTO {

    private Long id;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String questionType;
    private String questionText;
    private List<String> options;
    private List<String> answer;
    private Long knowledgePointId;
    private Integer isDelete;
    private String explanation;       // 题目讲解


    public BigDecimal getScore(List<String> userAnswer) {

        if(answer==null || answer.isEmpty()){
            log.error("正确答案缺失");
            return BigDecimal.ZERO;
        }

        if(questionType == null || questionType.isEmpty()){
            log.error("题目类别不可为空");
            return BigDecimal.ZERO;
        }
        if(userAnswer == null || userAnswer.isEmpty()){
            return BigDecimal.ZERO;
        }

        if (questionType.equals(Question.QuestionType.SINGLE.getValue()) || questionType.equals(Question.QuestionType.TRUE_FALSE.getValue())) {
            return userAnswer.equals(answer) ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        if (questionType.equals(Question.QuestionType.multiple.getValue())) {
            // 将正确答案和用户答案转为集合（已去重、排序后比较）
            Set<String> correctSet = new HashSet<>(answer);
            Set<String> userSet = new HashSet<>(userAnswer);
            correctSet.retainAll(userSet);

            return new BigDecimal(correctSet.size()).divide(new BigDecimal(answer.size()), 2, RoundingMode.HALF_UP);
        }
        log.error("题目类别错误");
        return BigDecimal.ZERO;

    }


}
