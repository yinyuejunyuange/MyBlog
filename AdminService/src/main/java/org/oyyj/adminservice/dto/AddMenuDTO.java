package org.oyyj.adminservice.dto;

import lombok.*;
import org.springframework.lang.NonNull;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddMenuDTO {

    private String name;
    private String url;
    private List<String> adminTypes;
    private Date createTime;
    private String createBy;
    private Date updateTime;
    private String updateBy;
    private Integer sort;
    private String parentId;
}
