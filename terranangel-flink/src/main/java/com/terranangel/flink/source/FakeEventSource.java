package com.terranangel.flink.source;

import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

import java.util.Random;

/**
 * 假数据源（演示用）—— 每秒随机生成一条用户行为事件。
 * <p>
 * 【真实场景】替换成 Kafka Source：
 *   FlinkKafkaConsumer<String> kafkaSource = new FlinkKafkaConsumer<>(
 *       "topic", new SimpleStringSchema(), kafkaProps);
 *   env.addSource(kafkaSource);
 * <p>
 * 数据格式：type,userId,productId,amount,eventTime
 *   例如：click,1,100,0,1700000000000
 */
public class FakeEventSource extends RichParallelSourceFunction<String> {

    private volatile boolean running = true;
    private final Random random = new Random();
    private static final String[] TYPES = {"click", "click", "click", "order", "pay"};

    public static FakeEventSource create() {
        return new FakeEventSource();
    }

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        while (running) {
            String type = TYPES[random.nextInt(TYPES.length)];
            long userId = random.nextInt(1000) + 1;
            long productId = random.nextInt(5000) + 1;
            double amount = "pay".equals(type) ? random.nextInt(10000) : 0;
            long eventTime = System.currentTimeMillis();
            // 输出格式：type,userId,productId,amount,eventTime
            ctx.collect(type + "," + userId + "," + productId + "," + amount + "," + eventTime);
            Thread.sleep(1000); // 每秒生成一条
        }
    }

    @Override
    public void cancel() {
        running = false;
    }
}
