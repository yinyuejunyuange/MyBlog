package org.oyyj.gatewaydemo.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.oyyj.gatewaydemo.pojo.auth.AuthUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.gatewaydemo.utils.JWTUtils;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 过滤器 通过token得出结果
 */
@Component
@Slf4j
public class JWTAuthenticationTokenFilter implements WebFilter { // 原来的过滤器实现接口存在问题

    @Autowired
    private RedisUtil redisUtil;

    private final ObjectMapper objectMapper =  new ObjectMapper();



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest mutatedRequest;
        String clientRealIp = getClientRealIp(request);
        if(Objects.isNull(clientRealIp)){
            return handleError(response,"请求无IP来源",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // 获取token
        String token = request.getHeaders().getFirst("X-Token");
        // token 为空交给 SpringSecurity的authorizeExchange决定
        if (!StringUtils.hasText(token)) {
            // 匿名请求 获取访问者的IP信息
            mutatedRequest=request.mutate()
                    .header("X-Real-IP", clientRealIp)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        AuthUser authUser = JWTUtils.parseTokenAndGetUserId(token);
        if(Objects.isNull(authUser)|| authUser.getUserId() == null){
            return handleError(response,"token无效",HttpStatus.UNAUTHORIZED);
        }

        String redisToken = (String) redisUtil.get(String.valueOf(authUser.getUserId()));
        if(redisToken == null || !redisToken.equals(token)){
            return handleError(response,"会话过期，请重新登录",HttpStatus.UNAUTHORIZED);
        }

        // 刷新redis中token有效期
        redisUtil.set(String.valueOf(authUser.getUserId()), token, 24, TimeUnit.HOURS);


        // 将用户信息添加到 请求头中
        try {
            mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(authUser.getUserId()))
                    .header("X-User-Name", authUser.getUsername())
                    .header("X-User-Permission", objectMapper.writeValueAsString(authUser.getPermissions()))
                    .header("X-User-Role", objectMapper.writeValueAsString(authUser.getRoles()))
                    .header("X-Authenticated", "true")
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 构建认证对象提供给 SpringSecurity webFlux
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(authUser.getUsername(), authUser.getPassword());

        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authenticationToken);

        try {
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
        } catch (Exception e) {
            return handleError(response,"过滤器异常",HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * 统一的错误处理方法（响应式版本）
     */
    private Mono<Void> handleError(ServerHttpResponse response, String message, HttpStatus status) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        errorResponse.put("success", false);
        errorResponse.put("timestamp", System.currentTimeMillis());

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // 如果JSON序列化失败，返回简单的错误信息
            String simpleError = "{\"error\":\"认证失败\"}";
            DataBuffer buffer = response.bufferFactory()
                    .wrap(simpleError.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * 获取用户真实IP
     * @param request
     * @return
     */
    private String getClientRealIp(ServerHttpRequest request) {

        // 标准代理头
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor) ) {
            xForwardedFor = xForwardedFor.split(",")[0]; // 可能存在多项IP
            return xForwardedFor.trim();
        }

        // Nginx代理（实际部署环境时使用）
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp) ) {
            return xRealIp;
        }

        // 在tcp/ip层获取到IP信息
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if(remoteAddress != null){
            String hostAddress = remoteAddress.getAddress().getHostAddress();
            if(!Strings.isBlank(hostAddress)){
                return hostAddress;
            }
        }
        // 没有编写来源 可能来源爬虫等工具
        log.warn("拦截无IP请求：{}",request.getHeaders());
        return null;
    }

}
