package com.terranangel.security;

import com.terranangel.context.CurrentUser;
import com.terranangel.context.UserContext;
import com.terranangel.security.dto.LoginRequest;
import com.terranangel.security.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 认证接口：登录、获取当前用户。
 * <p>
 * 【演示用的内存用户表】真实项目应该：
 *   1. 用户存数据库，密码用 BCrypt 加密（不可逆哈希，不是明文也不是可解密加密）
 *   2. 用 {@code UserDetailsService} 加载用户，交给 Spring Security 校验
 *   3. 支持"记住我"、多端登录、token 主动失效（黑名单 / Redis）
 * <p>
 * 【这个类演示的重点】
 *   /auth/me 里没有从任何参数取用户，而是用 {@link UserContext#require()} 拿到——
 *   证明"当前登录用户"这个信息已经由过滤器贯穿到了业务代码里。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 演示用的内存用户表：用户名 → 密码 */
    private static final Map<String, String> USERS = Map.of(
            "admin", "123456",
            "user", "123456");

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * 登录，成功返回 JWT。
     * <p>
     * 请求体：{"username": "admin", "password": "123456"}
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        String expected = USERS.get(request.username());
        if (expected == null || !expected.equals(request.password())) {
            // 这里刻意不区分"用户不存在"和"密码错误"，避免给攻击者提供用户名枚举的线索
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        CurrentUser user = new CurrentUser(
                "admin".equals(request.username()) ? 1L : 2L,
                request.username(),
                "admin".equals(request.username()) ? List.of("ADMIN") : List.of("USER"));

        String token = tokenProvider.createToken(user);
        return new LoginResponse(token, "Bearer", tokenProvider.expirationSeconds(),
                user.username(), user.roles());
    }

    /**
     * 获取当前登录用户。
     * <p>
     * 该路径不在白名单里，必须带 {@code Authorization: Bearer <token>} 才能访问；
     * 不带会被 JwtAuthFilter 放过后由 Security 拒绝，返回 401 JSON。
     */
    @GetMapping("/me")
    public CurrentUser me() {
        return UserContext.require();
    }
}
