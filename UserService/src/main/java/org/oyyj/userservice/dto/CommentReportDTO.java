package org.oyyj.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentReportDTO {
    private String commentId;
    private String content;
    private String userId;// 举报者的id
}
