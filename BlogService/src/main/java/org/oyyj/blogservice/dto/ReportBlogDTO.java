package org.oyyj.blogservice.dto;

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
public class ReportBlogDTO {

    private String id;
    private String blogId;
    private String blogName;
    private String userId;
    private String userName;
    private String content;
    private Date createTime;
    private Date finishTime;
    private String finishId;
    private String finishName;
    private Integer status;
    private Integer isDelete;
}
