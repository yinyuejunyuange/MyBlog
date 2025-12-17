package org.oyyj.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 博客作者信息 ：博客粉丝数 博客创建时间 博客简介 博客原创数 博客访问量
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogUserInfoDTO {
    private String userId;
    private String userName;
    private String imageUrl;
    private Date createTime;
    private String introduction;
    private Long blogNum;
    private Long visitedNum;
    private Long starNum; // 作者粉丝数
    private Long kudosNum; // 作者点赞量
    private Boolean isUserStar; // 判断当前用户是否点赞

}
