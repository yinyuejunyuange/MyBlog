package org.oyyj.adminservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReplyReportVO {
    private String replyId;
    private String content;
    private String userId;// 举报者的id
}
