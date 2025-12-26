package org.oyyj.mycommonbase.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Long RELEASE_SUCCESS = 1L;
    private static final String RELEASE_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";;


    // 设置键值对
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 设置键值对并指定过期时间
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    // 设置键值对并指定过期时间
    public void set(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    // 获取值
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 获取值
    public String getString(String key) {
        Object obj = redisTemplate.opsForValue().get(key);
        return obj == null ? null : obj.toString();
    }


    /**
     * 存储Map到Redis Hash
     * @param key Redis key
     * @param map 要存储的Map（Long→Double）
     */
    public void setHashWithLongDouble(String key, Map<Long, Double> map) {
        // 转换Map的key为String（Redis Hash的field只能是字符串）
        Map<String, Double> hashMap = map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(), // Long→String
                        Map.Entry::getValue
                ));
        // 存储到Redis Hash
        redisTemplate.opsForHash().putAll(key, hashMap);
    }

    /**
     * 读取Redis Hash为Map<Long, Double>
     * @param key Redis key
     * @return 解析后的Map
     */
    public Map<Long, Double> getHashWithLongDouble(String key) {
        Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(key);
        // 转换field从String→Long，value→Double
        return hashEntries.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> Long.parseLong(entry.getKey().toString()),
                        entry -> Double.parseDouble(entry.getValue().toString())
                ));
    }

    /**
     * 存储list
     * @param key
     * @param list
     */
    public void setList(String key, List<String> list) {
        redisTemplate.opsForList().leftPushAll(key, list);
    }

    /**
     * 获取列表
     * @param key
     * @return
     */
    public List<String> getList(String key) {
        List<Object> range = redisTemplate.opsForList().range(key, 0, -1);// 获取从0到最后一个
        return range.stream().map(String::valueOf).toList();
    }
    // 删除键
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // 判断键是否存在
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    // 如果不存在，则设置
    public Boolean setNx(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    // 如果不存在，则设置，附带过期时间
    public Boolean tryLock(String lockKey, String requestId, long seconds) {
        return redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, seconds, TimeUnit.SECONDS);
    }

    // 如果不存在，则设置，附带过期时间
    public Boolean tryLock(String lockKey, String requestId, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, timeout, unit);
    }

    // 不存在返回true，存在则删除
    public Boolean releaseLock(String lockKey, String requestId){
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(RELEASE_SCRIPT);
        redisScript.setResultType(Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        return RELEASE_SUCCESS.equals(result);
    }

}
