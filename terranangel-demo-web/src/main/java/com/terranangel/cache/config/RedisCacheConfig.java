package com.terranangel.cache.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置（分布式缓存）。
 * <p>
 * 【开关】terranangel.cache.enabled=true 就装配（与 Caffeine 各自独立，各管各的业务）。
 * <p>
 * 【用法】固定 bean 名 redisCacheManager，业务方法上显式指定：
 *   @Cacheable(cacheNames = "xxx", cacheManager = "redisCacheManager")
 * 不写 cacheManager 时走它（见下面 @Primary）。
 * <p>
 * 【定位】跨实例共享、需要分布式一致的数据；多实例部署时的主缓存。
 * <p>
 * 【面试点】
 *   1. CacheManager：Spring Cache 抽象的核心，决定缓存实现
 *   2. 序列化：默认 JDK 序列化不可读，这里配 JSON 序列化（可读、跨语言）
 *   3. TTL：缓存默认过期时间
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
public class RedisCacheConfig {

    /** 默认过期时间（秒），从配置读取 */
    @Value("${terranangel.cache.ttl-seconds:3600}")
    private long ttlSeconds;

    /** Redis 地址 */
    @Value("${terranangel.cache.redis.host:127.0.0.1}")
    private String redisHost;

    /** Redis 端口 */
    @Value("${terranangel.cache.redis.port:6379}")
    private int redisPort;

    /**
     * Redis 连接工厂（Lettuce）。
     * <p>
     * 因为启动类排除了 DataRedisAutoConfiguration，所以这里手动创建。
     * 懒连接：启动时不真正连 Redis，只有真正操作时才建立连接。
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Redis CacheManager（固定 bean 名 redisCacheManager，@Primary 默认缓存管理器）。
     * <p>
     * @Primary 作用：@Cacheable 不写 cacheManager 时，默认走 Redis。
     * 想改成默认走本地缓存，把 @Primary 挪到 caffeineCacheManager 上即可。
     * <p>
     * 关键配置：
     *   - key 用 String 序列化（可读）
     *   - value 用 JSON 序列化（可读、跨语言，不用 JDK 默认的乱码）
     *   - 默认 TTL
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // key 序列化：String（可读）
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // value 序列化：JSON（可读、跨语言）
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 默认过期时间
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                // 不缓存 null（防止缓存穿透）
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * StringRedisTemplate（key/value 都是 String 的 Redis 客户端）。
     * <p>
     * 用途：分布式锁（SETNX）、计数器、简单的 String 读写。
     * 用 String 序列化语义最直观，不会像 JSON 那样给 value 加引号。
     * <p>
     * 因为排除了 DataRedisAutoConfiguration，这里手动创建。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * RedisTemplate（Spring Data Redis 核心操作类）。
     * <p>
     * 因为排除了 DataRedisAutoConfiguration，这里手动创建。
     * 序列化配置：key 用 String（可读），value 用 JSON（可读、跨语言）。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // key/hashKey 用 String 序列化（可读）
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // value 用 JSON 序列化（可读、跨语言）
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }
}
