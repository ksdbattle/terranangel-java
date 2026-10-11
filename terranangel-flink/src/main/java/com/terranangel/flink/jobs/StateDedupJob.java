package com.terranangel.flink.jobs;

import com.terranangel.flink.source.FakeEventSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * 【场景 3】状态去重作业 —— 字节面试高频：Flink 状态管理（State）+ 幂等。
 * <p>
 * 业务场景：实时数据里同一用户可能产生重复事件（如重复点击、重复支付回调），
 * 用 Flink 的 Keyed State（状态）对"同一用户"去重，只处理第一次。
 * <p>
 * 【覆盖的 Flink 核心概念（面试）】
 *   1. Keyed State（ValueState）：按 key 存储状态（这里是"该用户是否处理过"）
 *   2. RichFunction：带状态的函数（open 里初始化状态）
 *   3. 幂等去重：防止重复数据处理（配合 Kafka at-least-once）
 * <p>
 * 【运行】IDEA 跑 JobLauncher，参数传 "dedup"。
 */
public class StateDedupJob {

    public static void execute(StreamExecutionEnvironment env) throws Exception {
        DataStream<String> source = env.addSource(FakeEventSource.create());

        DataStream<com.terranangel.flink.model.UserEvent> events = source
                .map(new MapFunction<String, com.terranangel.flink.model.UserEvent>() {
                    @Override
                    public com.terranangel.flink.model.UserEvent map(String line) {
                        String[] parts = line.split(",");
                        return new com.terranangel.flink.model.UserEvent(
                                parts[0], Long.valueOf(parts[1]), Long.valueOf(parts[2]),
                                Double.valueOf(parts[3]), Long.valueOf(parts[4]));
                    }
                })
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<com.terranangel.flink.model.UserEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withTimestampAssigner((e, ts) -> e.getEventTime()));

        // 按用户 keyBy，用状态去重：同一用户只处理一次"首次点击"
        events
                .filter(e -> "click".equals(e.getType()))
                .keyBy(com.terranangel.flink.model.UserEvent::getUserId)
                .flatMap(new DedupFlatMap())
                .print();

        System.out.println("[Flink] 状态去重作业已启动，同一用户重复事件只处理第一次...");
    }

    /**
     * 去重函数：用 ValueState 记录"该用户是否已处理过"。
     * 面试要点：RichFunction 的 open() 里初始化状态，processElement 里读写状态。
     */
    public static class DedupFlatMap extends RichFlatMapFunction<com.terranangel.flink.model.UserEvent, String> {

        private transient ValueState<Boolean> processedState;

        @Override
        public void open(Configuration parameters) {
            // 初始化状态：记录"该用户是否已处理过首次点击"
            ValueStateDescriptor<Boolean> descriptor =
                    new ValueStateDescriptor<>("processed", Boolean.class);
            processedState = getRuntimeContext().getState(descriptor);
        }

        @Override
        public void flatMap(com.terranangel.flink.model.UserEvent event, Collector<String> out) throws Exception {
            // 首次处理：状态为空 → 处理并标记
            if (processedState.value() == null) {
                processedState.update(true);   // 标记已处理
                out.collect("[状态去重] 首次处理用户 " + event.getUserId() + " 的点击: " + event);
            } else {
                // 重复事件：状态已标记 → 跳过（幂等）
                out.collect("[状态去重] 跳过重复用户 " + event.getUserId() + " 的事件: " + event);
            }
        }
    }
}
