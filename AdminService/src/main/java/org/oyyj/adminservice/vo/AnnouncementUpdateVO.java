package org.oyyj.adminservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnouncementUpdateVO {
    private Long announcementId;
    private Long adminId;
    private String title;
    private String content;
}
