package org.oyyj.blogservice.vo.reply;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.dto.ReadReplyDTO;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyResultVO {
    private List<ReadReplyDTO> list;
    private Date lastTime;
    private String lastId;
}
