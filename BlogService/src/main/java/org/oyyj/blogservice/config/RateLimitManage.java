package org.oyyj.blogservice.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;


/**
 * 基于redis的令牌桶算法
 */

@Component
public class RateLimitManage {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    // 总容量
    @Value("${tokenBucket.capacity}")
    private long capacity;
    // 每秒新增令牌数
    @Value("${tokenBucket.addTokenNum}")
    private  Long addTokenNum;

    // 使用缩放因子后每毫秒增加的令牌数量
    private long addTokenNumPerMill;

    // 缩放因子
    private static final long SCALE_FACTOR = 1000L;

    // 记录上次更新时间--- redis
    private static String LAST_UPDATE_TIME = "last:update:time";

    // 剩余容量 -- redis
    private static String REMAINING_TOKEN = "remaining:token";

    // 分布式锁
    private static String REDIS_TOKEN_LOCK = "redis:token:lock";


    @PostConstruct
    public void init() {
        addTokenNumPerMill = addTokenNum*SCALE_FACTOR / 1000;  // 计算出每毫秒需要增加的令牌数
        initBucket();
    }

    private long initBucket(){

        redisTemplate.opsForValue().setIfAbsent(LAST_UPDATE_TIME,System.currentTimeMillis());
        redisTemplate.opsForValue().setIfAbsent(REMAINING_TOKEN,capacity * SCALE_FACTOR); // 原子操作
        return capacity * SCALE_FACTOR;
    }


    /**
     * 尝试获取令牌
     * @param tokenNum
     * @return
     * @throws InterruptedException
     */
    public boolean tryCatchToken(int tokenNum) throws InterruptedException {
        RLock lock = redissonClient.getLock(REDIS_TOKEN_LOCK);
        if(lock.tryLock(1000,2000, TimeUnit.MILLISECONDS)) {
            try {
                Long refill = refill();
                if(refill == null){
                    refill=initBucket();
                }
                if(refill >= tokenNum*SCALE_FACTOR) {
                    redisTemplate.opsForValue().set(REMAINING_TOKEN, refill-tokenNum*SCALE_FACTOR);
                    return  true;
                }else{
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }finally {
                // 关键优化：仅当当前线程持有锁时才释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }else{
            return false;
        }
    }
    // 补充令牌桶
    private Long refill(){
        Object lastTime = redisTemplate.opsForValue().get(LAST_UPDATE_TIME);
        Object remainingTokenObject = redisTemplate.opsForValue().get(REMAINING_TOKEN);
        long lastUpdateTime ;
        Long remaining ;

        if(lastTime == null || remainingTokenObject == null){
            initBucket();
            return capacity*SCALE_FACTOR;
        }

        lastUpdateTime = (long)lastTime;
        remaining = Long.parseLong(remainingTokenObject.toString());

        long nowTime = System.currentTimeMillis();
        long gapTime = nowTime - lastUpdateTime;// 毫秒级别的差距

        if(gapTime<=0){
            return remaining;
        }

        long addTokenNum = addTokenNumPerMill*(gapTime);
        if(addTokenNum>0){
            long nowTokenNum = addTokenNum + remaining;
            remaining = Math.min(capacity*SCALE_FACTOR, nowTokenNum);
            redisTemplate.opsForValue().set(REMAINING_TOKEN, remaining);
            redisTemplate.opsForValue().set(LAST_UPDATE_TIME,nowTime);
            return remaining;
        }

        return  remaining;
    }


}
