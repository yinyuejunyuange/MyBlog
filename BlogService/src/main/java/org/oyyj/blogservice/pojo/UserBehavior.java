package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * 用户行为记录
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_behavior")
public class UserBehavior {
    @TableId("id")
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
