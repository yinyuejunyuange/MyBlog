package org.oyyj.userservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO {
    private String userId;

    private String userName;

    private String imageHead;

    private String introduction;

    private Integer kudus;

    private Integer view;

    private Integer star;

    private Integer attention;

    private Integer beAttention;

    private Integer blogs;

    // 判断是否为用户自己
    private Boolean isUserSelf;

    private Boolean isUserFollow;


}
