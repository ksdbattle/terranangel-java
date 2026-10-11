package com.terranangel.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.junit.jupiter.api.Test;

/**
 * Flink 作业本地测试 —— 不需要 Flink 集群，直接本地跑。
 * <p>
 * 【怎么用】
 *   直接运行这个测试类，就能本地跑 Flink 作业。
 *   这是"本地开发调试"的标准方式。
 * <p>
 * 【注意】
 *   - 用一个"有限数据源"（只发几条数据），作业处理完会自动结束
 *   - 不需要 Kafka / Flink 集群
 */
class FlinkJobTest {

    /**
     * 本地跑窗口统计作业。
     * 用有限数据源（发 5 条点击事件），作业处理完自动结束。
     */
    @Test
    void runWindowCountJobLocally() throws Exception {
        System.out.println("========== 开始本地运行 Flink 窗口统计作业 ==========");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 假数据源：只发 5 条点击事件，然后结束（作业自动停止）
        env.addSource(new FiniteClickSource());

        // 复用窗口统计作业的逻辑，但数据源用上面有限的
        com.terranangel.flink.jobs.WindowCountJob.execute(env);

        env.execute("WindowCountJob-Local-Test");
        System.out.println("========== Flink 窗口作业运行完成 ==========");
    }

    /**
     * 有限数据源：发 5 条点击事件就结束。
     * 格式：type,userId,productId,amount,eventTime
     */
    static class FiniteClickSource implements SourceFunction<String> {
        private static final long serialVersionUID = 1L;
        private volatile boolean running = true;
        private int count = 0;

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            long baseTime = System.currentTimeMillis();
            while (running && count < 5) {
                // 5 条点击事件，属于同一个用户（方便看到窗口统计）
                long userId = 1;
                long productId = 100 + count;
                ctx.collect("click," + userId + "," + productId + ",0," + (baseTime + count * 1000));
                count++;
                Thread.sleep(500);
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}
