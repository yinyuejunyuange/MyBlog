package org.oyyj.userservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnouncementUserVO {
    private Boolean isUserRead;
    private String id;
    private String title;
    private String content;
    private Date updateTime;
}
