package com.terranangel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 开关配置。
 * <p>
 * 核心：只有配置了 terranangel.feign.enabled=true 才启用 OpenFeign。
 * 没配或 enabled=false → 整个类不装配，OpenFeign 不初始化，Spring 照常启动（不报错）。
 * <p>
 * 这正是"按需加载"哲学：没配 OpenFeign，应用也能正常启动，启动速度不变。
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.feign", name = "enabled", havingValue = "true")
@EnableFeignClients(basePackages = "com.terranangel.api")
public class FeignSupportConfig {
}
