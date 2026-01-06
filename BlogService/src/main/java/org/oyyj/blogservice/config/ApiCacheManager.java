package org.oyyj.blogservice.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.oyyj.blogservice.config.Service.IApiConfigService;
import org.oyyj.blogservice.config.pojo.ApiConfig;
import org.oyyj.blogservice.config.pojo.EnhancedCorrelationData;
import org.oyyj.blogservice.config.pojo.RabbitMqMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.oyyj.mycommon.common.MqPrefix.*;


@Slf4j
@Configuration
@Component
public class ApiCacheManager {

    @Autowired
    private IApiConfigService apiConfigService;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_PREFIX = "cache:"; // 缓存信息
    private static final String VERSION_SUFFIX = ":version"; // 版本信息

    @Value("${spring.application.name:BlogService}") // 从配置中获取数据
    private String applicationName;

    @Value("${spring.cloud.nacos.discovery.cluster-name}")
    private String cluster_name;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CacheManager cacheManager;

    private final ReentrantLock lock = new ReentrantLock();


    // 使用LUA脚本 保证更新缓存时的数据一致性
    private final static DefaultRedisScript<Long> UPDATE_REDIS_CACHE = new DefaultRedisScript<>();

    static {
        UPDATE_REDIS_CACHE.setScriptText(
                "local current_version = redis.call('GET', KEYS[2]) \n "+
                        "if current_version and tonumber(current_version)>tonumber(ARGV[2]) then \n " +
                        "   return tonumber(current_version) \n " +
                        "end\n" +
                        "redis.call('SET', KEYS[1], ARGV[1] ,'EX',ARGV[3] )\n"+
                        "redis.call('SET', KEYS[2], ARGV[2] ,'EX',ARGV[4] )\n"+
                        "return tonumber(ARGV[2])"
        );
        UPDATE_REDIS_CACHE.setResultType(Long.class);

    }

    public <T>T get(String key){
        // 1.检查本地版本号
        Long localVersion = getLocalVersion(key+VERSION_SUFFIX);
        String redisVersionKey = CACHE_PREFIX + key + VERSION_SUFFIX; // redis中存储的相应的字段

        // 获取存储在redis中的版本号
        Long redisVersion = (Long) redisTemplate.opsForValue().get(redisVersionKey);

        // 如果当地版本号 不存在 或者 redis中的数据 与当前版本不一致 清空本地缓存 重新赋值
        if(localVersion == null || !Objects.equals(localVersion,redisVersion)){
            evictLocalCache(key);
            if(redisVersion != null){
                putLocalVersion(key+VERSION_SUFFIX, redisVersion);
            }
        }

        // 先尝试从本地获取数据
        T value = getLocalCache(key);
        if(value != null){
            return value;
        }

        // 尝试从redis中获取数据
        T redisCache = getRedisCache(CACHE_PREFIX + key);
        if(redisCache != null){
            putToLocalCache(key, redisCache);
            return  redisCache;
        }

        // 确保只有一个请求达到数据库
        lock.lock();
        try{
            // 双重锁检查 避免重复创建
            Object localCache = getLocalCache(key);
            if(localCache != null){
                return (T)localCache;
            }
            Object redisCaches = getRedisCache(CACHE_PREFIX + key);
            if(redisCaches != null){
                return (T)redisCaches;
            }

            // 通过数据源获取
            ApiConfig one = apiConfigService.getOne(Wrappers.<ApiConfig>lambdaQuery()
                    .eq(ApiConfig::getName, key)
            );
            puttoAllCache(key, one.getValue());
            return (T)one.getValue();
        }finally {
            lock.unlock();
        }
    }

    /**
     * 清空本地缓存
     */

    private void evictLocalCache(String key){
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            cache.evict(key);
            cache.evict(key+VERSION_SUFFIX);
        }
    }

    private Long getLocalVersion(String key){
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if(valueWrapper != null){
                return (Long)valueWrapper.get();
            }
        }
        return null;
    }

    private void putLocalVersion(String key,Long version){
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            cache.putIfAbsent(key,version);
        }
    }

    private <T>T getLocalCache(String key){
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if(valueWrapper != null){
                return (T)valueWrapper.get();
            }
        }
        return null;
    }

    private <T>T getRedisCache(String key){
        // return  (T)redisTemplate.opsForValue().get(key);  // 配置中设置了 字符串
        return  (T)stringRedisTemplate.opsForValue().get(key);
    }

    private void putToLocalCache(String key,Object value){
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            cache.putIfAbsent(key,value);
        }
    }

    private <T>void puttoAllCache(String key,T value){
        long dateTemp = System.currentTimeMillis();
        // 1. 版本  2. 缓存的值（唯一性）
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            cache.putIfAbsent(key,value);
            cache.putIfAbsent(key+VERSION_SUFFIX,dateTemp);
        }
        // 添加到redis中
        flushRedisDataAndVersion(key,value,dateTemp);
    }

    /**
     * 原子性处理 redis的数据和版本的更新
     */
    private <T>void flushRedisDataAndVersion(String key,T value,long version){
        List<String> keys = new ArrayList<>();
        keys.add(CACHE_PREFIX+key);
        keys.add(CACHE_PREFIX+key+VERSION_SUFFIX);

        List<String> values = new ArrayList<>();
        values.add(String.valueOf(value));
        values.add(String.valueOf(version));
        values.add("30"); // 数据时存储时间
        values.add("31"); // 版本存储时间

        Long execute = stringRedisTemplate.execute(
                UPDATE_REDIS_CACHE,
                keys,
                String.valueOf(value),
                String.valueOf(version),
                "1800",
                "1860"  // 多一分钟
                );
        if(!Objects.equals(execute,version)){
            log.error("原子性更改时间失败");
        }
    }

    /**
     * 更新缓存  --- 多查少改 直接使用 版本号删除即可
     *
     */
    public <T> void put(String key, T value) throws InterruptedException {

        // 更新数据库
        apiConfigService.update(Wrappers.<ApiConfig>lambdaUpdate()
                .eq(ApiConfig::getName, key)
                .set(ApiConfig::getValue,value)
        );

        // 删除redis
        redisTemplate.delete(CACHE_PREFIX+key);
        redisTemplate.delete(CACHE_PREFIX+key+VERSION_SUFFIX);

        // 删除 本地缓存数据
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            cache.evict(key);
            cache.evict(key+VERSION_SUFFIX);
        }

        sendInvalidationMessage(key);
    }

    /**
     * 删除缓存 --- 没有必要使用延时双删  已经进行了控制
     */
    public <T> void del(String key, T value) throws InterruptedException {
        // 更新数据库中的数据
        apiConfigService.update(Wrappers.<ApiConfig>lambdaUpdate()
               .eq(ApiConfig::getName, key)
               .eq(ApiConfig::getValue, value)
        );
        // 删除redis
        redisTemplate.delete(CACHE_PREFIX+key);
        redisTemplate.delete(CACHE_PREFIX+key+VERSION_SUFFIX);

        // 删除 本地缓存数据
        Cache cache = cacheManager.getCache("default");
        if(cache != null){
            cache.evict(key);
            cache.evict(key+VERSION_SUFFIX);
        }

        //  发送缓存失效消息，通知其他节点
        sendInvalidationMessage(key);
    }

    /**
     * 发送缓存失效的消息
     */
    private void sendInvalidationMessage(String key) {
        RabbitMqMessage message = new RabbitMqMessage(key, null , "");
        EnhancedCorrelationData ed=new EnhancedCorrelationData(UUID.randomUUID().toString(),message.toString());
        rabbitTemplate.convertAndSend(
                CACHE_INVALIDATION_EXCHANGE,
                CACHE_INVALIDATION_ROUTING_KEY,
                message,
                ed
        ); //  广播 也包括自己
    }


    /**
     * 接受消息队列 并 确认消息  使用SqLe方法
     */
    @RabbitListener(queues = "#{rabbitMqCacheInvalidationConfig.getCacheInvalidationQueue()}")
    public void handleInvalidationMessage(RabbitMqMessage message ,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("接受到消息队列中的数据");
        // 从Redis中获取id 查看是否已经处理过了
        Object o = redisTemplate.opsForValue().get(applicationName + ":" +cluster_name+":"+ message.getMessageId());
        if(o != null){
            // 已经处理完了
            if(channel.isOpen()){
                channel.basicAck(deliveryTag,false);
            }
            redisTemplate.opsForValue().set(applicationName + ":" +cluster_name+":"+ message.getMessageId(), 1,1,TimeUnit.HOURS);// 延时
            return ;
        }

        boolean isSuccess = true;
        // 从redis中获取重试次数
        Object repeat = redisTemplate.opsForValue().get(applicationName + ":" +cluster_name+":"+ message.getMessageId()+":repeat");
        if(repeat == null || (int)repeat<3){
            try {
                String key = message.getKey();
                // 删除redis
                redisTemplate.delete(CACHE_PREFIX+key);
                redisTemplate.delete(CACHE_PREFIX+key+VERSION_SUFFIX);

                Cache cache = cacheManager.getCache("default");
                if(cache != null){
                    Cache.ValueWrapper valueWrapper = cache.get(key + VERSION_SUFFIX);
                    if( valueWrapper != null){
                        cache.evict(key);
                        cache.evict(key+VERSION_SUFFIX);
                    }
                }

            } catch (Exception e) {
                log.error("处理失败："+e.getMessage());
                isSuccess = false;
            }finally {
                if(isSuccess){
                    if(channel.isOpen()){
                        channel.basicAck(deliveryTag,false); // 之确认当前信息 避免重复确认
                    }
                    redisTemplate.opsForValue().set(applicationName + ":" +cluster_name+":"+ message.getMessageId(), 1,1,TimeUnit.HOURS);
                }else{
                    int repeatNum = (repeat == null)? 1 :(int)repeat+1;
                    redisTemplate.opsForValue().set(applicationName + ":" +cluster_name+":"+ message.getMessageId()+":repeat", repeatNum,1,TimeUnit.HOURS);
                    channel.basicNack(deliveryTag,false,true);
                }
            }
        }else{
            // 重复三次不再放入队列 进入死信队列中
            EnhancedCorrelationData ed=new EnhancedCorrelationData(UUID.randomUUID().toString(),message.toString());
            rabbitTemplate.convertAndSend(
                    DXL_INVALIDATION_EXCHANGE,
                    DXL_INVALIDATION_ROUTING_KEY,
                    message,
                    ed
            ); //  存储到死信队列中

            if (channel.isOpen()) {
                channel.basicAck(deliveryTag,false);
            }

        }
    }

//    @RabbitListener(queues = "#{rabbitMqCacheInvalidationConfig.getCacheInvalidationQueue()}")
//    public void handleInvalidationMessage(RabbitMqMessage message,
//                                          Channel channel,
//                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
//
//        String messageId = message.getMessageId();
//        String messageKey = applicationName + ":" + cluster_name + ":" + messageId;
//        String repeatKey = messageKey + ":repeat";
//
//        log.info("接收到消息队列中的数据, messageId: {}", messageId);
//
//        try {
//            // 1. 幂等性检查
//            if (Boolean.TRUE.equals(redisTemplate.hasKey(messageKey))) {
//                log.debug("消息已处理过，直接确认. messageId: {}", messageId);
//                safeAck(channel, deliveryTag);
//                return;
//            }
//
//            // 2. 检查重试次数
//            Integer repeatCount = getRepeatCount(repeatKey);
//            if (repeatCount != null && repeatCount >= 3) {
//                log.warn("消息重试次数已达上限，将送入死信队列. messageId: {}, repeatCount: {}", messageId, repeatCount);
//                sendToDlx(message);
//                safeAck(channel, deliveryTag);
//                return;
//            }
//
//            // 3. 处理消息
//            processMessage(message);
//
//            // 4. 标记为已处理
//            markAsProcessed(messageKey);
//
//            // 5. 确认消息
//            safeAck(channel, deliveryTag);
//
//        } catch (Exception e) {
//            log.error("处理消息失败, messageId: {}", messageId, e);
//
//            // 增加重试次数
//            incrementRepeatCount(repeatKey);
//
//            // 拒绝消息并重新入队
//            safeNack(channel, deliveryTag, true);
//        }
//    }
//
//    // 安全确认方法
//    private boolean safeAck(Channel channel, long deliveryTag) {
//        try {
//            if (channel != null && channel.isOpen()) {
//                channel.basicAck(deliveryTag, false);
//                return true;
//            } else {
//                log.warn("通道已关闭，无法确认消息. deliveryTag: {}", deliveryTag);
//                return false;
//            }
//        } catch (Exception e) {
//            log.error("确认消息失败. deliveryTag: {}", deliveryTag, e);
//            return false;
//        }
//    }
//
//    // 安全拒绝方法
//    private boolean safeNack(Channel channel, long deliveryTag, boolean requeue) {
//        try {
//            if (channel != null && channel.isOpen()) {
//                channel.basicNack(deliveryTag, false, requeue);
//                return true;
//            } else {
//                log.warn("通道已关闭，无法拒绝消息. deliveryTag: {}", deliveryTag);
//                return false;
//            }
//        } catch (Exception e) {
//            log.error("拒绝消息失败. deliveryTag: {}", deliveryTag, e);
//            return false;
//        }
//    }
//
//    // 处理消息的核心逻辑
//    private void processMessage(RabbitMqMessage message) {
//        String key = message.getKey();
//
//        // 删除Redis缓存
//        redisTemplate.delete(CACHE_PREFIX + key);
//        redisTemplate.delete(CACHE_PREFIX + key + VERSION_SUFFIX);
//
//        // 删除本地缓存
//        Cache cache = cacheManager.getCache("default");
//        if (cache != null) {
//            cache.evict(key);
//            cache.evict(key + VERSION_SUFFIX);
//        }
//    }
//
//    // 标记消息为已处理
//    private void markAsProcessed(String messageKey) {
//        redisTemplate.opsForValue().set(messageKey, "PROCESSED", 1, TimeUnit.HOURS);
//    }
//
//    // 获取重试次数
//    private Integer getRepeatCount(String repeatKey) {
//        Object value = redisTemplate.opsForValue().get(repeatKey);
//        return value != null ? Integer.valueOf(value.toString()) : null;
//    }
//
//    // 增加重试次数
//    private void incrementRepeatCount(String repeatKey) {
//        Integer currentCount = getRepeatCount(repeatKey);
//        int newCount = (currentCount != null) ? currentCount + 1 : 1;
//        redisTemplate.opsForValue().set(repeatKey, newCount, 1, TimeUnit.HOURS);
//    }
//
//    // 发送到死信队列
//    private void sendToDlx(RabbitMqMessage message) {
//        try {
//            EnhancedCorrelationData ed = new EnhancedCorrelationData(UUID.randomUUID().toString(), message.toString());
//            rabbitTemplate.convertAndSend(
//                    RabbitMqConfig.DXL_INVALIDATION_EXCHANGE,
//                    RabbitMqConfig.DXL_INVALIDATION_ROUTING_KEY,
//                    message,
//                    ed
//            );
//        } catch (Exception e) {
//            log.error("发送到死信队列失败. messageId: {}", message.getMessageId(), e);
//        }
//    }

}

