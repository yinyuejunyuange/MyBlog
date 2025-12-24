package org.oyyj.gatewaydemo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.gatewaydemo.mapper.SysPermissionMapper;
import org.oyyj.gatewaydemo.mapper.SysRoleMapper;
import org.oyyj.gatewaydemo.mapper.UserMapper;
import org.oyyj.gatewaydemo.pojo.AuthUser;
import org.oyyj.gatewaydemo.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper; // 避免依赖循环

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    /**
     * 在过滤器链中 DaoAuthenticationProvider会调用此方法 来验证用户
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户信息
        User one = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getName, username));
        if (Objects.isNull(one)) {
            // 没有此用户
            throw new RuntimeException("用户名 或者密码错误"); // 在过滤器链中 存在一个过滤器会捕获后续的异常 ExceptionTranslationFilter
        }
        List<String> list=sysPermissionMapper.getPermissionsByUserId(one.getId());

        List<String> roles = sysRoleMapper.selectUserRole(one.getId());

        // 封装 后返回
        return new AuthUser(one.getId(),one.getName(),null,list,roles);
    }
}
