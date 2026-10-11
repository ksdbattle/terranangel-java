package com.terranangel.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spring Cache 缓存演示服务。
 * <p>
 * 【演示核心】统一注解，不用关心底层是 Redis 还是 Caffeine：
 *   @Cacheable  —— 先查缓存，命中直接返回，没命中执行方法并缓存
 *   @CachePut   —— 每次执行方法，并更新缓存（常用于"写后更新"）
 *   @CacheEvict —— 删除缓存（常用于"删除后清缓存"）
 * <p>
 * 【场景】模拟查询商品价格：第一次查走"数据库"（这里模拟随机价格 + 延迟），
 *         之后查走缓存，直接返回，不打"数据库"。
 * <p>
 * 【开关】terranangel.cache.enabled=true 才装配，否则应用照常启动。
 */
@Service
@ConditionalOnProperty(prefix = "terranangel.cache", name = "enabled", havingValue = "true")
public class CacheDemoService {

    /**
     * 查询商品价格（@Cacheable）。
     * 第一次查缓存没命中 → 执行方法（模拟慢查询）→ 缓存结果；
     * 之后查缓存命中 → 直接返回，不执行方法。
     */
    @Cacheable(cacheNames = "product", key = "#productId")
    public String getPrice(Long productId) {
        // 模拟数据库慢查询（打日志能看出走了几次 DB）
        System.out.println("[缓存演示] 未命中缓存，查数据库获取商品 " + productId + " 价格...");
        // 模拟随机价格 + 模拟耗时
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        return "商品" + productId + "价格: " + (100 + ThreadLocalRandom.current().nextInt(900)) + "元";
    }

    /**
     * 更新价格（@CachePut）。
     * 每次执行方法（更新"数据库"），并更新缓存，保证缓存与数据库一致。
     */
    @CachePut(cacheNames = "product", key = "#productId")
    public String updatePrice(Long productId, Integer newPrice) {
        System.out.println("[缓存演示] 更新商品 " + productId + " 价格为 " + newPrice);
        return "商品" + productId + "价格: " + newPrice + "元";
    }

    /**
     * 删除商品（@CacheEvict）。
     * 删除缓存，下次查询重新走数据库。
     */
    @CacheEvict(cacheNames = "product", key = "#productId")
    public String deletePrice(Long productId) {
        System.out.println("[缓存演示] 删除商品 " + productId + " 的缓存");
        return "已删除商品 " + productId + " 缓存";
    }

    // ==================== 演示：两个缓存各管各的业务 ====================
    // 不写 cacheManager 时走默认的 redisCacheManager（@Primary）；
    // 想走本地缓存，就显式写 cacheManager = "caffeineCacheManager"。
    // 注意：同一个业务的 @Cacheable/@CachePut/@CacheEvict 必须指定同一个 cacheManager，否则清不掉。

    /**
     * 强制走 Caffeine（本地缓存）。
     * 场景：热点数据、单机就能满足，不需要跨服务共享 → 走本地最快。
     */
    @Cacheable(cacheNames = "localProduct", key = "#productId", cacheManager = "caffeineCacheManager")
    public String getPriceFromCaffeine(Long productId) {
        System.out.println("[缓存演示-Caffeine] 未命中本地缓存，查数据库获取商品 " + productId);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        return "商品" + productId + "本地缓存价格: " + (100 + ThreadLocalRandom.current().nextInt(900)) + "元";
    }

    /**
     * 强制走 Redis（分布式缓存）。
     * 场景：多个服务实例共享缓存、需要跨进程一致 → 走 Redis。
     */
    @Cacheable(cacheNames = "remoteProduct", key = "#productId", cacheManager = "redisCacheManager")
    public String getPriceFromRedis(Long productId) {
        System.out.println("[缓存演示-Redis] 未命中 Redis 缓存，查数据库获取商品 " + productId);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        return "商品" + productId + "Redis 缓存价格: " + (100 + ThreadLocalRandom.current().nextInt(900)) + "元";
    }
}
