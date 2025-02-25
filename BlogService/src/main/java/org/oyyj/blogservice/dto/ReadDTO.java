package org.oyyj.blogservice.dto;

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
public class ReadDTO {
    private String id; // 博客id
    private Long userId;
    private String title;
    private String Introduce;
    private String context;
    private List<String> typeList;
    private Date createTime;
    private Date updateTime;
}
