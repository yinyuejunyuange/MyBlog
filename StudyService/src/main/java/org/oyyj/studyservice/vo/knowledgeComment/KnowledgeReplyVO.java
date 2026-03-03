package org.oyyj.studyservice.vo.knowledgeComment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointCommentDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointReplyDTO;

import java.util.Date;
import java.util.List;

/**
 * 展示回复的信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeReplyVO {

    private List<KnowledgePointReplyDTO> knowledgePointReplyDTOList;

    private String lastReplyId;

    private Date lastReplyTime;

    private Boolean hasMore;

}
