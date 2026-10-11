package com.terranangel.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 分布式锁（SETNX 实现）。
 * <p>
 * 【和 RedisUniqueService 的区别】
 *   RedisUniqueService —— 只判断"key 是否首次出现"，不关心是谁设置的
 *   RedisLockService   —— 加锁后必须由"持有者"释放，所以要认领归属（token）
 * <p>
 * 【核心命令】SET key value NX EX seconds —— 一条命令完成"不存在才设置 + 带过期"，天然原子。
 * 拆成 SETNX + EXPIRE 两条命令就有坑：中间宕机 → 锁永不过期 → 死锁。
 * <p>
 * 【为什么 value 用随机 token】
 * 释放锁时必须确认"这把锁是我加的"。如果直接 DEL，可能出现：
 *   A 加锁 → A 业务超时锁自动过期 → B 加锁成功 → A 业务结束 DEL → 把 B 的锁删了。
 * 所以 value 存 token，释放时先比对再删。
 * <p>
 * 【为什么释放用 Lua】
 * "比对 value" + "删除 key" 两步如果分开执行，中间又存在并发窗口。
 * 用 Lua 脚本让 Redis 单线程原子执行整个判断+删除。
 * <p>
 * 【开关】terranangel.cache.enabled=true 才装配。
 */
@Service
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
public class RedisLockService {

    /**
     * 释放锁脚本：value 等于自己的 token 才删，否则不动。
     * KEYS[1] = 锁 key，ARGV[1] = 自己的 token。
     * 返回 1 表示删除成功（锁是自己加的），0 表示不是自己的锁（不删）。
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试加锁（SETNX + 过期，原子）。
     *
     * @param key 锁 key
     * @param ttl 锁自动过期时间（兜底，防止业务崩溃后死锁）
     * @return 加锁成功返回持有者 token（解锁时要用），失败返回 null
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 释放锁（Lua 原子校验，只删自己加的锁）。
     *
     * @return true 表示确实释放了自己的锁；false 表示锁已过期或已被别人持有
     */
    public boolean unlock(String key, String token) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        return result != null && result > 0;
    }
}
