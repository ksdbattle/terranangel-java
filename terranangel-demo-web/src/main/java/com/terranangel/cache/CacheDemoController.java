package com.terranangel.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Spring Cache 缓存演示接口。
 * <p>
 * 【前提】terranangel.cache.enabled=true，且缓存实现可用（redis 需起 Redis）。
 * <p>
 * 【测试入口】
 *   GET  /cache/price/1            查询商品价格（第一次慢，第二次走缓存快）
 *   PUT  /cache/price/1?price=888  更新价格（@CachePut，同步更新缓存）
 *   DELETE /cache/price/1          删除缓存（@CacheEvict）
 *   GET  /cache/local/price/1      强制走 Caffeine 本地缓存
 *   GET  /cache/redis/price/1      强制走 Redis 分布式缓存
 *   GET  /cache/unique/order-1     Redis 唯一性判断（SETNX），重复调用返回"重复"
 *   GET  /cache/lock/order-1       分布式锁（SETNX），并发调用只有一个能执行
 * <p>
 * 【怎么看出缓存生效】
 * 连续 GET 两次：第一次日志打"未命中缓存"，第二次不打（直接走缓存），且速度快。
 */
@RestController
@RequestMapping("/cache")
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
public class CacheDemoController {

    private final CacheDemoService cacheDemoService;
    private final RedisUniqueService redisUniqueService;
    private final RedisLockService redisLockService;

    public CacheDemoController(CacheDemoService cacheDemoService,
                               RedisUniqueService redisUniqueService,
                               RedisLockService redisLockService) {
        this.cacheDemoService = cacheDemoService;
        this.redisUniqueService = redisUniqueService;
        this.redisLockService = redisLockService;
    }

    /** 查询商品价格（走缓存） */
    @GetMapping("/price/{productId}")
    public String getPrice(@PathVariable Long productId) {
        return cacheDemoService.getPrice(productId);
    }

    /** 更新价格（@CachePut：执行方法 + 更新缓存） */
    @PutMapping("/price/{productId}")
    public String updatePrice(@PathVariable Long productId, @RequestParam Integer price) {
        return cacheDemoService.updatePrice(productId, price);
    }

    /** 删除缓存（@CacheEvict） */
    @DeleteMapping("/price/{productId}")
    public String deletePrice(@PathVariable Long productId) {
        return cacheDemoService.deletePrice(productId);
    }

    /** 强制走 Caffeine 本地缓存 */
    @GetMapping("/local/price/{productId}")
    public String getLocal(@PathVariable Long productId) {
        return cacheDemoService.getPriceFromCaffeine(productId);
    }

    /** 强制走 Redis 分布式缓存 */
    @GetMapping("/redis/price/{productId}")
    public String getRemote(@PathVariable Long productId) {
        return cacheDemoService.getPriceFromRedis(productId);
    }

    /**
     * 唯一性判断演示（SETNX）。
     * <p>
     * 同一个 key 第一次调用 → 首次；之后调用（30 分钟内）→ 重复。
     * 试法：连续请求两次 GET /cache/unique/order-1
     */
    @GetMapping("/unique/{bizId}")
    public String unique(@PathVariable String bizId) {
        boolean first = redisUniqueService.markIfAbsent("unique:" + bizId, Duration.ofMinutes(30));
        return first ? "首次，已受理：" + bizId : "重复，已忽略：" + bizId;
    }

    /**
     * 分布式锁演示。
     * <p>
     * 加锁成功 → 模拟 3 秒业务 → 主动释放锁。
     * 3 秒内再并发调用同一 key，会返回"加锁失败"，证明锁生效。
     * <p>
     * 试法：连续快速请求两次 GET /cache/lock/order-1
     *   第一次：加锁成功并执行完业务（约 3 秒）
     *   第二次：加锁失败，已被其他请求持有
     */
    @GetMapping("/lock/{key}")
    public String lock(@PathVariable String key) {
        String lockKey = "lock:" + key;
        String token = redisLockService.tryLock(lockKey, Duration.ofSeconds(10));
        if (token == null) {
            return "加锁失败：[" + key + "] 已被其他请求持有";
        }
        try {
            Thread.sleep(3000);   // 模拟业务耗时
            return "加锁成功，业务执行完毕，token=" + token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "业务执行被中断";
        } finally {
            redisLockService.unlock(lockKey, token);
        }
    }
}
