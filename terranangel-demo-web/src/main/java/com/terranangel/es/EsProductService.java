package com.terranangel.es;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品搜索服务（Elasticsearch 演示）。
 * <p>
 * 【用法类比 JPA】和 UserAppService 一个套路，只是底层换成了 ES 索引。
 * <p>
 * 【开关】terranangel.es.enabled=true 才装配，否则应用照常启动。
 */
@Service
@ConditionalOnProperty(prefix = "terranangel.es", name = "enabled", havingValue = "true")
public class EsProductService {

    private final EsProductRepository repository;

    public EsProductService(EsProductRepository repository) {
        this.repository = repository;
    }

    /** 新增/更新商品（id 相同就是覆盖更新） */
    public EsProduct save(EsProduct product) {
        return repository.save(product);
    }

    /** 按 id 查商品 */
    public EsProduct getById(String id) {
        return repository.findById(id).orElse(null);
    }

    /** 查全部 */
    public List<EsProduct> listAll() {
        List<EsProduct> list = new ArrayList<>();
        repository.findAll().forEach(list::add);
        return list;
    }

    /** 按商品名搜索（分词） */
    public List<EsProduct> searchByName(String name) {
        return repository.findByName(name);
    }

    /** 按品牌精确查（Keyword 字段） */
    public List<EsProduct> findByBrand(String brand) {
        return repository.findByBrand(brand);
    }

    /** 按价格区间查 */
    public List<EsProduct> findByPriceBetween(Double min, Double max) {
        return repository.findByPriceBetween(min, max);
    }

    /** 关键字搜索商品名（模糊/分词匹配） */
    public List<EsProduct> searchByKeyword(String keyword) {
        return repository.findByNameContaining(keyword);
    }

    /** 删除商品 */
    public void delete(String id) {
        repository.deleteById(id);
    }

    /**
     * 初始化演示数据（一键灌入几条商品，方便测试）。
     * 真实项目里数据来自数据库同步（如 Canal 监听 MySQL binlog 同步到 ES）。
     */
    public String initDemoData() {
        List<EsProduct> products = List.of(
                new EsProduct("1", "iPhone 17 Pro", "苹果最新款智能手机，A19 芯片", "Apple",
                        8999.0, 100),
                new EsProduct("2", "iPhone 17", "苹果标准版智能手机", "Apple",
                        5999.0, 200),
                new EsProduct("3", "小米 16", "小米旗舰手机，性价比高", "Xiaomi",
                        3999.0, 300),
                new EsProduct("4", "MacBook Pro", "苹果专业笔记本电脑，M5 芯片", "Apple",
                        14999.0, 50),
                new EsProduct("5", "小米电视", "小米 65 寸 4K 电视", "Xiaomi",
                        2999.0, 80)
        );
        repository.saveAll(products);
        return "已初始化 " + products.size() + " 条商品数据";
    }
}
