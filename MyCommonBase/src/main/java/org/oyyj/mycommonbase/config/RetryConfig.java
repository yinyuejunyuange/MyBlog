package org.oyyj.mycommonbase.config;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RetryConfig {


    public static final Retryer<Boolean> LOCK_RETRYER = RetryerBuilder.<Boolean>newBuilder()
            .retryIfResult(Boolean.FALSE::equals)
            // 指数退避等待策略 避免重试风暴
            .withWaitStrategy(WaitStrategies.exponentialWait(100,3, TimeUnit.SECONDS))
            // 最多重试三次
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();
}
