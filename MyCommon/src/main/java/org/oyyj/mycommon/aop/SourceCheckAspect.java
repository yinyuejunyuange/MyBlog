package org.oyyj.mycommon.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.oyyj.mycommon.annotation.SourceCheck;
import org.oyyj.mycommonbase.common.RequestHeadItems;
import org.oyyj.mycommonbase.utils.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Aspect
public class SourceCheckAspect {

    @Autowired
    private AESUtil aesUtil;

    // 切点： 拦截所有带指定注解的方法
    @Pointcut("@annotation(org.oyyj.mycommon.annotation.SourceCheck)")
    public void sourceCheck(){}
    /**
     * 前置通知
     */
    @Before("sourceCheck()")
    public void doSourceCheck(JoinPoint joinPoint){
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SourceCheck annotation = method.getAnnotation(SourceCheck.class);
        List<String> allowSourceList = Arrays.asList(annotation.allowService());
        // 提取请求信息
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(requestAttributes == null){
            throw new RuntimeException("无法获取请求上下文");
        }
        // 从请求头中获取 密文 以及 随机的IV
        HttpServletRequest request = requestAttributes.getRequest();
        String secretStr = request.getHeader(RequestHeadItems.X_SOURCE);
        String IV = request.getHeader(RequestHeadItems.X_SOURCE_IV);
        if(secretStr == null||IV == null){
            throw new RuntimeException("数据无来源未知，禁止访问");
        }
        String decrypt = aesUtil.decrypt(secretStr, IV);

        if(!allowSourceList.contains(decrypt)  ){
            throw new RuntimeException("数据来源错误，禁止访问");
        }
    }

}
