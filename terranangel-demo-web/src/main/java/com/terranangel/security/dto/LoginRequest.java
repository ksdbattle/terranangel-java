package com.terranangel.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体。
 * <p>
 * {@code @Valid} 触发校验，不通过时抛 MethodArgumentNotValidException，
 * 由 GlobalExceptionHandler 统一转成 400 JSON。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {
}
