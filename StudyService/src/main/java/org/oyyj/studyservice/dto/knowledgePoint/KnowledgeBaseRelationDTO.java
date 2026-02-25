package org.oyyj.studyservice.dto.knowledgePoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseRelationDTO {
    private Long id;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete; // 逻辑删除：1-已删除，0-未删除
    private Long knowledgeBaseId;  // 知识库ID
    private Long knowledgePointId; // 知识点ID
}
