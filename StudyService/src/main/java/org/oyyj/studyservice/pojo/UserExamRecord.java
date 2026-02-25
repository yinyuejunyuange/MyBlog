package org.oyyj.studyservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * 用户答题批次表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_exam_record")
public class UserExamRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("create_by")
    private String createBy;      // 创建人（用户ID或用户名）

    @TableField("create_time")
    private Date createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private Date updateTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;     // 逻辑删除：1-已删除，0-未删除

    @TableField("user_id")
    private Long userId;          // 用户ID

    @TableField("user_name")
    private String userName;      // 用户名称（冗余）

    @TableField("total_score")
    private Integer totalScore;   // 总分

    @TableField("start_time")
    private Date startTime;       // 答题开始时间

    @TableField("end_time")
    private Date endTime;         // 答题结束时间

    @TableField("status")
    private Integer status;       // 状态：0-进行中，1-已完成，2-已评价
}
