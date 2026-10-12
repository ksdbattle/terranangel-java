package com.terranangel.db.repository;

import com.terranangel.db.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓库（DDD Repository 层，接口即 DAO）。
 * <p>
 * Spring Data JPA：只要继承 JpaRepository，就自动获得全套 CRUD 方法
 * （save/findById/findAll/deleteById 等），不用写实现。
 * <p>
 * 同时可以按"方法名规范"或 @Query 定义自定义查询。
 * <p>
 * 【为什么查询方法要加 @EntityGraph】
 * User.orders 是 @OneToMany 默认 LAZY，而本项目排除了 HibernateJpaAutoConfiguration、
 * 手工装配 JPA，没有 open-in-view（OSIV）。Controller 把实体序列化成 JSON 时，
 * 事务和会话都已结束，再去访问未加载的 orders 集合就会抛
 * LazyInitializationException。
 * 用 @EntityGraph 让这些查询在 SQL 层就把 orders 一并 join 出来，
 * 既不依赖 OSIV，也不用为了序列化把业务逻辑裹进事务里。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 查全部用户（连带 orders，避免序列化时懒加载失败） */
    @Override
    @EntityGraph(attributePaths = "orders")
    List<User> findAll();

    /** 按用户名精确查询（方法名规范自动生成 SQL：findByUsername） */
    @EntityGraph(attributePaths = "orders")
    Optional<User> findByUsername(String username);

    /**
     * 按 id 查单个用户（连带 orders）。
     * <p>
     * 【为什么必须覆盖】父接口 JpaRepository 的 findById 不带 fetch，
     * 返回的 User.orders 是未初始化的懒加载代理。下单接口拿到它之后虽然调用了
     * user.addOrder(...)，但对未初始化的 bag，Hibernate 会把 add 挂起（queue）而
     * 不去查库，集合始终是未初始化状态；等事务提交、Session 关闭后，
     * 序列化成 JSON 时再访问 orders 就抛 LazyInitializationException。
     */
    @Override
    @EntityGraph(attributePaths = "orders")
    Optional<User> findById(Long id);

    /** 按邮箱模糊查询 */
    List<User> findByEmailContaining(String email);

    /** 按年龄范围查询 */
    List<User> findByAgeBetween(Integer minAge, Integer maxAge);

    /**
     * 自定义 JPQL 查询：查出年龄大于某值的用户（演示 @Query 写法）。
     * <p>
     * 用 DISTINCT 是因为 fetch join 一对多集合会产生重复行。
     */
    @EntityGraph(attributePaths = "orders")
    @Query("SELECT DISTINCT u FROM User u WHERE u.age > :minAge")
    List<User> findOlderThan(@Param("minAge") Integer minAge);
}
