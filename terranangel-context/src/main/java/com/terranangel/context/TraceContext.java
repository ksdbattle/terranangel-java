package com.terranangel.context;

/**
 * 链路追踪上下文（Scoped Value）。
 * <p>
 * 【作用】给每个请求打一个唯一 ID，贯穿日志、下游调用、异常响应。
 * 线上排查问题时，拿这个 ID 就能把一次请求的全部日志串起来。
 * <p>
 * 【为什么用 ScopedValue 而不是 ThreadLocal】
 *   1. 生命周期由"作用域"决定：{@link #call} 执行完（正常返回或抛异常）自动失效，
 *      根本不存在"忘记 clear 导致下一个请求读到上一个请求的数据"（串号）的可能；
 *   2. 不可变：绑定后不能被改写，天然线程安全；
 *   3. 内存友好：海量虚拟线程场景下，不像 ThreadLocal 那样每个线程都留一份副本。
 * <p>
 * 【怎么用】Web 层在过滤器里把整条请求链包进作用域：
 * <pre>
 *   TraceContext.call(traceId, () -&gt; { chain.doFilter(request, response); return null; });
 * </pre>
 * 作用域内的代码（Controller / Service / 异常处理器）直接读：
 * <pre>
 *   String traceId = TraceContext.getOrEmpty();
 * </pre>
 * <p>
 * 【和 MDC 的关系】本类负责"持有"traceId，日志框架的 MDC 负责"打印"。
 * 过滤器里两个都写：本类供业务代码读取，MDC 供日志 pattern 引用。
 * 注意 MDC 底层仍是 ThreadLocal（Logback 实现），由过滤器负责清理。
 * <p>
 * 【和生产级方案的差距】这里只是最小实现。真实项目用
 * Micrometer Tracing + OpenTelemetry，能跨服务自动传递和采样。
 */
public final class TraceContext {

    /** HTTP 请求/响应头名称，网关或上游传进来时不生成新的，实现跨服务串联 */
    public static final String HEADER = "X-Trace-Id";

    /** 日志 MDC 中的 key，配合 logback pattern 里的 %X{traceId} 使用 */
    public static final String MDC_KEY = "traceId";

    /** 作用域值：只有绑定了才可读，作用域结束自动失效 */
    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    private TraceContext() {
    }

    /**
     * 在指定 traceId 的作用域内执行操作。
     * <p>
     * 作用域结束后 traceId 自动失效，无需（也无法）手动清理。
     *
     * @param traceId 本次请求的链路 ID
     * @param op      作用域内要执行的操作
     * @return 操作返回值
     */
    public static <T, X extends Throwable> T call(String traceId, ScopedValue.CallableOp<T, X> op) throws X {
        // 【临时诊断】用来确认正在运行的进程到底加载的是本模块的最新字节码，还是 ~/.m2 里的旧 jar。
        // 每个请求都会走到这里（TraceIdFilter 包裹整条链），所以任何接口都能触发。
        // 确认完成后删除这一行即可。
        System.out.println("[TraceContext] call() 执行了, traceId=" + traceId + "  <== context-print-v11");
        return ScopedValue.where(TRACE_ID, traceId).call(op);
    }

    /** 在指定 traceId 的作用域内执行一段无返回值、不抛受检异常的代码 */
    public static void run(String traceId, Runnable op) {
        ScopedValue.where(TRACE_ID, traceId).run(op);
    }

    /** 读取当前 traceId，不在作用域内返回 null */
    public static String get() {
        // 注意：不能用 orElse(null)——ScopedValue.orElse 的入参不允许为 null，
        // 传 null 会直接抛 NullPointerException，必须先判断是否已绑定。
        return TRACE_ID.isBound() ? TRACE_ID.get() : null;
    }

    /** 读取当前 traceId，不在作用域内返回空串（便于拼 JSON / 日志） */
    public static String getOrEmpty() {
        return TRACE_ID.isBound() ? TRACE_ID.get() : "";
    }
}
