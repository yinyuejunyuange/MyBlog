package org.oyyj.blogservice.interceptor;

import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.config.RateLimitBigDecimalManager;
import org.oyyj.blogservice.config.RateLimitManage;
import org.oyyj.blogservice.config.exception.RateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitManage rateLimitManage;

    @Autowired
    private RateLimitBigDecimalManager rateLimitBigDecimalManager;

    private String VIP_CATCH_PATH="/VIPCacheInfo";

    // 逻辑处理前执行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("requestURI:{}",requestURI+"进入拦截器");
        if(requestURI.contains(VIP_CATCH_PATH)){
            if(!rateLimitManage.tryCatchToken(1)){
                log.warn("当前访问量较大 ");
                throw new RateLimitException("当前访问量较大 请稍后在试");
            }
//            if(!rateLimitBigDecimalManager.tryAcquireToken(1)){
//                log.warn("当前访问量较大 ");
//                throw new RateLimitException("当前访问量较大 请稍后在试");
//            }
        }
        return true;
    }
}
