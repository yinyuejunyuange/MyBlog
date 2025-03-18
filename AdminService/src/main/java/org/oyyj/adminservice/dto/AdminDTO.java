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
public class AdminDTO {
    private String id;
    private String name;
    private String imageUrl;
    private String phone;
    private String email;
    private Date createTime;
    private String createBy;
    private Date updateTime;
    private String updateBy;
    private Integer isDelete;
    private Integer isFreeze;
    private Integer adminType;
}
