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
public class AnnouncementAdminDTO {

    private String id;
    private String adminName;
    private String taskName; // 记录是从哪一个任务中产生的
    private String title;
    private String content;
    private Date createTime;
    private Date updateTime;

}
