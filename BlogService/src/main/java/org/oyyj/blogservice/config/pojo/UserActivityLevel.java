package org.oyyj.blogservice.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityLevel {

    private Long userId;

    private Double weightedActivity; // 权重综合* 时间衰减

    private Integer behaviorCount; // 操作次数

    private Date lastActTime;

}
