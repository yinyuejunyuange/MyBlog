package org.oyyj.studyservice.dto.knowledgePoint;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 知识点增加
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgePointDTO {


    private Long id;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String title;             // 题目
    private String recommendedAnswer; // 推荐回答
    private List<InterviewQuestionsDTO> relatedQuestions;
    private String level;
    private List<String> type;
    private Integer isDelete;         // 是否删除 1已删除 0未删除

}
