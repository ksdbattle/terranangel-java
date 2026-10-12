package com.terranangel.security.dto;

import java.util.List;

/**
 * 登录响应体。
 *
 * @param token            签发的 JWT，前端存起来，后续请求带在 Authorization 头里
 * @param tokenType        固定为 Bearer，方便前端拼 "Bearer " + token
 * @param expiresInSeconds 有效期（秒），前端据此判断何时提示重新登录
 * @param username         用户名
 * @param roles            角色列表
 */
public record LoginResponse(String token,
                            String tokenType,
                            long expiresInSeconds,
                            String username,
                            List<String> roles) {
}
