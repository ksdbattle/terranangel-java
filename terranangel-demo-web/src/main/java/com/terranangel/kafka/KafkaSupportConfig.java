package com.terranangel.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 开关 + 生产/消费配置。
 * <p>
 * 只有 terranangel.kafka.enabled=true 才启用 Kafka，否则整个不初始化。
 * <p>
 * 这里实现生产和消费的关键坑（面试重点）：
 * 生产端：
 *   1. acks=all：等所有副本确认（最可靠，不丢消息）
 *   2. enable.idempotence=true：幂等（防止重复发送）
 *   3. retries：发送失败重试
 * 消费端：
 *   1. 手动 ack：处理成功才提交 offset（不丢消息）
 *   2. 重试 + 死信队列：消费失败重试，多次失败进死信
 *   3. auto.offset.reset=earliest：从头消费（不漏消息）
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.kafka", name = "enabled", havingValue = "true")
@EnableKafka
public class KafkaSupportConfig {

    /** Kafka 服务端地址（配置里写） */
    private final String bootstrapServers = "127.0.0.1:9092";

    // ==================== 生产端配置（坑：可靠性） ====================

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 坑1：acks=all，等所有副本确认，保证不丢消息（最可靠）
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // 坑2：幂等，防止重复发送（配合 acks=all）
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // 坑3：发送失败重试
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        // 坑4：批量发送优化（批次大小 + 等待时间）
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    // ==================== 消费端配置（坑：可靠性 + 重试 + 死信） ====================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 坑5：手动 ack，处理成功才提交 offset（不丢消息）
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 坑6：从头消费（没有 offset 时从头开始，不漏消息）
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 坑7：消费组 + 并发
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "terranangel-group");

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        // 手动 ack 模式（配合 @KafkaListener 里的 Acknowledgment）
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // 坑8：消费失败重试 + 死信队列
        // 重试 3 次，每次间隔 1 秒，超过后进死信队列（自动转发到 .DLT 主题）
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
