package org.oyyj.mycommon.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {
    @Value("${snowflake.worker-id}")
    private Long snowflakeWorkerId;
    @Value("${snowflake.datacenter-id}")
    private Long snowflakeDatacenterId;

    @Bean
    public Snowflake snowflake(){
        return new Snowflake(snowflakeWorkerId,snowflakeDatacenterId);
    }

}
