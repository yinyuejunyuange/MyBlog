package org.oyyj.gatewaydemo.filter;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.gatewaydemo.pojo.User;
import org.oyyj.gatewaydemo.pojo.dto.LoginDTO;
import org.oyyj.gatewaydemo.pojo.dto.RegisterDTO;
import org.oyyj.gatewaydemo.pojo.vo.JWTUserVO;
import org.oyyj.gatewaydemo.service.IUserService;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class LocalRequestFilter extends AbstractGatewayFilterFactory<LocalRequestFilter.Config> {

    @Autowired
    private IUserService userService;

    @Autowired
    private ObjectMapper objectMapper;


    public LocalRequestFilter() {
        super(Config.class);
    }

    private static final DataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory(); // 字节缓冲应对GATEWAY

    // todo 补充 管理员端的 登录（管理员的注册和注销只能由超管负责）
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain)->{
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            return handLocalRequest(config.handlerType,request,response)
                    .switchIfEmpty(chain.filter(exchange))
                    .onErrorResume(e->{
                        log.error("本地过滤器处理失败",e);
                        return writeErrorResponse(response,HttpStatus.INTERNAL_SERVER_ERROR,"服务器错误："+e.getMessage());
                    });

        };
    }

    private Mono<Void> handLocalRequest(HandlerType handlerType,ServerHttpRequest request, ServerHttpResponse response){
        return switch (handlerType) {
            case LOGIN -> handleLogin(request, response);
            case REGISTER -> handleRegister(request, response);
            case LOGOUT -> handleLogout(request, response);
            default -> Mono.empty();
        };
    }


    private Mono<Void> handleLogin(ServerHttpRequest request, ServerHttpResponse response){
        return parseRequestBody(request, LoginDTO.class)
                .flatMap(loginRequest->{
                    if(StringUtils.isEmpty(loginRequest.getPassword())||
                    StringUtils.isEmpty(loginRequest.getUsername())){
                        return writeErrorResponse(response,HttpStatus.BAD_REQUEST,"用户名或密码不可为空");
                    }

                    try {
                        return userService.login(loginRequest.getUsername(), loginRequest.getPassword())
                                .flatMap(loginVO->{
                                    return writeSuccessResponse(response,loginVO);
                                })
                                .onErrorResume(e->{
                                    return writeErrorResponse(response,HttpStatus.UNAUTHORIZED,"用户名或密码错误");
                                });
                    } catch (JsonProcessingException e) {
                        log.error("登录失败：{}",e.getMessage(),e);
                        return writeErrorResponse(response,HttpStatus.INTERNAL_SERVER_ERROR,"登录失败!");
                    }
                });

    }

    // todo 后续实现 收集验证码登录 和微信扫码登录

    private Mono<Void> handleRegister(ServerHttpRequest request, ServerHttpResponse response){
        return parseRequestBody(request, RegisterDTO.class)
                .flatMap(registerDTO -> {
                    if(StringUtils.isEmpty(registerDTO.getEmail())||
                    StringUtils.isEmpty(registerDTO.getPassword())){
                        return writeErrorResponse(response,HttpStatus.BAD_REQUEST,"用户名和密码不可为空！");
                    }

                    // 查询是否存在相同名称的用户
                    long userCount = userService.count(Wrappers.<User>lambdaQuery()
                            .eq(User::getName, registerDTO.getUsername()));

                    if(userCount>0){
                        return writeErrorResponse(response,HttpStatus.BAD_REQUEST,"存在相同账号用户，请更换");
                    }

                    try {
                        return userService.registerUser(registerDTO)
                                .flatMap(jwtUserVO -> {
                                    return writeSuccessResponse(response,jwtUserVO);
                                })
                                .onErrorResume(e->{
                                    log.error("注册失败：{}",e.getMessage(),e);
                                    return writeErrorResponse(response,HttpStatus.INTERNAL_SERVER_ERROR,"注册失败!");
                                });

                    } catch (IOException e) {
                        log.error("注册失败：{}",e.getMessage(),e);
                        return writeErrorResponse(response,HttpStatus.INTERNAL_SERVER_ERROR,"注册失败!");
                    }

                });
    }



    private Mono<Void> handleLogout(ServerHttpRequest request, ServerHttpResponse response) {
        String token = extractToken(request);
        if(StringUtils.isEmpty(token)){
            return writeErrorResponse(response,HttpStatus.UNAUTHORIZED,"TOKEN不可为空");
        }

        userService.LoginOut();
        return writeSuccessResponse(response,"用户登出成功");
    }




    private Mono<Void> writeSuccessResponse(ServerHttpResponse response, Object responseInfo) {
        try {
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> stringObjectMap = ResultUtil.successMap(response, "");
            String responseBody = objectMapper.writeValueAsString(stringObjectMap);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = BUFFER_FACTORY.wrap(bytes);

            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write response", e);
            return writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "响应生成失败");
        }
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response,
                                          HttpStatus status,
                                          String message) {
        try {
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            org.oyyj.mycommonbase.utils.ResultUtil<Object> errorResponse = ResultUtil.fail(status.value(), message);
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = BUFFER_FACTORY.wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
            return Mono.empty();
        }
    }

    /**
     * 获取token
     * @param request
     * @return
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private <T> Mono<T> parseRequestBody(ServerHttpRequest request, Class<T> clazz) {

        return request.getBody()
                .collectList()  // 等待将响应式数据收集完毕
                .flatMap(dataBuffers ->{
                    // 计算所有缓冲区的总字节数
                    byte[] bytes =  new byte[dataBuffers.stream()
                            .mapToInt(DataBuffer::readableByteCount)
                            .sum()
                            ];
                    // 将缓冲区内容合并到一个字节数组中
                    int offset = 0;
                    for (DataBuffer dataBuffer : dataBuffers) {
                        int length = dataBuffer.readableByteCount();
                        dataBuffer.read(bytes, offset, length);
                        offset += length;
                    }

                    // 转换成 UTF-8
                    String body = new String(bytes, StandardCharsets.UTF_8);

                    try {
                        return Mono.just(objectMapper.readValue(body,clazz));
                    } catch (JsonProcessingException e) {
                        log.error( "请求体解析失败:{}", e.getMessage());
                        return Mono.empty();
                    }
                });
    }

    @Data
    public static class Config{
        private HandlerType handlerType;

        public HandlerType getHandlerType() {
            return handlerType;
        }

        public void setHandlerType(HandlerType handlerType) {
            this.handlerType = handlerType;
        }
    }

    public enum HandlerType {
        LOGIN,
        REGISTER,
        LOGOUT
    }

}
