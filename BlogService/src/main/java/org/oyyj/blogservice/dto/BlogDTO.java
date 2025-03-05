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
public class BlogDTO {
    private String id;
    private String title;
    private String context;
    private String text;  // 纯文本 博客内容
    private String userId;
    private String userName;
    private String introduce;
    private Date createTime;
    private Date updateTime;
    private Integer status;
    private List<String> typeList;
    private String kudos;
    private String star;
    private String watch;
    private String commentNum;
}
