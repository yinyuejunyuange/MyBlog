package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.mapper.AdminMapper;
import org.oyyj.adminservice.mapper.RoleMapper;
import org.oyyj.adminservice.pojo.Admin;
import org.oyyj.adminservice.pojo.JWTAdmin;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.pojo.Role;
import org.oyyj.adminservice.service.IAdminService;

import org.oyyj.adminservice.service.IRoleService;
import org.oyyj.adminservice.service.ISysMenuService;
import org.oyyj.adminservice.util.RedisUtil;
import org.oyyj.adminservice.util.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.security.sasl.AuthenticationException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    private static final String ADMIN = "admin";
    private static final String SUPER_ADMIN = "super_admin";

    private final Logger log= LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RoleMapper roleMapper; // 避免 循环依赖

    @Autowired
    private ISysMenuService sysMenuService;



    @Override
    public JWTAdmin login(String phone, String password,String encode,String uuid) throws AuthenticationException, JsonProcessingException {
        // 通过uuid从redis中判断数据是否正确
        System.out.println(uuid);
        String redisEncode = (String) redisUtil.get(uuid);
        if(!encode.equals(redisEncode)){
            // 验证码不正确
            throw new AuthenticationException("验证码错误");  // 抛出异常 直接交给 异常处理过滤器
        }

        System.out.println(phone+":"+password);

        // username 是正式姓名 所以按照手机号+密码进行登录
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(phone, password));
        if(Objects.isNull(authenticate)){
            throw new AuthenticationException("用户名或密码错误");
        }

        LoginAdmin principal = (LoginAdmin) authenticate.getPrincipal();
        String token = TokenProvider.createToken(principal, "wenb", "admin");
        // 存入redis 设置时间24小时
        redisUtil.set(String.valueOf(principal.getAdmin().getId())+"admin",token,24L, TimeUnit.HOURS);
        return JWTAdmin.builder()
                .id(String.valueOf(principal.getAdmin().getId()))
                .name(principal.getAdmin().getName())
                .imageUrl(principal.getAdmin().getImageUrl())
                .token(token)
                .isValid(true)
                .build();
    }

    @Override
    public List<MenuDTO> getMenuDTO() throws AuthenticationException {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        if(authorities.contains(new SimpleGrantedAuthority(SUPER_ADMIN))){
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, SUPER_ADMIN));
            return sysMenuService.adminMenu(role.getId());
        }else if(authorities.contains(new SimpleGrantedAuthority(ADMIN))){
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, ADMIN));
            return sysMenuService.adminMenu(role.getId());
        }else{
            log.error("管理员角色存在问题");
            throw new AuthenticationException("角色存在问题");
        }
    }
}
