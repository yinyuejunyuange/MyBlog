package org.oyyj.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuAdminDTO {
    private String id;
    private String name;
    private String url;
    private List<String> adminTypes;
    private Date createTime;
    private String createBy;
    private Date updateTime;
    private String updateBy;
    private Integer sort;
    private List<MenuAdminDTO> children;
}
