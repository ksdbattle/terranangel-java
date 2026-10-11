package com.terranangel.flink;

import com.terranangel.flink.jobs.WindowCountJob;
import com.terranangel.flink.jobs.KafkaStreamJob;
import com.terranangel.flink.jobs.RealTimeAlertJob;
import com.terranangel.flink.jobs.StateDedupJob;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Flink 作业入口（Launcher）—— 使用者直接跑这个类就能启动任意一个作业。
 * <p>
 * 【怎么用】（不用查文档）
 *   1. 在 IDEA 直接运行本类的 main 方法
 *   2. 通过 main 参数指定要跑的作业编号，例如：
 *      参数: "window"   → 运行【窗口统计作业】（实时统计每分钟点击量）
 *      参数: "kafka"    → 运行【Kafka 实时接入作业】（读 Kafka 打印数据）
 *      参数: "alert"    → 运行【实时告警作业】（风控，超阈值告警）
 *      参数: "dedup"    → 运行【状态去重作业】（幂等，防重复）
 *      不传参数 → 默认跑窗口统计作业
 * <p>
 * 【提交到 Flink 集群】（生产）
 *   mvn clean package
 *   flink run -c com.terranangel.flink.JobLauncher target/terranangel-flink-0.1.0-SNAPSHOT.jar window
 */
public class JobLauncher {

    public static void main(String[] args) throws Exception {
        // 1. 创建 Flink 执行环境（本地跑 / 集群跑自动识别）
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2. 根据参数选择要跑的作业
        String job = args.length > 0 ? args[0] : "window";

        switch (job) {
            case "kafka":
                KafkaStreamJob.execute(env);       // Kafka 实时接入
                break;
            case "window":
                WindowCountJob.execute(env);       // 窗口统计（滚动/滑动/会话）
                break;
            case "alert":
                RealTimeAlertJob.execute(env);     // 实时告警（风控）
                break;
            case "dedup":
                StateDedupJob.execute(env);        // 状态去重（幂等）
                break;
            default:
                System.out.println("[Flink] 未知作业: " + job + "，可选: kafka / window / alert / dedup");
                WindowCountJob.execute(env);
        }

        // 3. 执行作业（阻塞，持续运行）
        env.execute("TerranAngel Flink Job: " + job);
    }
}
