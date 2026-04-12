package org.oyyj.gatewaydemo.config;


import org.oyyj.gatewaydemo.filter.JWTAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 密码加密的配置类
 */
@Configuration
@EnableReactiveMethodSecurity // 启用响应式方法级安全注解，如 @PreAuthorize
@EnableWebFluxSecurity // 配置为符合getaway格式的信息
public class SecurityConfig {

    // springboot版本大于2.7后 没有WebSecurityConfigurerAdapter了 推荐基于组件的安全配置
    @Autowired
    private JWTAuthenticationTokenFilter jwtAuthenticationTokenFilter;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // encode 加密 并在密文前面展示盐值
        // matches 验证 （明文，密文）
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            ReactiveUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager manager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        return manager;
    }


    // 放行--安全配置
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) throws Exception {

       return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
               .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()) // 无状态，不存储安全上下文
                // 允许匿名访问的url
               .authorizeExchange(exchanges -> exchanges
                       .pathMatchers(HttpMethod.OPTIONS).permitAll()  // 放行所有预检查的请求
                       .pathMatchers( "/myBlog/blog/searchShow","/myBlog/blog/getHotImageBlogs","/myBlog/user/hotAuthorList","/myBlog/blog/getHotProjectBlogs","/myBlog/auth/login","/myBlog/blog/downFile**","/myBlog/auth/adminLogin","/myBlog/auth/register","/myBlog/blog/commendBlogByAuthor**","/myBlog/blog/getByKeyWord**","/myBlog/blog/read","/myBlog/user/getHead/**",
                               "/myBlog/user/blog/file/download/**","/myBlog/blog/comment/getComment**","/myBlog/blog/testUploadFile","/myBlog/blog/testMergeFile","/myBlog/blog/testExistFile",
                               "/myBlog/user/verify/getCode","/myBlog/user/verify/checkCode","/myBlog/user/getHead/**","/myBlog/user/getUserName",
                               "/myBlog/blog/homeBlogs","/myBlog/user/blog/isUserStar**","/myBlog/user/blog/isUserKudos**",
                               "/myBlog/user/getImageInIds**","/myBlog/user/getNameInIds**","/myBlog/user/blog/getUserKudosReply**",
                               "/myBlog/user/blog/getUserKudosComment**","/myBlog/user/getBlogUserInfo**","/myBlog/user/blog/getBlogByName**",
                               "/myBlog/user/blog/getBlogByTypeList**","/myBlog/blog/getBlogBySelect**","/myBlog/user/blog/getHotBlog",
                               "/myBlog/user/getUserNum","/myBlog/user/getUserInfoList","/myBlog/user/updateUserStatus",
                               "/myBlog/user/deleteUser","/myBlog/user/getUserIdByName","/myBlog/user/isUserExist","/myBlog/user/getIdsLikeName**",
                               "/myBlog/user/getUserReports**","/myBlog/user/deleteUserReport**","/myBlog/user/updateUserReport"
                       ).permitAll()
                       .anyExchange().authenticated()
               )
               .exceptionHandling(exceptionHandling -> exceptionHandling
                       .authenticationEntryPoint((exchange, ex) -> writeJsonResponse(exchange.getResponse(), 401, "请先登录"))
                       .accessDeniedHandler((exchange, ex) -> writeJsonResponse(exchange.getResponse(), 400, "请求处理失败"))
               )
               .addFilterAt(jwtAuthenticationTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION)
               .build();
    }


    private Mono<Void> writeJsonResponse(ServerHttpResponse response, int code, String message) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }




}
