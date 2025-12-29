package org.oyyj.mycommonbase.common.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知属性
public class LoginUser {

    private Long userId;

    private String userName;

    private String password;

    private String imageUrl; // 头像

    private List<String> permissions;

    private List<String> roles;

    private String ip; //  用户的IP

    private Integer isUserLogin;


    public LoginUser(Long userId, String userName, String password, List<String> permissions, List<String> roles, Integer isUserLogin) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.permissions = permissions;
        this.roles = roles;
        this.isUserLogin = isUserLogin;
    }

}
