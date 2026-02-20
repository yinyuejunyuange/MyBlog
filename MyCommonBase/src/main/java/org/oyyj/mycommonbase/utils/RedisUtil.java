package org.oyyj.mycommonbase.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 处理String的存储 避免发生如 将List<String>全部序列化为字符串的情况


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

    /**
     * 重置式延期：将Key的过期时间设为固定秒数（覆盖原有过期时间）
     * @param key Redis Key
     * @param expireSeconds 过期时间（秒）
     * @return 是否延期成功（Key存在则返回true，不存在返回false）
     */
    public boolean resetExpire(String key, long expireSeconds) {
        // expire方法：原子操作，直接重置过期时间
        return redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 增量式续期：在 Key 剩余过期时间基础上增加指定时长
     * 场景：需精准控制剩余时长（如剩余 10 分钟，再加 5 分钟）
     * @param key Redis Key
     * @param addSeconds 要增加的秒数
     * @return true=续期成功，false=续期失败（Key 不存在/永不过期）
     */
    public boolean incrementExpire(String key, long addSeconds,TimeUnit unit) {
        try {
            // 1. 获取 Key 剩余过期时间（秒）：-1=永不过期，-2=不存在/已过期
            Long remainSeconds = redisTemplate.getExpire(key, unit);
            if (remainSeconds == -2) {
                log.warn("Redis Key 不存在/已过期，无法增量续期，key:{}", key);
                return false;
            }
            // 2. 永不过期的 Key 需先设初始过期时间，再增量
            if (remainSeconds == -1) {
                return redisTemplate.expire(key, addSeconds, unit);
            }
            // 3. 计算新过期时间并续期
            long newExpire = remainSeconds + addSeconds;
            return redisTemplate.expire(key, newExpire, unit);
        } catch (Exception e) {
            log.error("Redis 增量式续期失败，key:{}，addSeconds:{}", key, addSeconds, e);
            return false;
        }
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
     * 获取 INTEGER 的数据
     * @param key
     * @return
     */
    public Integer getInteger(String key) {
        String integer = stringRedisTemplate.opsForValue().get(key);
        if (Objects.isNull(integer)){
            return null;
        }
        return Integer.valueOf(integer);
    }

    /**
     * 加一
     * @param key
     */
    public void incr(String key) {
        stringRedisTemplate.opsForValue().increment(key);
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
     * 存储key和value全是String的Map
     * @param key
     * @param map
     */
    public void setHashWithString(String key, Map<String, String> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 存储HashMap并设置过期时间
     * @param key
     * @param map
     * @param expireSeconds
     * @param unit
     */
    public void setHashWithString(String key, Map<String, String> map, long expireSeconds, TimeUnit unit) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, expireSeconds, unit);
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
     * 取Redis Hash为Map<String, String>   key为空时会返回一个空的Map
     * @param key
     * @return
     */
    public Map<String, String> getHashWithString(String key) {
        Map<Object, Object> hashEntries = redisTemplate.opsForHash().entries(key);
        return hashEntries.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));
    }

    /**
     * 存储list
     * @param key
     * @param list
     */
    public void setList(String key, List<String> list) {

        if(key == null || list == null || list.isEmpty()){
            return;
        }
        list = list.stream().filter(Objects::nonNull).collect(Collectors.toList());
        // 核心：先删除原有key，清空旧列表
        stringRedisTemplate.delete(key);
        stringRedisTemplate.opsForList().rightPushAll(key, list); //  向右添加数据
    }

    /**
     * 获取列表
     * @param key
     * @return
     */
    public List<String> getList(String key) {
        if(key == null || key.isEmpty()){
            return Collections.emptyList();
        }
        return stringRedisTemplate.opsForList().range(key, 0, -1);// 获取从0到最后一个

    }

    /**
     * 删除redis列表中指定的元素
     * @param key
     * @param count 删除数量 -1 删除所有匹配向 1 删除第一个匹配项 -2 删除最后一个匹配项目
     * @param value
     * @return
     */
    public Long removeListItem(String key,long count , String value){
        if(key == null || key.isEmpty()){
            return 0L;
        }
        return stringRedisTemplate.opsForList().remove(key,count,value);
    }

    /**
     * 删除脚本
     * @param key
     * @return
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }


    /**
     * LUA 脚本 原子性增加博客阅读
     */
    // 加载Lua脚本
    private static final DefaultRedisScript<Long> READ_COUNT_SCRIPT;

    static {
        READ_COUNT_SCRIPT = new DefaultRedisScript<>();
        // Lua脚本逻辑：
        // KEYS[1] = 博客阅读数key（blog:info:{blogId}）
        // KEYS[2] = 用户阅读标记key（blog:user:read:{blogId}:{userId}）
        // ARGV[1] = 用户阅读标记过期时间（秒，默认3600=1小时）
        // ARGV[2] = 阅读数key续期时间（秒，默认600=10分钟）
        READ_COUNT_SCRIPT.setScriptText(
                "local hasRead = redis.call('EXISTS', KEYS[2])\n" +
                        "if hasRead == 1 then\n" +
                        // 已阅读：续期用户标记，返回0（不计数）
                        "    redis.call('EXPIRE', KEYS[2], ARGV[1])\n" +
                        "    return 0\n" +
                        "end\n" +
                        // 未阅读：设置用户标记（带过期）+ 递增阅读数 + 续期阅读数key
                        "redis.call('SET', KEYS[2], 1, 'EX', ARGV[1])\n" +
                        "local current = redis.call('HINCRBY',KEYS[1],'watch') \n"+
                        "redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
                        "return current"
        );
        READ_COUNT_SCRIPT.setResultType(Long.class);
    }

    /**
     * 原子性增加博客的阅读数量
     * @param keys 1： 博客阅读数key（blog:info:{blogId}）  2：用户阅读标记key（blog:user:read:{blogId}:{userId}）
     * @param args 1：用户阅读标记过期时间（秒，默认3600=1小时）  2：阅读数key续期时间（秒，默认600=10分钟）
     * @return
     */
    public Long incrBlogReadNum(String []keys,String[] args ){
        // 3. 执行Lua脚本（原子操作）
        return  redisTemplate.execute(
                READ_COUNT_SCRIPT,
                Arrays.asList(keys),
                args[0],
                args[1]
        );
    }

    /**
     * LUA 脚本原子性回滚 博客阅读数-1 同时删除用户博客阅读标志
     */
    private static final DefaultRedisScript<Long> READ_COUNT_SCRIPT_DECR;

    static {
        READ_COUNT_SCRIPT_DECR = new DefaultRedisScript<>();
        // Lua脚本逻辑：
        // KEYS[1] = 博客阅读数key（blog:info:{blogId}）
        // KEYS[2] = 用户阅读标记key（blog:user:read:{blogId}:{userId}）
        READ_COUNT_SCRIPT_DECR.setScriptText(
                "local hasRead = redis.call('EXISTS', KEYS[2])\n" +
                        //  避免重复回滚
                "if hasRead == 1 then\n" +
                        // 删除标志
                "    redis.call('DEL', KEYS[2])\n" +
                        // 数据减一
                "    local current = redis.call('HINCRBY',KEYS[1],'watch',-1) \n"+
                "    if current < 0 then\n" +
                "       redis.cal('HSET', KEYS[1], 'watch',0)\n" +
                "    end\n" +
                "    return 1\n"+
                "end\n" +
                "return 0"
        );
        READ_COUNT_SCRIPT_DECR.setResultType(Long.class);
    }

    public Long incrBlogReadNumRollBack(String []keys){
        // 3. 执行Lua脚本（原子操作）
        return  redisTemplate.execute(
                READ_COUNT_SCRIPT_DECR,
                Arrays.asList(keys)
        );
    }

    // 添加单个成员到 Zset
    public Boolean zAdd(String key, Object value, long score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    public Set<Object> zGet(String key , long score , Integer start, Integer count) {
        return redisTemplate.opsForZSet().rangeByScore(key,0,score,start,count);
    }

    public Set<Object> zGet(String key , long score ) {
        return redisTemplate.opsForZSet().rangeByScore(key,0,score);
    }



    /**
     * 通过时间删除数据
     * @param key
     * @param score
     * @return
     */
    public Long zRem(String key, long score) {
        return redisTemplate.opsForZSet().removeRangeByScore(key,0,score);
    }
    // 批量添加成员到 Zset
    public Long zAdd(String key, Set<ZSetOperations.TypedTuple<Object>> tuples) {
        return redisTemplate.opsForZSet().add(key, tuples);
    }

    // 从 Zset 删除成员
    public Long zRem(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    // 获取 Zset 所有成员（带分数）
    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
    }

}
