package org.oyyj.adminservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * @param redisConnectionFactory 自动装配 通过 yml文件中的设置
     * @return
     */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        /*
        * 由于 Redis 存储的数据是以字节数组形式存在的，而 Java 程序使用的是各种对象和数据类型，所以需要通过序列化器将 Java 对象转换为字节数组存入 Redis，
        * 从 Redis 读取数据时再将字节数组转换回 Java 对象。*/


        // 设置ObjectMapper 在转化内java对象是会使用
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY); // 配置序列化时的可见规则
        // 参数1是所有属性包括方法 参数2是所有修饰包括public private等
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        /* 第一个参数是一个 SubTypeValidator 类型的对象，用于验证子类型是否允许被序列化和反序列化。此处参数 instance
             表示LaissezFaireSubTypeValidator的一个实例 代表不对子类进行任何验证
         第二个参数是 ObjectMapper.DefaultTyping 枚举类型，用于指定哪些类型需要添加类型信息。
             此处表示对于任何非final定义的字段 都会在序列化时自动添加类型信息导json中*/
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();// 处理字符串的序列化器
        Jackson2JsonRedisSerializer<Object> objectJackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
        // 构建序列化器 通过传入参数 mapper 指定序列化的格式


        // 设置redis的序列化器
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer); // 设置hash数据结构的序列化器
        template.setValueSerializer(objectJackson2JsonRedisSerializer);
        template.setHashValueSerializer(objectJackson2JsonRedisSerializer);

        template.afterPropertiesSet(); // 初始化
        return template;
    }

}
