package org.oyyj.gatewaydemo.config.handle;

import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import org.apache.http.auth.AuthenticationException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;

@Component
@Order(-2) // 优先级高于SpringSecurity自带的异常处理其
public class GlobalExceptionHandle implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if(response.isCommitted()){
            return Mono.error(ex);
        }

        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Access-Control-Allow-Origin", "*");

        String msg ;
        Integer code;
        if(ex instanceof AuthenticationException){
            code = HttpStatus.UNAUTHORIZED.value();
            msg = "会话过期，请重新登录";
        }else if(ex instanceof AccessDeniedException){
            code = HttpStatus.FORBIDDEN.value();
            msg = "用户无权操作";
        }else{
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            msg = "服务器内部错误";
        }

        // 构建 JSON
        String body = "{\"code\":" + code + ",\"message\":\"" + msg + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); // 指定 UTF-8 编码
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        // ⚠️ 设置 HTTP 状态码为 OK 也可以，如果你想用 JSON code 控制前端逻辑
        response.setStatusCode(HttpStatus.OK);

        return response.writeWith(Mono.just(buffer))
                .doOnError(error -> {
                    // 如果写入失败，可以记录日志
                    System.err.println("响应写入失败：" + error.getMessage());
                });

    }
}
