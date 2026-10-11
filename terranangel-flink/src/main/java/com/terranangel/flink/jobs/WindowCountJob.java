package com.terranangel.flink.jobs;

import com.terranangel.flink.source.FakeEventSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

/**
 * 【场景 1】窗口统计作业 —— 字节面试高频考点：窗口（Window）。
 * <p>
 * 业务场景：实时统计"每分钟 / 每 30 秒"的点击量（实时数仓 T+0 报表的典型）。
 * <p>
 * 【覆盖的 Flink 核心概念（面试）】
 *   1. 滚动窗口（Tumbling）：固定大小，不重叠，如每 1 分钟统计一次
 *   2. 滑动窗口（Sliding）：固定大小 + 滑动步长，重叠，如每 30 秒统计近 1 分钟
 *   3. 水印（Watermark）：处理乱序数据，允许迟到 5 秒
 *   4. 事件时间（Event Time）：按事件发生时间统计（不是处理时间）
 * <p>
 * 【运行】IDEA 跑 JobLauncher，参数传 "window"，或直接跑本类 main。
 */
public class WindowCountJob {

    public static void execute(StreamExecutionEnvironment env) throws Exception {
        // ===== 1. 数据源：真实用 Kafka Source，这里用假数据源（演示方便）=====
        DataStream<String> source = env.addSource(FakeEventSource.create());

        // ===== 2. 解析字符串 → UserEvent，并按事件时间 + 水印处理乱序 =====
        // Watermark：允许事件迟到 5 秒（晚到的数据如果在水印内，仍被计入窗口）
        DataStream<com.terranangel.flink.model.UserEvent> events = source
                .map(new MapFunction<String, com.terranangel.flink.model.UserEvent>() {
                    @Override
                    public com.terranangel.flink.model.UserEvent map(String line) {
                        // 简单解析：模拟 "click,1,100,0,1700000000000"
                        String[] parts = line.split(",");
                        return new com.terranangel.flink.model.UserEvent(
                                parts[0],
                                Long.valueOf(parts[1]),
                                Long.valueOf(parts[2]),
                                Double.valueOf(parts[3]),
                                Long.valueOf(parts[4]));
                    }
                })
                // 按事件时间处理 + 水印允许迟到 5 秒
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<com.terranangel.flink.model.UserEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withTimestampAssigner((event, ts) -> event.getEventTime()));

        // ===== 3. 只保留 click（点击）事件，转为 (用户id, 1) 用于计数 =====
        // 注意：lambda 返回 Tuple2 泛型会擦除，必须用 returns() 显式声明类型
        DataStream<Tuple2<Long, Integer>> clicks = events
                .filter(e -> "click".equals(e.getType()))
                .map(e -> new Tuple2<>(e.getUserId(), 1))
                .returns(org.apache.flink.api.common.typeinfo.Types.TUPLE(
                        org.apache.flink.api.common.typeinfo.Types.LONG,
                        org.apache.flink.api.common.typeinfo.Types.INT));

        // ===== 4a. 滚动窗口：每 60 秒统计一次"每个用户的点击量" =====
        // 注意：sum(1) 后返回 Tuple2<Long,Integer>，需用 returns() 显式声明类型（泛型擦除）
        clicks
                .keyBy(t -> t.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .sum(1)
                .returns(org.apache.flink.api.common.typeinfo.Types.TUPLE(
                        org.apache.flink.api.common.typeinfo.Types.LONG,
                        org.apache.flink.api.common.typeinfo.Types.INT))
                .map(t -> "【滚动窗口-每60秒】用户" + t.f0 + " 点击量: " + t.f1)
                .print();

        // ===== 4b. 滑动窗口：每 30 秒统计"近 60 秒"点击量（重叠）=====
        clicks
                .keyBy(t -> t.f0)
                .window(SlidingEventTimeWindows.of(Time.seconds(60), Time.seconds(30)))
                .sum(1)
                .returns(org.apache.flink.api.common.typeinfo.Types.TUPLE(
                        org.apache.flink.api.common.typeinfo.Types.LONG,
                        org.apache.flink.api.common.typeinfo.Types.INT))
                .map(t -> "【滑动窗口-每30秒看近60秒】用户" + t.f0 + " 点击量: " + t.f1)
                .print();

        System.out.println("[Flink] 窗口统计作业已启动，观察滚动窗口和滑动窗口的输出...");
    }
}
