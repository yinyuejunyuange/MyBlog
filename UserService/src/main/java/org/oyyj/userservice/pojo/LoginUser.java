package org.oyyj.userservice.pojo;

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

/**
 * 实现userDetails
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知属性
public class LoginUser implements UserDetails {


    private User user;

    private List<String> permission;

    // 此集合中 的类不满足 反序列定义的要求 所以忽略
    @JsonIgnore // 对应于 jackson
    private List<SimpleGrantedAuthority> collect;

    public LoginUser(User user, List<String> permission) {
        this.user = user;
        this.permission = permission;
    }


    // 抽象类 不好实现序列化 所以忽略
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 把permission中的权限信息 封装成SimpleGrantedAuthority对象

        if(collect == null) {
            collect = permission.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            return collect;
        }else{
            return collect;
        }
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getName();
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
