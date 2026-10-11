package com.terranangel.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 生产方（Producer）。
 * <p>
 * 由 terranangel.kafka.enabled 开关控制：
 *   - enabled=false（默认）→ 不装配，不初始化，Spring 照常启动
 *   - enabled=true        → 装配，可以发送消息
 * <p>
 * 生产端的坑（面试重点）：
 *   1. ack 确认机制（在配置里控制）
 *   2. 幂等性 enable.idempotence（防止重复发送）
 *   3. 按 key 分区保证同 key 有序
 *   4. 发送回调（异步结果处理）
 */
@Component
@ConditionalOnProperty(prefix = "terranangel.kafka", name = "enabled", havingValue = "true")
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送消息（指定 key，保证同 key 消息有序到同一分区）。
     * 坑：同 key 会哈希到同一分区，保证该 key 的消息有序。
     */
    public void sendWithKey(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
    }

    /**
     * 发送消息（不指定 key，轮询分布到不同分区，无法保证全局有序）。
     */
    public void send(String topic, Object message) {
        kafkaTemplate.send(topic, message);
    }

    /**
     * 发送消息 + 回调（获取发送结果，处理成功/失败）。
     * 坑：异步发送必须用回调确认结果，不能"发了就不管"。
     */
    public void sendWithCallback(String topic, Object message) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);
        // 显式标注 lambda 参数类型，避免泛型推断退化导致 result 变成 Object
        future.whenComplete((SendResult<String, Object> result, Throwable ex) -> {
            if (ex != null) {
                System.err.println("[Kafka 生产] 发送失败: " + ex.getMessage());
            } else {
                System.out.println("[Kafka 生产] 发送成功, 分区: " + result.getRecordMetadata().partition()
                        + ", offset: " + result.getRecordMetadata().offset());
            }
        });
    }
}
