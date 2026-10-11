package com.terranangel.mp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * MySQL 数据访问装配（MyBatis-Plus）。
 * <p>
 * 【开关】terranangel.datasource.mysql.enabled=true 才启用，默认 false 不连库。
 * <p>
 * 【和 JPA 的关系】
 * 本配置只管 MySQL（MyBatis-Plus）；
 * PostgreSQL 那边由 com.terranangel.db.config.PostgresJpaConfig 用 JPA/Hibernate 管。
 * 两个数据源、两个 ORM 完全独立（业务无交集），不会互相干扰。
 * <p>
 * 【注意】用的是 MybatisSqlSessionFactoryBean（不是 MyBatis 原生的 SqlSessionFactoryBean），
 * 这是 MyBatis-Plus 专有的工厂，才能支持 BaseMapper 的 CRUD 增强。
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.datasource.mysql", name = "enabled", havingValue = "true")
@MapperScan(basePackages = "com.terranangel.mp.mapper", sqlSessionTemplateRef = "mysqlSqlSessionTemplate")
public class MybatisPlusConfig {

    @Value("${terranangel.datasource.mysql.url}")
    private String url;

    @Value("${terranangel.datasource.mysql.username}")
    private String username;

    @Value("${terranangel.datasource.mysql.password}")
    private String password;

    @Value("${terranangel.datasource.mysql.max-pool-size:10}")
    private int maxPoolSize;

    /**
     * MySQL 数据源（Hikari 连接池）。
     */
    @Bean("mysqlDataSource")
    public DataSource mysqlDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(2);
        return ds;
    }

    /**
     * MyBatis-Plus 拦截器（分页插件）。
     * <p>
     * MyBatis-Plus 的功能增强靠拦截器实现，
     * 分页查询必须注册 PaginationInnerInterceptor 才能生效。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件，指定数据库类型 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * SqlSessionFactory（MyBatis-Plus 专用工厂）。
     * 扫描 mp.entity 包下的实体（@TableName）和 mp.mapper 下的 Mapper 接口。
     */
    @Bean("mysqlSqlSessionFactory")
    public SqlSessionFactory mysqlSqlSessionFactory(
            @Qualifier("mysqlDataSource") DataSource dataSource,
            MybatisPlusInterceptor interceptor) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        // 实体包（@TableName 所在包）
        factory.setTypeAliasesPackage("com.terranangel.mp.entity");
        // 本项目用注解方式写 SQL（@Select 等），不设 mapperLocations，避免"资源未找到"的 WARN。
        // 如果以后要用 XML 写 SQL，把 XML 放到 src/main/resources/mapper/ 下并在这里指定即可。
        // 注册 MyBatis-Plus 拦截器（分页等）
        factory.setPlugins(interceptor);
        return factory.getObject();
    }

    /**
     * SqlSessionTemplate（线程安全的 SqlSession 封装，Mapper 实际用它操作数据库）。
     */
    @Bean("mysqlSqlSessionTemplate")
    public SqlSessionTemplate mysqlSqlSessionTemplate(
            @Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 事务管理器（MyBatis 用 DataSourceTransactionManager 管 MySQL 事务）。
     * <p>
     * 注意：JPA/PG 那边的事务管理器叫 transactionManager（默认名），
     * 这里是两个独立的事务管理器，操作 MySQL 时必须显式指定：
     * @Transactional(transactionManager = "mysqlTransactionManager")
     */
    @Bean("mysqlTransactionManager")
    public PlatformTransactionManager mysqlTransactionManager(
            @Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
