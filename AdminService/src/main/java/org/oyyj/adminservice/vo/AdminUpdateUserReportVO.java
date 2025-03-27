package org.oyyj.adminservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUpdateUserReportVO {
    private String reportId;
    private Long adminId;
    private String adminName;
    private Integer status;

}
