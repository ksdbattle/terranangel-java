package com.terranangel.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费方（Listener）——完整演示消费端的坑。
 * <p>
 * 由 terranangel.kafka.enabled 开关控制：
 *   - enabled=false（默认）→ 不装配，不初始化，Spring 照常启动
 *   - enabled=true        → 装配，监听消息
 * <p>
 * 消费端的坑（面试重点）：
 *   1. 手动 ack：处理成功才 acknowledge()，失败不 ack 触发重投（不丢消息）
 *   2. 幂等消费：同一消息可能重复消费，必须幂等（用已处理 key 去重）
 *   3. 重试：配合 ErrorHandler / 异常重投（在配置里控制）
 *   4. offset 提交：手动提交，防止"处理完但没提交就挂了"导致消息丢失
 */
@Component
@ConditionalOnProperty(prefix = "terranangel.kafka", name = "enabled", havingValue = "true")
public class KafkaConsumer {

    /**
     * 幂等消费的"已处理"记录（简单演示用 HashSet，生产用 Redis/DB 做去重）。
     * 坑：Kafka 消费不保证 exactly-once（至少一次），同一消息可能重复投递，必须幂等。
     */
    private final java.util.Set<String> processedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 手动 ack 消费：处理成功才 acknowledge。
     * 坑：消费失败不 ack，Kafka 会重新投递该消息（保证不丢）。
     * 坑：幂等——即使重投，也要保证处理结果一致。
     */
    @KafkaListener(topics = "terranangel-topic", groupId = "terranangel-group", concurrency = "3")
    public void onMessage(String message, Acknowledgment ack) {
        // 幂等判断：消息带 id，处理过的跳过
        String msgId = extractId(message);
        if (msgId != null && processedKeys.contains(msgId)) {
            System.out.println("[Kafka 消费] 重复消息跳过（幂等）: " + msgId);
            ack.acknowledge();  // 重复消息也要 ack，否则无限重投
            return;
        }

        try {
            // 真正的业务处理
            System.out.println("[Kafka 消费] 处理消息: " + message);
            // 模拟业务处理
            Thread.sleep(100);

            // 记录已处理（幂等）
            if (msgId != null) {
                processedKeys.add(msgId);
            }
            // 处理成功，手动 ack（提交 offset）
            ack.acknowledge();
            System.out.println("[Kafka 消费] 处理成功并 ack");

        } catch (Exception e) {
            // 处理失败：不 ack，Kafka 会重新投递（重试机制）
            // 坑：一直失败会无限重投，生产要配"重试次数 + 死信队列"
            System.err.println("[Kafka 消费] 处理失败，不 ack（将重投）: " + e.getMessage());
            // 这里不调 ack.acknowledge()，让 Kafka 重新投递
        }
    }

    /**
     * 从消息里提取消息 id（用于幂等）。
     * 简单实现：假设消息格式是 "id:xxx;data:xxx"。
     */
    private String extractId(String message) {
        if (message == null || !message.startsWith("id:")) {
            return null;
        }
        int semicolon = message.indexOf(';');
        return semicolon > 0 ? message.substring(3, semicolon) : message.substring(3);
    }
}
