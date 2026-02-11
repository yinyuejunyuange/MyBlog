package org.oyyj.gatewaydemo.pojo.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知属性
public class AuthUser implements UserDetails {

    private Long userId;
    @JsonAlias({"username", "userName"})
    private String userName;

    private String password;

    private String imageUrl; // 头像

    private List<String> permissions;

    private List<String> roles;

    private String ip; //  用户的IP

    private Integer isUserLogin;

    @JsonIgnore
    private List<SimpleGrantedAuthority> authorities;

    public AuthUser(Long userId, String userName, String password, List<String> permissions, List<String> roles,Integer isUserLogin,String imageUrl) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.imageUrl = imageUrl;
        this.permissions = permissions;
        this.roles = roles;
        this.isUserLogin = isUserLogin;

    }


    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            return  authorities;
        }else{
            return authorities;
        }
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
