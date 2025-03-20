package org.oyyj.adminservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnouncementTaskVO {

    private String id;
    private String taskName;
    private String title;
    private String content;
    private String adminId;
    private Date createTime;
    private Date updateTime;
    private String frequency;
    private String status;
}
