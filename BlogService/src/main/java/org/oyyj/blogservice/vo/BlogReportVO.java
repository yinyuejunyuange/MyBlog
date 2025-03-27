package org.oyyj.blogservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogReportVO {
    private String blogId;
    private String content;
    private String userId;// 举报者的id
}
