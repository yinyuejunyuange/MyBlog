package org.oyyj.userservice.DTO;

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
public class BlogDTO {
    private String id;
    private String title;
    private String context;
    private String userId;
    private String userName;
    private String Introduce;
    private Date createTime;
    private Date updateTime;
    private Integer status;
    private List<String> typeList;
}
