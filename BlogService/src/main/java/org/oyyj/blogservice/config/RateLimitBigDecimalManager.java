package org.oyyj.blogservice.config;


import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的令牌桶算法（高精度版）
 * 使用BigDecimal确保计算精度，适用于需要精确控制令牌生成速率的场景
 */
@Component
public class RateLimitBigDecimalManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 总容量
    @Value("${tokenBucket.capacity:10}")
    private BigDecimal capacity;

    // 每秒新增令牌数
    @Value("${tokenBucket.addTokenNum:1}")
    private BigDecimal addTokenNum;

    // 记录上次更新时间--- redis
    private static final String LAST_UPDATE_TIME = "last:update:time";

    // 剩余容量 -- redis
    private static final String REMAINING_TOKEN = "remaining:token";

    // 分布式锁
    private static final String REDIS_TOKEN_LOCK = "redis:token:lock";

    // 计算精度（小数位数）
    private static final int SCALE = 10;

    // 毫秒与秒的转换因子
    private static final BigDecimal MILLIS_TO_SECONDS = new BigDecimal("0.001");

//    @PostConstruct
//    public void init() {
//        initBucket();
//    }

    /**
     * 初始化令牌桶
     */
    private void initBucket() {
        redisTemplate.opsForValue().setIfAbsent(LAST_UPDATE_TIME, System.currentTimeMillis());
        redisTemplate.opsForValue().setIfAbsent(REMAINING_TOKEN, capacity.toString());
    }

    /**
     * 尝试获取令牌
     * @param tokenNum 需要的令牌数量
     * @return 是否成功获取令牌
     */
    public boolean tryAcquireToken(int tokenNum) {
        return tryAcquireToken(new BigDecimal(tokenNum));
    }

    /**
     * 尝试获取令牌
     * @param tokenNum 需要的令牌数量
     * @return 是否成功获取令牌
     */
    public boolean tryAcquireToken(BigDecimal tokenNum) {
        RLock lock = redissonClient.getLock(REDIS_TOKEN_LOCK);
        try {
            if (lock.tryLock(1000, 2000, TimeUnit.MILLISECONDS)) {
                try {
                    BigDecimal currentTokens = refill();
                    if (currentTokens == null) {
                        initBucket();
                        currentTokens = capacity;
                    }

                    if (currentTokens.compareTo(tokenNum) >= 0) {
                        // 有足够的令牌，扣除令牌
                        BigDecimal newTokenCount = currentTokens.subtract(tokenNum);
                        redisTemplate.opsForValue().set(REMAINING_TOKEN, newTokenCount.toString());
                        return true;
                    } else {
                        // 令牌不足
                        return false;
                    }
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 获取锁失败
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 补充令牌桶
     * @return 当前令牌数量，如果桶不存在返回null
     */
    private BigDecimal refill() {
        Long lastUpdateTime = (Long) redisTemplate.opsForValue().get(LAST_UPDATE_TIME);
        String remainingStr = (String) redisTemplate.opsForValue().get(REMAINING_TOKEN);

        if (lastUpdateTime == null || remainingStr == null) {
            return null;
        }

        BigDecimal remaining;
        try {
            remaining = new BigDecimal(remainingStr);
        } catch (NumberFormatException e) {
            // 如果存储的值不是有效的数字，重新初始化桶
            initBucket();
            return capacity;
        }

        long nowTime = System.currentTimeMillis();
        long gapTime = nowTime - lastUpdateTime;

        if (gapTime <= 0) {
            return remaining;
        }

        // 计算应添加的令牌数: 时间间隔(毫秒) * 每秒令牌数 * 0.001
        BigDecimal tokensToAdd = addTokenNum
                .multiply(new BigDecimal(gapTime))
                .multiply(MILLIS_TO_SECONDS)
                .setScale(SCALE, RoundingMode.HALF_UP);

        if (tokensToAdd.compareTo(BigDecimal.ZERO) > 0) {
            // 有令牌需要添加
            BigDecimal newTokenCount = remaining.add(tokensToAdd);

            // 不能超过容量
            if (newTokenCount.compareTo(capacity) > 0) {
                newTokenCount = capacity;
            }

            // 更新Redis中的令牌数和最后更新时间
            redisTemplate.opsForValue().set(REMAINING_TOKEN, newTokenCount.toString());
            redisTemplate.opsForValue().set(LAST_UPDATE_TIME, nowTime);

            return newTokenCount;
        }

        return remaining;
    }

    // Getter和Setter方法
    public BigDecimal getCapacity() {
        return capacity;
    }

    public void setCapacity(BigDecimal capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getAddTokenNum() {
        return addTokenNum;
    }

    public void setAddTokenNum(BigDecimal addTokenNum) {
        this.addTokenNum = addTokenNum;
    }
}