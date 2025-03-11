package org.oyyj.adminservice.filter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.oyyj.adminservice.exception.SourceNotLegitimateException;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.util.RedisUtil;
import org.oyyj.adminservice.util.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *  过滤器 通过token 得出结果
 */
@Component
public class JWTAuthenticationTokenFilter extends OncePerRequestFilter {

    private static final Logger log= LoggerFactory.getLogger(JWTAuthenticationTokenFilter.class);

    @Autowired
    private RedisUtil redisUtil;

    private final static String FROMWORD="FROM_GATEWAY";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(!FROMWORD.equalsIgnoreCase(request.getHeader("from"))&&request.getHeader("source").isEmpty()){
            log.error("请求来源不合法");
            throw new SourceNotLegitimateException("请求来源不合法"); // 使用自定义异常
        }

        String token = request.getHeader("token");
        if(!StringUtils.hasText(token)){
            // token 为空 无法解析
            filterChain.doFilter(request, response);// 放行
            return; // 结束 避免回来后执行后续的代码
        }

        // 解析token
        ObjectMapper objectMapper=new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

        LoginAdmin loginAdmin = TokenProvider.parseTokenAndGetUserId(token);
        if(Objects.isNull(loginAdmin)){
            // token 不合法
            log.error("请求token不合法");
            throw new InsufficientAuthenticationException("无效用户信息 请重新登录");
        }

        String redisToken= (String)redisUtil.get(String.valueOf(loginAdmin.getAdmin().getId()));
        if(!token.equals(redisToken)){
            // token已经过期
            log.error("请求用户 token过期");
            throw new InsufficientAuthenticationException("无效用户信息 请重新登录");
        }

        Long expire = redisUtil.getExpire(String.valueOf(loginAdmin.getAdmin().getId()));
        if(expire<=30){
            // 距离过期时间小于30分钟 重新设置
            redisUtil.set(String.valueOf(loginAdmin.getAdmin().getId()),token,24, TimeUnit.HOURS);
        }
        // 将用户信息存入SecurityContextHolder中 便于后面的过滤器获取信息 三个参数的构造方法会将类中的某个验证是否通过的boolean设置为true
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginAdmin, null, loginAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 放行
        filterChain.doFilter(request, response);
    }
}
