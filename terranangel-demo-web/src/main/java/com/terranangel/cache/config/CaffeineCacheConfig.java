package com.terranangel.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置（进程内缓存）。
 * <p>
 * 【开关】terranangel.cache.enabled=true 就装配（与 Redis 各自独立，各管各的业务）。
 * <p>
 * 【用法】固定 bean 名 caffeineCacheManager，业务方法上显式指定：
 *   @Cacheable(cacheNames = "xxx", cacheManager = "caffeineCacheManager")
 * <p>
 * 【定位】进程内、单机最快；不适合多实例间需要共享/一致的数据（那用 Redis）。
 * <p>
 * 【面试点】
 *   1. 本地缓存 vs 分布式缓存：Caffeine 是进程内，Redis 是跨进程共享
 *   2. 缓存淘汰策略：LRU/LFU（Caffeine 用 Window-TinyLFU，命中率高）
 *   3. 过期时间 + 最大容量：防止内存无限增长
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
public class CaffeineCacheConfig {

    /** 默认过期时间（秒），从配置读取 */
    @Value("${terranangel.cache.ttl-seconds:3600}")
    private long ttlSeconds;

    /** 最大缓存条目数 */
    @Value("${terranangel.cache.max-size:1000}")
    private long maxSize;

    /**
     * Caffeine CacheManager（固定 bean 名 caffeineCacheManager）。
     * <p>
     * 关键配置：
     *   - expireAfterWrite：写入后过期（缓存一致性，比 access 更适合业务）
     *   - maximumSize：最大条目，防止内存溢出
     *   - recordStats：记录命中率统计
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)   // 写入后过期
                .maximumSize(maxSize)                              // 最大容量
                .recordStats());                                   // 记录命中率统计
        return cacheManager;
    }
}
