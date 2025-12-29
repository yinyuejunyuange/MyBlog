package org.oyyj.userservice.filter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.exception.SourceException;
import org.oyyj.userservice.pojo.LoginUser;


import org.oyyj.userservice.utils.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 过滤器 通过token得出结果
 */
@Component
public class JWTAuthenticationTokenFilter extends OncePerRequestFilter { // 原来的过滤器实现接口存在问题

    @Autowired
    private RedisUtil redisUtil;

    private final static String FROMWORD="FROM_GATEWAY";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            if(!FROMWORD.equals(request.getHeader("from"))&&request.getHeader("source").isEmpty()){
                // 不是来自gateway转发而来的  报错抛出异常
                //throw new RuntimeException("来源错误");
                System.out.println(request.getHeader("source"));
                throw new SourceException("来源错误");
            }

            // 获取token
            String token = request.getHeader("token");
            if(!StringUtils.hasText(token)){
                //token为空 没法解析 放行
                filterChain.doFilter(request, response);
                return;// 避免返回回来时还执行后面的代码
            }

            // 获取用户信息
            ObjectMapper objectMapper=new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            LoginUser loginUser = TokenProvider.parseTokenAndGetUserId(token);
            if(Objects.isNull(loginUser)){
                // 非法 token
                throw  new RuntimeException("无效用户信息 重新登录");
            }
            String redisToken = (String)redisUtil.get(String.valueOf(loginUser.getUser().getId()));
            if(redisToken==null||!redisToken.equals(token)){
                throw new RuntimeException("无效用户信息 重新登录");
            }
            // 重新设置时间
            redisUtil.set(String.valueOf(loginUser.getUser().getId()), token,24, TimeUnit.HOURS);
            System.out.println(loginUser);
            // 将用户信息存入SecurityContextHolder中（后面的过滤器都是从中获取信息）
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser,
                    null, loginUser.getAuthorities()); // 三个成员变量的构造方法中 会把一个成员变量设置为true 判断是否验证
            // todo 权限信息
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            Map<String,Object> errors = ResultUtil.failMap(message);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            OutputStream out = response.getOutputStream();
            ObjectMapper objectMapper=new ObjectMapper();
            objectMapper.writeValue(out, errors);
            out.flush();
        } catch (SourceException e) {
            String message = e.getMessage();
            Map<String,Object> errors = ResultUtil.failMap(message);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            OutputStream out = response.getOutputStream();
            ObjectMapper objectMapper=new ObjectMapper();
            objectMapper.writeValue(out, errors);
            out.flush();
        }
    }
}
