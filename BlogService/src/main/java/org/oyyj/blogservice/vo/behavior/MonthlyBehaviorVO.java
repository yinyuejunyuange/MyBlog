package org.oyyj.blogservice.vo.behavior;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyBehaviorVO {

    private String month; // yyyy-MM
    private Long viewCount = 0L;
    private Long likeCount = 0L;
    private Long commentCount = 0L;
    private Long collectCount = 0L;
    private Long shareCount = 0L;

}
