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
    private String like;
    private String star;
    private String view;
    private String commentNum;
    private Double score; // 评分
    private String userHead;// 作者头像
    private Date publishTime; // 定时
    private Integer delayMinutes ; // 延时 时长
    private String timedType; // 延时的类别 定时 or 延时
    private String publishMode;// 发布模式


}
