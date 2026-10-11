package com.terranangel.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kafka 测试接口：提供 HTTP 入口触发生产消息。
 * 由 terranangel.kafka.enabled 开关控制，没启用时整个不装配。
 */
@RestController
@ConditionalOnProperty(prefix = "terranangel.kafka", name = "enabled", havingValue = "true")
public class KafkaController {

    private final KafkaProducer kafkaProducer;

    public KafkaController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * 发送消息（带 id，方便演示幂等消费）。
     * 访问：/kafka/send?message=hello
     */
    @GetMapping("/kafka/send")
    public String send(@RequestParam(defaultValue = "hello") String message) {
        // 格式：id:xxx;data:xxx（消费端用 id 做幂等）
        String msgId = String.valueOf(System.currentTimeMillis());
        kafkaProducer.sendWithKey("terranangel-topic", msgId, "id:" + msgId + ";data:" + message);
        return "已发送: id=" + msgId + ", message=" + message;
    }

    /**
     * 带回调发送，看发送结果（分区、offset）。
     */
    @GetMapping("/kafka/send/callback")
    public String sendCallback(@RequestParam(defaultValue = "hello") String message) {
        kafkaProducer.sendWithCallback("terranangel-topic", "id:" + System.currentTimeMillis() + ";data:" + message);
        return "已发送（带回调）: " + message;
    }
}
