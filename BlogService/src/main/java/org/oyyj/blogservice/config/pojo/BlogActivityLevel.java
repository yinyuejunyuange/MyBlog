package org.oyyj.blogservice.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogActivityLevel {

    private Long blogId;

    private Double weightedActivity; // 权重综合* 时间衰减

}
