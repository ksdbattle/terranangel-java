package com.terranangel.security;

import tools.jackson.databind.json.JsonMapper;
import com.terranangel.context.TraceContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 配置。
 * <p>
 * 【整体思路：前后端分离 + JWT 无状态认证】
 *   - 不创建 Session，每个请求靠 Authorization 头里的 token 认证
 *   - 关掉默认的表单登录 / Basic 认证，因为我们要的是 JSON API
 *   - 未登录返回 401、权限不足返回 403，都是 JSON 而不是 HTML 错误页
 * <p>
 * 【认证流程】
 *   请求到达 → TraceIdFilter（打 traceId）
 *            → JwtAuthFilter（解析 token，写 UserContext）
 *            → authorizeHttpRequests 判断路径是否有权限
 *            → Controller
 * <p>
 * 【为什么不用 @EnableMethodSecurity】
 * 本演示只做到"登录 / 未登录"这一层。要按角色细化到方法级
 * （@PreAuthorize("hasRole('ADMIN')")），再加这个注解即可。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 无需登录即可访问的路径。
     * <p>
     * 注意 {@code {"/auth/me"}} 不在其中——它就是要演示"必须带 token 才能访问"。
     */
    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/hello",
            // PostgreSQL 演示接口（唯一开关是 terranangel.datasource.postgres.enabled）
            // 开出来方便直接验证线上库是否接通；不需要了把这行删掉即可恢复需登录
            "/db/user/**",
            "/actuator/**",
            "/error",
            "/favicon.ico",
            "/**/*.html",
            "/**/*.css",
            "/**/*.js",
            "/**/*.ico"
    };

    private final JwtTokenProvider tokenProvider;

    private final JsonMapper jsonMapper;

    public SecurityConfig(JwtTokenProvider tokenProvider, JsonMapper jsonMapper) {
        this.tokenProvider = tokenProvider;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + 无状态 token，不存在浏览器自动携带凭证的问题，故关闭 CSRF
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 不创建也不使用 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 关掉默认的表单登录页和 Basic 弹窗
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 路径级权限：白名单放行，其余一律要求认证
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // JWT 过滤器要排在"用户名密码认证过滤器"之前，先把身份认出来
                .addFilterBefore(new JwtAuthFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                // 认证/授权失败也返回 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    /** 未登录（没带 token，或 token 无效/过期）→ 401 */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
    }

    /** 已登录但角色不够 → 403 */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "当前账号无权访问该资源");
    }

    /**
     * 跨域配置。
     * 这里放得比较宽（演示用）；生产环境应把 allowedOriginPatterns 收紧到具体域名。
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** 统一输出 JSON 错误体，并带上 traceId 方便排查 */
    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(Map.of(
                "code", status,
                "message", message,
                "traceId", TraceContext.getOrEmpty())));
    }
}
