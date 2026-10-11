package com.terranangel;

import com.terranangel.api.HelloFeignClient;
import com.terranangel.api.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * web -> OpenFeign 调用示例。
 * <p>
 * 通过服务名走注册中心（Nacos）调用，获取复杂对象 UserDTO。
 * 接口和 DTO 都在共享 api 模块，两端字段一致。
 */
@RestController
@ConditionalOnProperty(prefix = "terranangel.feign", name = "enabled", havingValue = "true")
public class FeignController {

    @Autowired
    private HelloFeignClient helloFeignClient;

    @GetMapping("/callFeign")
    public String callFeign() {
        return "web 调用 OpenFeign 成功: " + helloFeignClient.hello();
    }

    /**
     * 通过微服务获取复杂对象：浏览器访问这个接口，内部用 OpenFeign 调 /user/{id} 拿到 UserDTO。
     */
    @GetMapping("/callFeignUser")
    public String callFeignUser(@RequestParam(defaultValue = "1") Long id) {
        UserDTO user = helloFeignClient.getUser(id);
        return "通过微服务获取复杂对象成功: " + user;
    }
}
