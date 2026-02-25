package org.oyyj.studyservice.vo.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 试卷信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionPageVO {

    private String userId;

    private List<QuestionItemVO> questions;

    /**
     * 知识库Id
     */
    private String knowledgeBaseId;

    private String knowledgeBaseName;

    /**
     * 知识点
     */
    private String knowledgePointId;

    private String knowledgePointName;


}
