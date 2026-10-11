package com.terranangel.db.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * PostgreSQL 数据访问装配（Spring Data JPA + Hibernate）。
 * <p>
 * 【开关】terranangel.datasource.postgres.enabled=true 才启用，
 * 默认 false 不连库，应用照常启动。
 * <p>
 * 【和 MyBatis-Plus 的关系】
 * 本配置只管 PostgreSQL（JPA/Hibernate）；
 * MySQL 那边由 com.terranangel.mp.config.MybatisPlusConfig 用 MyBatis-Plus 管。
 * 两个数据源、两个 ORM 完全独立，业务无交集，不会互相干扰。
 * <p>
 * 【全部用 @Qualifier 精确指定数据源】，不用 @Primary，避免多个 DataSource 时搞混。
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.datasource.postgres", name = "enabled", havingValue = "true")
@EnableJpaRepositories(
        basePackages = "com.terranangel.db.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class PostgresJpaConfig {

    @Value("${terranangel.datasource.postgres.url}")
    private String url;

    @Value("${terranangel.datasource.postgres.username}")
    private String username;

    @Value("${terranangel.datasource.postgres.password}")
    private String password;

    @Value("${terranangel.datasource.postgres.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${terranangel.datasource.postgres.generate-ddl:true}")
    private boolean generateDdl;

    /**
     * PostgreSQL 数据源（Hikari 连接池）。
     */
    @Bean("postgresDataSource")
    public DataSource postgresDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(2);
        return ds;
    }

    /**
     * JPA EntityManagerFactoryBuilder（手动创建，因为排除了 JPA 自动配置）。
     */
    @Bean("postgresEntityManagerFactoryBuilder")
    public EntityManagerFactoryBuilder postgresEntityManagerFactoryBuilder() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(org.springframework.orm.jpa.vendor.Database.POSTGRESQL);
        vendorAdapter.setGenerateDdl(generateDdl);

        return new EntityManagerFactoryBuilder(
                vendorAdapter,
                dataSource -> java.util.Collections.emptyMap(),
                null,
                null
        );
    }

    /**
     * JPA EntityManagerFactory（扫描 db.entity 包下的领域模型）。
     * <p>
     * 【为什么用默认 bean 名 entityManagerFactory】
     * Spring Data JPA 内部（jpaSharedEM_entityManagerFactory 等）按默认名查找，
     * 改了名字会报 "required a bean named 'entityManagerFactory'"。
     * 而 MyBatis-Plus 用的是 SqlSessionFactory（类型不同），不会冲突，所以不用改名区分。
     */
    @Bean("entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("postgresDataSource") DataSource dataSource,
            @Qualifier("postgresEntityManagerFactoryBuilder") EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.terranangel.db.entity")
                .persistenceUnit("postgres")
                .build();
    }

    /**
     * 事务管理器（JPA 用它管理 PostgreSQL 事务）。
     * <p>
     * 【为什么用默认 bean 名 transactionManager】
     * @Transactional 不指定名字时按默认名/唯一候选查找，
     * 用默认名可以让 JPA 相关的 @Transactional 直接生效。
     * MyBatis-Plus 的事务管理器叫 mysqlTransactionManager，使用时显式指定即可。
     */
    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
