package com.terranangel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Consumer 应用：独立的服务消费方。
 * <p>
 * 使用 OpenFeign 通过 Consul 发现并调用 demo-web。
 * 端口 8081（与 demo-web 的 8080 区分）。
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.terranangel.api")
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
