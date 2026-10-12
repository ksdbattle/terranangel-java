package com.terranangel.context;

import java.util.List;

/**
 * 当前登录用户（纯数据模型）。
 * <p>
 * 【设计原则】只依赖 JDK，不依赖 Spring / Security / JWT。
 * 这样任何模块（Web、Flink、RPC、定时任务）都能用它携带"当前操作人"，
 * 谁负责产生它（Security 过滤器的 JWT 解析）谁去构造。
 *
 * @param userId   用户 ID
 * @param username 用户名
 * @param roles    角色列表（如 ADMIN / USER）
 */
public record CurrentUser(Long userId, String username, List<String> roles) {

    /** 是否拥有某个角色 */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /** 是否管理员 */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
