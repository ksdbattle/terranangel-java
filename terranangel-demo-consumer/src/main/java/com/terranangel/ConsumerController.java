package com.terranangel;

import com.terranangel.api.HelloFeignClient;
import com.terranangel.api.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumer 调用接口：使用 OpenFeign 通过 Consul 发现并调用 demo-web。
 * <p>
 * 复用 api 模块的 HelloFeignClient（服务名 terranangel-demo-web），
 * 通过 Consul 注册中心发现服务，OpenFeign + LoadBalancer 调用。
 */
@RestController
public class ConsumerController {

    @Autowired
    private HelloFeignClient helloFeignClient;

    @GetMapping("/consumer/callHello")
    public String callHello() {
        return "Consumer 调用 demo-web 成功: " + helloFeignClient.hello();
    }

    @GetMapping("/consumer/callUser")
    public String callUser(@RequestParam(defaultValue = "1") Long id) {
        UserDTO user = helloFeignClient.getUser(id);
        return "Consumer 通过微服务获取复杂对象成功: " + user;
    }
}
