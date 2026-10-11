package com.terranangel.flink.jobs;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;

import java.util.Properties;

/**
 * 【场景 4】Kafka 实时接入作业 —— 字节面试高频：Flink + Kafka 实时链路。
 * <p>
 * 业务场景：从 Kafka 消费消息（实时流），解析后打印/下沉。
 * 这是字节最常见的实时链路：业务 → Kafka → Flink → 下游。
 * <p>
 * 【覆盖的 Flink 核心概念（面试）】
 *   1. KafkaSource：Flink 从 Kafka 读数据（新 API）
 *   2. 消费起始位置：earliest（从头）
 *   3. Sink：结果写回 Kafka / 打印
 * <p>
 * 【运行前提】本地要有 Kafka（127.0.0.1:9092），且有 topic "terranangel-topic"
 * 【运行】IDEA 跑 JobLauncher，参数传 "kafka"。
 */
public class KafkaStreamJob {

    public static void execute(StreamExecutionEnvironment env) throws Exception {
        // ===== 1. Kafka Source：从 Kafka 读数据 =====
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "127.0.0.1:9092");
        props.setProperty("group.id", "flink-group");

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers("127.0.0.1:9092")
                .setTopics("terranangel-topic")          // 消费的主题
                .setGroupId("flink-group")               // 消费组
                .setStartingOffsets(OffsetsInitializer.earliest())  // 从头消费
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> kafkaStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source");

        // ===== 2. 处理：打印 Kafka 消息 =====
        // 真实场景：这里做解析、窗口、过滤、聚合等计算
        DataStream<String> processed = kafkaStream
                .map(new MapFunction<String, String>() {
                    @Override
                    public String map(String msg) {
                        return "[Kafka-Flink] 收到消息: " + msg;
                    }
                });

        // ===== 3. Sink：打印 + 写回 Kafka（真实场景可写 DB）=====
        processed.print();  // 打印到控制台

        // 写回 Kafka（结果 topic）
        org.apache.flink.connector.kafka.sink.KafkaSink<String> kafkaSink =
                org.apache.flink.connector.kafka.sink.KafkaSink.<String>builder()
                        .setBootstrapServers("127.0.0.1:9092")
                        .setRecordSerializer(
                                org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema.builder()
                                        .setTopic("terranangel-result-topic")
                                        .setValueSerializationSchema(new SimpleStringSchema())
                                        .build())
                        .build();
        processed.sinkTo(kafkaSink);

        System.out.println("[Flink] Kafka 实时接入作业已启动，等待 Kafka 消息...");
    }
}
