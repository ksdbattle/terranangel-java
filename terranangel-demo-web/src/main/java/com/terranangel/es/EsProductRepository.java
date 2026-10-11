package com.terranangel.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 商品搜索仓库（Elasticsearch Repository）。
 * <p>
 * 【类比 JPA】用法几乎一样：
 *   JpaRepository<Product, Long>      → MySQL/PG
 *   ElasticsearchRepository<Product, String> → ES 索引
 * <p>
 * 【面试点】ES 的查询方式：
 *   1. 方法名派生（findByName / findByPriceBetween）—— 简单，和 JPA 一致
 *   2. @Query 注解写 ES 原生 DSL —— 复杂查询/聚合
 */
public interface EsProductRepository extends ElasticsearchRepository<EsProduct, String> {

    /** 按商品名搜索（方法名派生，支持分词匹配） */
    List<EsProduct> findByName(String name);

    /** 按品牌精确查询（Keyword 字段） */
    List<EsProduct> findByBrand(String brand);

    /** 按价格区间查询 */
    List<EsProduct> findByPriceBetween(Double min, Double max);

    /** 商品名包含关键字（模糊/分词搜索） */
    List<EsProduct> findByNameContaining(String keyword);
}
