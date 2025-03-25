package org.oyyj.userservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnouncementUserDTO {
    private Boolean isUserRead;
    private String id;
    private Long adminId;
    private String title;
    private String content;
    private Date updateTime;
}
