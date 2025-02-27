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
    private String userId;
    private String userName;
    private String title;
    private String Introduce;
    private String context;
    private List<String> typeList;
    private Date createTime;
    private Date updateTime;
    private String star;
    private String kudos;
    private String watch;
    private String commentNum;
    private Boolean isUserKudos=false;
    private Boolean isUserStar=false;
}
