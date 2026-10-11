package com.terranangel.db.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户聚合根（DDD 领域模型）。
 * <p>
 * 这是领域模型（Domain Model），通过 JPA @Entity 映射到数据库表 t_user。
 * <p>
 * 聚合根含义：User 是一组相关对象的聚合入口，下面挂 Order（一对多）。
 * 领域模型里可以包含业务行为（方法），不只是字段容器（贫血模型 vs 充血模型）。
 */
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（唯一） */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 邮箱 */
    @Column(length = 100)
    private String email;

    /** 年龄 */
    private Integer age;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 一对多：用户的订单列表（由 Order 的 user 字段维护外键） */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    /** 构造函数 */
    public User() {
    }

    public User(String username, String email, Integer age) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 领域方法：给用户加一个订单（维护聚合内部一致性，而不是外部直接操作 orders）
     */
    public void addOrder(Order order) {
        this.orders.add(order);
        order.setUser(this);
    }

    // ==================== getter/setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}
