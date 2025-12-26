package org.oyyj.mycommon.config;

import org.oyyj.mycommon.config.resolver.HandlerMethodAuthUserResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcCustomConfig implements WebMvcConfigurer {

    /**
     * 向spring MVC中添加自定的参数解析器
     * @param argumentResolvers
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new HandlerMethodAuthUserResolver() );
    }

}
