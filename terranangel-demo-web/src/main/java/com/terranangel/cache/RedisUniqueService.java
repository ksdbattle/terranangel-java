package com.terranangel.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 唯一性判断（SETNX）。
 * <p>
 * 【场景】只关心"这个 key 是不是第一次出现"，不需要锁语义。
 * 例如：订单号去重、请求幂等、重复提交拦截、唯一编号占位。
 * <p>
 * 【本质】SET key value NX EX ttl —— 一条原子命令：
 *   key 不存在 → 写入成功 → 返回 1（首次）
 *   key 已存在 → 什么都不做 → 返回 0（重复）
 * Spring Data Redis 把返回的 1/0 包装成 Boolean（true/false）。
 * <p>
 * 【开关】terranangel.cache.enabled=true 才装配。
 */
@Service
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
public class RedisUniqueService {

    private final StringRedisTemplate redisTemplate;

    public RedisUniqueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 标记 + 判断是否首次（SETNX）。
     *
     * @param key 业务唯一标识，如 "order:no:20261009001"
     * @param ttl 标记保留时间，到期后可再次进入
     * @return true = 首次（设置成功）；false = 已存在（重复）
     */
    public boolean markIfAbsent(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
    }

    /** 是否已标记 */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** 移除标记（允许该 key 下次再进来） */
    public boolean remove(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
}
