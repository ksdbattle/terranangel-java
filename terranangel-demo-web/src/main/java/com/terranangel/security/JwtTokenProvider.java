package com.terranangel.security;

import com.terranangel.context.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT 令牌的签发与校验。
 * <p>
 * 【JWT 结构】header.payload.signature 三段，用点分隔。
 *   - header    ：算法信息（如 HS256）
 *   - payload   ：业务数据（用户名、角色、过期时间），**Base64 编码而非加密，前端可解开看**
 *   - signature ：用密钥签名，防止篡改
 * <p>
 * 【为什么不用服务端 Session】
 * Session 需要服务端存储（内存或 Redis），多实例部署时要共享。
 * JWT 把状态放在客户端，服务端无状态，天然适合水平扩容 + 微服务。
 * <p>
 * 【注意】payload 可被任何人解码，**绝不能放密码等敏感信息**。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;

    private final Duration expiration;

    public JwtTokenProvider(
            @Value("${terranangel.jwt.secret}") String secret,
            @Value("${terranangel.jwt.expiration-hours:2}") long expirationHours) {
        // HS256 要求密钥不短于 256 bit（32 字节），太短 jjwt 会直接抛异常提醒
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(expirationHours);
    }

    /**
     * 签发令牌。
     * subject 放用户名，额外带 userId 和 roles，这样校验时无需再查库。
     */
    public String createToken(CurrentUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .claim("uid", user.userId())
                .claim("roles", user.roles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌，还原出当前用户。
     * 签名不对、被篡改或已过期都会抛 {@link JwtException}。
     */
    @SuppressWarnings("unchecked")
    public CurrentUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = null;
        Object rawUid = claims.get("uid");
        if (rawUid instanceof Number number) {
            userId = number.longValue();
        }

        List<String> roles = claims.get("roles", List.class);
        return new CurrentUser(userId, claims.getSubject(), roles);
    }

    /** 令牌是否有效（签名正确且未过期） */
    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 令牌有效期（秒），返回给前端用于提示"多久后需要重新登录" */
    public long expirationSeconds() {
        return expiration.toSeconds();
    }
}
