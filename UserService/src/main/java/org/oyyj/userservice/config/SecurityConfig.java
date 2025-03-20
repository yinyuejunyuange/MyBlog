package org.oyyj.userservice.config;


import org.oyyj.userservice.filter.JWTAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 密码加密的配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // springboot版本大于2.7后 没有WebSecurityConfigurerAdapter了 推荐基于组件的安全配置
    @Autowired
    private JWTAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // encode 加密 并在密文前面展示盐值
        // matches 验证 （明文，密文）
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 放行--安全配置
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

       return http.csrf(csrf->csrf.disable())
               .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 基于token 所以不需要session
                // 允许匿名访问的url
                .authorizeHttpRequests((requests)->{
                    requests.requestMatchers("myBlog/user/login","myBlog/user/register","/myBlog/user/blog/read",
                                    "/myBlog/user/blog/file/download/**","http://localhost:8080/myBlog/user/blog/read/**",
                                    "/myBlog/user/verify/getCode","myBlog/user/verify/checkCode","/myBlog/user/getHead/**","/myBlog/user/getUserName",
                                    "/myBlog/user/blog/getBlogList","/myBlog/user/blog/isUserStar**","/myBlog/user/blog/isUserKudos**",
                                    "/myBlog/user/getImageInIds**","/myBlog/user/getNameInIds**","/myBlog/user/blog/getUserKudosReply**",
                                    "/myBlog/user/blog/getUserKudosComment**","/myBlog/user/getBlogUserInfo**","/myBlog/user/blog/getBlogByName**",
                                    "/myBlog/user/blog/getBlogByTypeList**","/myBlog/user/blog/getBlogByUserId**","/myBlog/user/blog/getHotBlog",
                                    "/myBlog/user/getUserNum","/myBlog/user/getUserInfoList","/myBlog/user/updateUserStatus",
                                    "/myBlog/user/deleteUser","/myBlog/user/getUserIdByName","/myBlog/user/isUserExist").permitAll()// 允许匿名访问的资源
                            .anyRequest().authenticated(); // 此外的所有全部需要验证
                })
               .exceptionHandling(exceptions->exceptions.authenticationEntryPoint(customAuthenticationEntryPoint))
               .exceptionHandling(exception->exception.accessDeniedHandler(customAccessDeniedHandler))
               .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)// 添加一个过滤器在哪种过滤器之前
               .cors(cors->cors.disable())
               .build();
    }

    // 过滤器链配置





}
