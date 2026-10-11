package com.terranangel.es;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品搜索演示接口（Elasticsearch）。
 * <p>
 * 【前提】terranangel.es.enabled=true 且本地 ES 已启动（默认 9200）。
 * <p>
 * 【测试入口】
 *   POST   /es/product/init                          一键灌入演示数据
 *   POST   /es/product?name=...&brand=...            新增商品
 *   GET    /es/product/{id}                          按 id 查
 *   GET    /es/product/all                           查全部
 *   GET    /es/product/search?keyword=iPhone         关键字搜索商品名（分词）
 *   GET    /es/product/brand/Apple                   按品牌精确查
 *   GET    /es/product/price?min=3000&max=9000       按价格区间查
 *   DELETE /es/product/{id}                          删除
 */
@RestController
@RequestMapping("/es/product")
@ConditionalOnProperty(prefix = "terranangel.es", name = "enabled", havingValue = "true")
public class EsProductController {

    private final EsProductService esProductService;

    public EsProductController(EsProductService esProductService) {
        this.esProductService = esProductService;
    }

    /** 一键灌入演示数据（最方便，先调这个） */
    @PostMapping("/init")
    public String init() {
        return esProductService.initDemoData();
    }

    /** 新增商品 */
    @PostMapping
    public EsProduct save(@RequestParam String id,
                          @RequestParam String name,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String brand,
                          @RequestParam(required = false) Double price,
                          @RequestParam(required = false) Integer stock) {
        return esProductService.save(new EsProduct(id, name, description, brand, price, stock));
    }

    /** 按 id 查 */
    @GetMapping("/{id}")
    public EsProduct getById(@PathVariable String id) {
        return esProductService.getById(id);
    }

    /** 查全部 */
    @GetMapping("/all")
    public List<EsProduct> listAll() {
        return esProductService.listAll();
    }

    /** 关键字搜索商品名（体验 ES 分词搜索） */
    @GetMapping("/search")
    public List<EsProduct> search(@RequestParam String keyword) {
        return esProductService.searchByKeyword(keyword);
    }

    /** 按品牌精确查 */
    @GetMapping("/brand/{brand}")
    public List<EsProduct> findByBrand(@PathVariable String brand) {
        return esProductService.findByBrand(brand);
    }

    /** 按价格区间查 */
    @GetMapping("/price")
    public List<EsProduct> findByPriceBetween(@RequestParam Double min,
                                              @RequestParam Double max) {
        return esProductService.findByPriceBetween(min, max);
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        esProductService.delete(id);
        return "删除成功: " + id;
    }
}
