package com.terranangel.flink.jobs;

import com.terranangel.flink.source.FakeEventSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

/**
 * 【场景 2】实时告警作业 —— 字节面试高频：实时风控/异常检测。
 * <p>
 * 业务场景：实时监控"每个用户在 60 秒内的支付金额"，超过 50000 触发告警（风控拦截）。
 * <p>
 * 【覆盖的 Flink 核心概念（面试）】
 *   1. 窗口聚合 + 阈值判断 → 实时告警
 *   2. keyBy（按用户分组）+ sum（求和）
 *   3. 实时风控：秒级发现异常（刷单、大额交易）
 * <p>
 * 【运行】IDEA 跑 JobLauncher，参数传 "alert"。
 */
public class RealTimeAlertJob {

    public static final double ALERT_THRESHOLD = 50000;

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

        // 只保留支付事件，转为 (用户id, 金额)
        // 注意：lambda 返回 Tuple2 泛型会擦除，必须用 returns() 显式声明类型
        DataStream<Tuple2<Long, Double>> pays = events
                .filter(e -> "pay".equals(e.getType()))
                .map(e -> new Tuple2<>(e.getUserId(), e.getAmount()))
                .returns(org.apache.flink.api.common.typeinfo.Types.TUPLE(
                        org.apache.flink.api.common.typeinfo.Types.LONG,
                        org.apache.flink.api.common.typeinfo.Types.DOUBLE));

        // 每 60 秒统计每个用户的支付总额，超过阈值触发告警（风控）
        pays
                .keyBy(t -> t.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .sum(1)
                .returns(org.apache.flink.api.common.typeinfo.Types.TUPLE(
                        org.apache.flink.api.common.typeinfo.Types.LONG,
                        org.apache.flink.api.common.typeinfo.Types.DOUBLE))
                .filter(t -> t.f1 >= ALERT_THRESHOLD)   // 超过阈值 → 告警
                .map(t -> "【风控告警】用户" + t.f0 + " 60秒内支付 " + t.f1 + " 元，超过阈值 "
                        + ALERT_THRESHOLD + "，触发风控拦截!")
                .print();

        System.out.println("[Flink] 实时告警作业已启动，观察超过阈值" + ALERT_THRESHOLD + "的告警...");
    }
}
