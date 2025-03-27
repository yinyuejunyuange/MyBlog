package org.oyyj.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReplyReportForAdminDTO {
    private String id;
    private String replyId;
    private String replyContent;
    private String userId;
    private String userName;
    private String content;
    private Date createTime;
    private Date finishTime;
    private String finishId;
    private String finishName;
    private Integer status;
    private Integer isDelete;
}
