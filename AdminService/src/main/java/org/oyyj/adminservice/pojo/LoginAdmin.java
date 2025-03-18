package org.oyyj.adminservice.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginAdmin implements UserDetails {

    private Admin admin;

    private List<String> permissions;

    @JsonIgnore // 集合中的类属于抽象类 不好实现反序列化 所以忽略
    private List<SimpleGrantedAuthority> authorities=new ArrayList<>(); // 不可以让getAuthorities方法返回空

    public LoginAdmin(Admin admin, List<String> permissions) {
        this.admin = admin;
        this.permissions = permissions;
    }

    @Override
    @JsonIgnore  // 同理因为抽象 忽略 反序列化
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(authorities.isEmpty()){
            authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            return authorities;
        }else{
            return authorities;
        }
    }

    @Override
    public String getPassword() {
        return admin.getPassword();
    }

    @Override
    public String getUsername() {
        return admin.getName();
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
