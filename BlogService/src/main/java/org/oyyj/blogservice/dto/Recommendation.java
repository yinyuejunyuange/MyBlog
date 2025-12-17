package org.oyyj.blogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协同过滤的推荐结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    private Long blogId;
    private double predictedRating;

}
