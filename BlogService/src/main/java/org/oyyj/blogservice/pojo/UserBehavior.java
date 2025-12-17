package org.oyyj.blogservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户行为记录
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBehavior {
    private Long id;
    private Long userId;
    private Long blogId;
    private BehaviorType behaviorType;
    private Double weight;
    private Date createTime;

    @Getter
    public enum BehaviorType{
        VIEW(0.3),
        LIKE(0.6),
        COMMENT(0.4),
        COLLECT(0.7),
        SHARE(0.5);
        private double weight;
        BehaviorType(double weight){
            this.weight = weight;
        }
    }

}
