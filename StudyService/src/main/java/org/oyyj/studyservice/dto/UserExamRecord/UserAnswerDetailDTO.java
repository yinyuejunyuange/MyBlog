package org.oyyj.studyservice.dto.UserExamRecord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAnswerDetailDTO {

    private Long id;

    private String createBy;

    private Date createTime;

    private String updateBy;

    private Date updateTime;

    private Integer isDelete;     // 逻辑删除：1-已删除，0-未删除

    private Long recordId;        // 所属答题批次ID

    private Long questionId;      // 试题ID

    private String userAnswer;    // 用户提交的答案
    private Integer isCorrect;    // 是否正确：0-错误，1-正确
    private Integer score;        // 该题得分

}
