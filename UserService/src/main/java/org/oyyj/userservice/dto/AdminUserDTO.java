package org.oyyj.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserDTO {
    private String id;
    private String name;
    private String imageUrl;
    private String email;
    private Date createTime;
    private Date updateTime;
    private String status;
}
