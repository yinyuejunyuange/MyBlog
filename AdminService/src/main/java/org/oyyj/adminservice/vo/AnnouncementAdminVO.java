package org.oyyj.adminservice.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnouncementAdminVO {

    private Long id;
    private Long adminId;
    private String taskName; // 记录是从哪一个任务中产生的
    private String title;
    private String content;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;

}
