package com.terranangel.security;

import com.terranangel.context.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器。
 * <p>
 * 【流程】
 *   1. 从请求头 X-Trace-Id 取 traceId；没有就生成一个
 *   2. 写入 ScopedValue（业务代码可读）和 MDC（日志会打印）
 *   3. 回写响应头，方便前端 / 调用方拿到这个 ID 报障
 * <p>
 * 【为什么从请求头取】上游服务（网关、Feign 调用方）传进来时沿用同一个 ID，
 * 就能把一次跨服务调用的日志串起来；没有才新建，保证链路起点也有 ID。
 * <p>
 * 【@Order(HIGHEST_PRECEDENCE)】让它排在 Spring Security 过滤器链之前，
 * 这样连 401 拒绝请求的日志也能带上 traceId。
 * <p>
 * 【清理】TraceContext 用的是 ScopedValue，作用域结束自动失效，不需要清理；
 * 但 MDC 底层仍是 ThreadLocal（Logback 实现），必须在 finally 里 clear。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String MDC_URI_KEY = "uri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TraceContext.HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(TraceContext.MDC_KEY, traceId);
        MDC.put(MDC_URI_KEY, request.getMethod() + " " + request.getRequestURI());
        response.setHeader(TraceContext.HEADER, traceId);

        try {
            // 整条请求链都在这个作用域内；作用域一结束 traceId 自动失效，无需清理
            TraceContext.call(traceId, () -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException e) {
            throw e;
        } catch (Exception e) {
            // ScopedValue.call 会把 lambda 抛出的多个受检异常推断成 Exception，这里还原语义
            throw new ServletException(e);
        } finally {
            MDC.clear();
        }
    }
}
