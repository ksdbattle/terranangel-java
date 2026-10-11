package com.terranangel.cache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache 总开关。
 * <p>
 * 【开关】terranangel.cache.enabled=true 才启用缓存注解（@Cacheable/@CachePut/@CacheEvict）。
 * 默认 false：不开缓存，应用照常启动（符合框架"按需装配"哲学）。
 * <p>
 * 【两个实现，各管各的业务，不做多级嵌套】
 *   - caffeineCacheManager —— Caffeine 本地缓存（进程内）
 *   - redisCacheManager    —— Redis 分布式缓存（跨实例，@Primary 默认）
 * <p>
 * 用法（业务方法上显式指定，用哪个写哪个）：
 *   @Cacheable(cacheNames = "x", cacheManager = "caffeineCacheManager")  → 走本地
 *   @Cacheable(cacheNames = "y", cacheManager = "redisCacheManager")     → 走 Redis
 *   @Cacheable(cacheNames = "z")                                          → 不指定，走默认（Redis）
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
@EnableCaching
public class CacheSupportConfig {
}
