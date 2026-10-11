package com.terranangel.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 商品文档（Elasticsearch 文档模型）。
 * <p>
 * 【类比】和 JPA 的 @Entity 是同一个套路：
 *   @Entity  + @Table      → 映射数据库表
 *   @Document + @Field     → 映射 ES 索引
 * <p>
 * 【关键注解】
 *   @Document(indexName = "product")  —— 指定 ES 索引名（相当于表名）
 *   @Id                              —— 文档唯一标识
 *   @Field(type = ...)               —— 字段类型和分词设置
 * <p>
 * 【面试点】ES 里两个关键类型：
 *   Text    —— 会分词（全文检索用，如商品名、描述）
 *   Keyword —— 不分词（精确匹配/聚合用，如品牌、分类）
 */
@Document(indexName = "product")
public class EsProduct {

    /** 文档 ID */
    @Id
    private String id;

    /** 商品名（Text：分词，支持全文搜索） */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    /** 商品描述（Text：分词，支持全文搜索） */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    /** 品牌（Keyword：不分词，精确匹配/聚合） */
    @Field(type = FieldType.Keyword)
    private String brand;

    /** 价格（用 Double 而不是 BigDecimal：ES 的 Double 类型本来就是这个，
     *  而且 Java 高版本模块系统不允许反射 BigDecimal 内部字段，会报 InaccessibleObjectException） */
    @Field(type = FieldType.Double)
    private Double price;

    /** 库存 */
    @Field(type = FieldType.Integer)
    private Integer stock;

    public EsProduct() {
    }

    public EsProduct(String id, String name, String description, String brand, Double price, Integer stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.price = price;
        this.stock = stock;
    }

    // ==================== getter/setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
