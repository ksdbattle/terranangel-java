package com.terranangel.flink.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * 用户行为事件（数据模型）——模拟实时数据流里的"一条数据"。
 * <p>
 * 这是 Flink 作业里的核心数据对象，对应业务里的：
 *   - 用户点击/浏览事件（推荐、画像）
 *   - 订单/交易事件（数仓、风控）
 *   - 日志/指标事件（监控、告警）
 * <p>
 * 用 JSON 序列化/反序列化（方便和 Kafka 配合）。
 */
public class UserEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型：click(点击)、order(下单)、pay(支付)、login(登录) */
    private String type;
    /** 用户 id */
    private Long userId;
    /** 商品 id（点击/订单时用） */
    private Long productId;
    /** 金额（支付时用） */
    private Double amount;
    /** 事件时间戳（毫秒） */
    private Long eventTime;

    public UserEvent() {
    }

    public UserEvent(String type, Long userId, Long productId, Double amount, Long eventTime) {
        this.type = type;
        this.userId = userId;
        this.productId = productId;
        this.amount = amount;
        this.eventTime = eventTime;
    }

    /** 工厂方法：生成一条点击事件 */
    public static UserEvent click(Long userId, Long productId) {
        return new UserEvent("click", userId, productId, 0.0, System.currentTimeMillis());
    }

    /** 工厂方法：生成一条支付事件 */
    public static UserEvent pay(Long userId, Double amount) {
        return new UserEvent("pay", userId, null, amount, System.currentTimeMillis());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {
        return "UserEvent{type=" + type + ", userId=" + userId + ", productId=" + productId
                + ", amount=" + amount + ", eventTime=" + Instant.ofEpochMilli(eventTime) + "}";
    }
}
