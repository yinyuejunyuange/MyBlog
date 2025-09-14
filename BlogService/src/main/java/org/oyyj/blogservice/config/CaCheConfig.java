package org.oyyj.blogservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching // 开启本地缓存
public class CaCheConfig {
    @Bean
    public CacheManager cacheManager(){
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)   // 最大条数
                .expireAfterWrite(5, TimeUnit.MINUTES)   // 缓存时间
                .recordStats()    // 统计命中率
        );
        return cacheManager;
    }

}
