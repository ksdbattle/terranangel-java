package com.terranangel.security;

import com.terranangel.context.CurrentUser;
import com.terranangel.context.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 * <p>
 * 【职责】每个请求进来自动做三件事：
 *   1. 从 Authorization: Bearer xxx 取出令牌
 *   2. 校验通过后解析出用户，写入 {@link UserContext}（ScopedValue）和 Spring Security 上下文
 *   3. 请求结束后 Spring Security 上下文由本过滤器清理；UserContext 随作用域自动失效
 * <p>
 * 【为什么写两处上下文】
 *   UserContext           —— 给你的业务代码用，UserContext.get() 随手可取（ScopedValue，作用域内有效）
 *   SecurityContextHolder —— 给 Spring Security 用，authorizeHttpRequests / @PreAuthorize 靠它判断
 *      （框架底层仍是 ThreadLocal，由框架的 SecurityContextHolderFilter 兜底清理）
 * <p>
 * 【为什么不用 @Component】
 * 这个过滤器只应挂在 Spring Security 的过滤器链上。
 * 如果标了 @Component，Spring Boot 会把它**再注册到 Servlet 容器**，
 * 导致每个请求执行两次。所以这里不标，由 SecurityConfig 手动装配。
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";

    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        CurrentUser user = (token != null && tokenProvider.validate(token))
                ? tokenProvider.parseToken(token)
                : null;

        if (user == null) {
            // 没带（或带错）token：不进入作用域，UserContext.get() 自然返回 null
            filterChain.doFilter(request, response);
            return;
        }

        // Spring Security 上下文：让权限判断生效
        List<SimpleGrantedAuthority> authorities = user.roles() == null
                ? List.of()
                : user.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
        var authentication = new UsernamePasswordAuthenticationToken(user.username(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            // 自定义上下文：作用域结束自动失效，不存在"忘记清理导致串号"的可能
            UserContext.call(user, () -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException e) {
            throw e;
        } catch (Exception e) {
            // ScopedValue.call 会把 lambda 抛出的多个受检异常推断成 Exception，这里还原语义
            throw new ServletException(e);
        } finally {
            // SecurityContextHolder 底层是 ThreadLocal，仍需显式清理（框架也会兜底）
            SecurityContextHolder.clearContext();
        }
    }

    /** 从 Authorization: Bearer xxx 中取出令牌，没有则返回 null */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
