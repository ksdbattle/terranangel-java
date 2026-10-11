package com.terranangel;

import com.terranangel.api.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：提供返回复杂对象 UserDTO 的接口，供 OpenFeign 通过微服务调用。
 */
@RestController
public class UserController {

    /**
     * 返回一个复杂对象（UserDTO）。
     * OpenFeign 客户端通过服务名走注册中心找到本服务，调用这个接口拿到复杂对象。
     */
    @GetMapping("/user/{id}")
    public UserDTO getUser(@PathVariable("id") Long id) {
        return new UserDTO(id, "TerranAngel" + id, 25, "user" + id + "@terranangel.com");
    }
}
