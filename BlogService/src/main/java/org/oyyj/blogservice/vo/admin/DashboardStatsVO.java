package org.oyyj.blogservice.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStatsVO {
    private Long recentBlogIncrease;
    private Long recentCommentIncrease;
    private Long recentReplyIncrease;
}
