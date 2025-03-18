package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.adminservice.mapper.AdminMapper;
import org.oyyj.adminservice.mapper.PermissionMapper;
import org.oyyj.adminservice.mapper.RoleMapper;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.LoginAdmin;
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
    private AdminMapper adminMapper; // 避免依赖循环

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RoleMapper roleMapper;


    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getPhone, phone)
        );

        if(Objects.isNull(admin)){
            throw new UsernameNotFoundException("用户名或密码错误"); // 抛出异常交给 ExceptionTranslationFilter 处理
        }

        //List<String> list=permissionMapper.getAdminPermissions(admin.getId());

        List<String> adminRole = roleMapper.getAdminRole(admin.getId()); // 使用权限

        return new LoginAdmin(admin,adminRole);

    }
}
