package com.terranangel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * TerranAngel Demo 启动类。
 * <p>
 * 【为什么排除数据源/JPA/缓存/ES 自动配置】
 * 框架哲学是"配置驱动、按需装配"：terranangel.*.enabled 默认 false，
 * 不配置对应能力时应用要照常启动。
 * Spring Boot 的 DataSource/JPA/Redis/Cache/ES 自动配置只要 classpath 有对应依赖
 * 就会尝试初始化（数据源没配会报 "Failed to configure a DataSource"），
 * 所以必须在启动类排除，改由 com.terranangel.db.config / cache.config / es.config 手动装配。
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class,
        DataRedisAutoConfiguration.class,
        CacheAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class,
        ElasticsearchClientAutoConfiguration.class,
        DataElasticsearchAutoConfiguration.class,
        DataElasticsearchRepositoriesAutoConfiguration.class
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
