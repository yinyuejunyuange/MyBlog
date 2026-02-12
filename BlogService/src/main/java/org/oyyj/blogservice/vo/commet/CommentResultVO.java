package org.oyyj.blogservice.vo.commet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.blogservice.dto.ReadCommentDTO;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResultVO {
    private List<ReadCommentDTO> list;
    private Date lastTime;
    private String lastId;
}
