package com.terranangel.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign 客户端接口（放在共享 api 模块）。
 * <p>
 * 通过服务名 terranangel-demo-web 走注册中心（Nacos）发现服务，
 * 而不是写死 URL。返回复杂对象 UserDTO。
 */
@FeignClient(name = "terranangel-demo-web")
public interface HelloFeignClient {

    @GetMapping("/hello")
    String hello();

    /**
     * 通过微服务获取一个复杂对象 UserDTO。
     */
    @GetMapping("/user/{id}")
    UserDTO getUser(@PathVariable("id") Long id);
}
