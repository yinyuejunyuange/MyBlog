package org.oyyj.adminservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.auth.InvalidCredentialsException;
import org.oyyj.adminservice.exception.SourceNotLegitimateException;
import org.oyyj.adminservice.util.ResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log= LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        String message="";
        ObjectMapper mapper = new ObjectMapper();

        if(authException instanceof InsufficientAuthenticationException){
            // token 无效获取 或者 token 过期
            message=authException.getMessage();
            Map<String, Object> map = ResultUtil.failMap(401, message);
            String mapStr = mapper.writeValueAsString(map);

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(mapStr);

            log.error(message);

        }else if(authException instanceof SourceNotLegitimateException){
            // 请求来源不正确
            message=authException.getMessage();
            Map<String, Object> map = ResultUtil.failMap(403, message);
            String mapStr = mapper.writeValueAsString(map);

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(mapStr);

            log.error(message);
        }else{
            message=authException.getMessage();
            Map<String, Object> map = ResultUtil.failMap(400, message);
            String mapStr = mapper.writeValueAsString(map);

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(mapStr);

            log.error(message);
        }

    }
}
