package org.oyyj.userservice.dto.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息展示视图
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO {
    private String userName;

    private String introduce;
    /**
     * 头像
     */
    private String head;

    private String blogs;

    private String likes;

    private String star;
    /**
     * 粉丝数
     */
    private String funS;
    /**
     * 用户是否关注博主
     */
    private Integer isUserStar;
}
