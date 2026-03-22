package org.oyyj.mycommon.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.oyyj.mycommon.annotation.RequestRole;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 注解 从请求头中获取 参数
 */
@Slf4j
@Component
@Aspect
public class RequestRoleAspect {


    private ObjectMapper mapper = new ObjectMapper();

    @Pointcut("@annotation(org.oyyj.mycommon.annotation.RequestRole)")
    public void requestRolePointcut(){}

    @Before("@annotation(requestRole)")
    public void before(JoinPoint joinPoint, RequestRole requestRole){

        // 获取 request
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(attributes == null){
            throw new RuntimeException("无法获取请求对象");
        }

        HttpServletRequest request = attributes.getRequest();

        // 从请求头获取角色
        String role = request.getHeader("X-User-Role");

        if(role == null){
            throw new RuntimeException("请求头缺少 role");
        }

        List<String> strings = List.of();
        try {
            strings = mapper.readValue(role, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("数据结构不正常");
        }

        // 获取注解允许的角色
        String[] roles = requestRole.role();

        boolean hasPermission = false;

        for(String r : roles){
            if(strings.contains(r)){
                hasPermission = true;
                break;
            }
        }
        if(!hasPermission){
            throw new RuntimeException("权限不足，当前角色: " + role);
        }
        log.info("角色校验通过: {}", role);
    }

}
