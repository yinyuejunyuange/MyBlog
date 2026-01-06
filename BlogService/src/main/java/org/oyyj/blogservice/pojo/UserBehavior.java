package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
    private Integer behaviorType;
    private Double weight;
    private Date createTime;
    @TableLogic
    private Integer isDelete;

}
