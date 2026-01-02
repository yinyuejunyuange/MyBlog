package org.oyyj.gatewaydemo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.gatewaydemo.mapper.SysPermissionMapper;
import org.oyyj.gatewaydemo.mapper.SysRoleMapper;
import org.oyyj.gatewaydemo.mapper.UserMapper;
import org.oyyj.gatewaydemo.pojo.auth.AuthUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.gatewaydemo.pojo.User;
import org.oyyj.gatewaydemo.pojo.dto.RegisterDTO;
import org.oyyj.gatewaydemo.pojo.vo.JWTUserVO;
import org.oyyj.gatewaydemo.service.IUserService;
import org.oyyj.gatewaydemo.utils.JWTUtils;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private ReactiveAuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;



    @Override // 返回相关结果
    public Mono<JWTUserVO> login(String username, String password){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
       // Authentication authentication = authenticate.block();  // 不能使用block阻塞 官方要求 保留特性
        return authenticationManager.authenticate(authenticationToken)
                .flatMap(authentication -> {
                    if(Objects.isNull(authentication)){
                        // 认证失败
                        throw new RuntimeException("登录失败 用户名或密码错误");
                    }
                    // 封装 userdetails信息
                    // 登录成功后 authentication中的Principal 中会存储用户的信息
                    AuthUser authUser = (AuthUser) authentication.getPrincipal();

                    String token = null; // 获取到token
                    try {
                        token = JWTUtils.createToken(authUser, "web", "USER");
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    // 将token存储到redis中
                    redisUtil.set(String.valueOf(authUser.getUserId()), token,24, TimeUnit.HOURS); // 存储并设置时间24小时
                    return Mono.just(JWTUserVO.builder()
                            .id(String.valueOf(authUser.getUserId()))
                            .username(authUser.getUsername())
                            .imageUrl(authUser.getImageUrl())
                            .token(token)
                            .isValid(true)
                            .build());
                }); // 统一外层处理异常
    }

    // 从上下文环境中 securitycontextHolder 获取到用户的信息
    @Override
    public void LoginOut() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken=
                (UsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) usernamePasswordAuthenticationToken.getPrincipal();
        // 删除redis中用户的信息
        redisUtil.delete(String.valueOf(loginUser.getUserId()));
    }

    @Override
    public Mono<JWTUserVO> registerUser(RegisterDTO registerDTO) throws IOException {
        User one = getOne(Wrappers.<User>lambdaQuery().eq(User::getName, registerDTO.getUsername()));
        if(!Objects.isNull(one)){
            return null;
        }
        // todo 后续添加默认头像图片

        Date date = new Date();
        User build = User.builder()
                .name(registerDTO.getUsername())
                .sex(registerDTO.getSex())
                .email(registerDTO.getEmail())
                .imageUrl("123")
                .createTime(date)
                .updateTime(date)
                .isDelete(0)
                .isFreeze(0)
                .build();
        String encode = passwordEncoder.encode(registerDTO.getPassword());
        build.setPassword(encode);

        boolean save = save(build);

        if(save){
            // 默认将 新注册的用户设置为 user
            Long roleId = roleMapper.selectRoleBuName("user");
            Integer i = roleMapper.defaultSetUser(build.getId(), roleId);
            if(i==0){
                // 权限添加失败
                throw new RuntimeException("权限添加 失败");
            }

            // 生成token 并存储到redis
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(registerDTO.getUsername(), registerDTO.getPassword());
            return authenticationManager.authenticate(usernamePasswordAuthenticationToken)
                    .flatMap(authentication -> {
                        if(Objects.isNull(authentication)){
                            // 认证失败
                            throw new RuntimeException("登录失败 用户名或密码错误");
                        }

                        AuthUser authUser = (AuthUser) authentication.getPrincipal();

                        String token = null;
                        try {
                            token = JWTUtils.createToken(authUser, "web", "USER");
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        redisUtil.set(String.valueOf(build.getId()),token,24,TimeUnit.HOURS);

                        return Mono.just(JWTUserVO.builder()
                                .id(String.valueOf(build.getId()))
                                .isValid(true)
                                .token(token)
                                .username(build.getName())
                                .build());
                    });

        }else{
            throw  new RuntimeException("注册失败");
        }
    }

}
