package org.oyyj.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnouncementTaskDTO {

    // 公用
    private String adminId;

    // 公告相关
    private Long announcementId;
    private String title;
    private String content;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;

    // 任务相关
    private String taskId;
    private String taskName;
    private String frequency; // corn表达式
    private Integer status;
}
