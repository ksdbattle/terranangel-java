package com.terranangel.db.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（属于 User 聚合根下的子实体，多对一关联 User）。
 * <p>
 * DDD 里 Order 是 User 聚合的成员，外键 user_id 指向用户。
 */
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号 */
    @Column(name = "order_no", nullable = false, unique = true, length = 40)
    private String orderNo;

    /** 订单金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 下单时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 所属用户（外键，多方维护关联） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Order() {
    }

    public Order(String orderNo, BigDecimal amount) {
        this.orderNo = orderNo;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== getter/setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
