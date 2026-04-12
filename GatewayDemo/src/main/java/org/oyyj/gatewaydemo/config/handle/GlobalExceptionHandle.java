package org.oyyj.gatewaydemo.config.handle;

import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import org.apache.http.auth.AuthenticationException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;

@Component
@Order(Integer.MIN_VALUE) // 优先级高于SpringSecurity自带的异常处理其
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

        String msg;
        int code;
        if(ex instanceof AuthenticationException || ex instanceof AccessDeniedException){
            code = 401;
            msg = "请先登录";
        }else if(ex instanceof ResponseStatusException responseStatusException) {
            int status = responseStatusException.getStatusCode().value();
            code = status >= 500 ? 500 : 400;
            msg = responseStatusException.getReason();
            if (msg == null || msg.isEmpty()) {
                msg = code == 500 ? "服务器内部错误" : "请求处理失败";
            }
        }else{
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            msg = "服务器内部错误";
        }

        // 构建 JSON (与 ResultUtil 格式保持一致)
        String body = "{\"code\":" + code + ",\"message\":\"" + msg + "\",\"data\":null}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); // 指定 UTF-8 编码
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        // 设置 HTTP 状态码为 OK 也可以，如果你想用 JSON code 控制前端逻辑
        response.setStatusCode(HttpStatus.OK);

        return response.writeWith(Mono.just(buffer))
                .doOnError(error -> {
                    // 如果写入失败，可以记录日志
                    System.err.println("响应写入失败：" + error.getMessage());
                });

    }
}
