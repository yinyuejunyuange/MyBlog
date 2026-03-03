package org.oyyj.studyservice.vo.knowledgeComment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointCommentDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointReplyDTO;

import java.util.Date;
import java.util.List;

/**
 * 展示评论的信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeCommentVO {

    private List<KnowledgePointCommentDTO> knowledgePointCommentDTOList;

    private String lastCommentId;

    private Date lastCommentTime;

    private Boolean hasMore;


}
