package org.oyyj.gatewaydemo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.oyyj.gatewaydemo.mapper.SysPermissionMapper;
import org.oyyj.gatewaydemo.mapper.SysRoleMapper;
import org.oyyj.gatewaydemo.mapper.UserMapper;
import org.oyyj.gatewaydemo.pojo.User;
import org.oyyj.mycommonbase.common.auth.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;

@Service
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {

    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysPermissionMapper  sysPermissionMapper;
    @Autowired
    private UserMapper userMapper;


    @Override
    public Mono<UserDetails> findByUsername(String username) {
        // 核心：将同步的MyBatis查询封装为异步操作
        // fromCallable：把同步代码包装为Mono（异步流）
        // subscribeOn：指定同步操作在弹性线程池执行，避免阻塞WebFlux的事件循环线程
        return Mono.fromCallable(() -> {
                    // 1. 保留原有查询用户逻辑
                    User one = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getName, username));
                    if (Objects.isNull(one)) {
                        // 规范：抛出Spring Security标准的UsernameNotFoundException（而非RuntimeException）
                        throw new UsernameNotFoundException("用户名或密码错误");
                    }

                    // 2. 保留原有查询权限、角色逻辑
                    List<String> permissions = sysPermissionMapper.getPermissionsByUserId(one.getId());
                    List<String> roles = sysRoleMapper.selectUserRole(one.getId());

                    // 3. 封装为AuthUser（和原有逻辑一致）
                    return (UserDetails) new AuthUser(one.getId(), one.getName(), null, permissions, roles);

                })
                // 将同步IO操作（查库）放到弹性线程池，避免阻塞网关
                .subscribeOn(Schedulers.boundedElastic())
                // 异常兜底：确保所有同步异常转为Reactive异常
                .onErrorMap(e -> {
                    if (e instanceof UsernameNotFoundException) {
                        return e;
                    }
                    // 其他异常统一转为用户不存在（避免暴露底层错误）
                    return new UsernameNotFoundException("用户查询失败：" + e.getMessage());
                });
    }
}
