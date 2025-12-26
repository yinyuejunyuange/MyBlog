package org.oyyj.mycommonbase.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {


    @Value("${spring.data.redis.host}")
    private String redisAddress;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    private static final String prefix = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(prefix+redisAddress+":"+redisPort)
                .setDatabase(0);
        return Redisson.create(config);
    }

}
