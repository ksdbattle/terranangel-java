package com.terranangel.db;

import com.terranangel.db.entity.User;
import com.terranangel.db.service.UserAppService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * PostgreSQL 数据访问演示接口（Spring Data JPA）。
 * <p>
 * 【前提】需要 terranangel.datasource.postgres.enabled=true 且本地 PG 服务启动。
 * <p>
 * 【测试入口】启动 PG 后访问：
 *   POST /db/user/register?username=xxx&email=xxx&age=18     注册用户
 *   POST /db/user/{id}/order?orderNo=NO001&amount=100        给用户下单
 *   GET  /db/user/name/{username}                             按用户名查用户
 *   GET  /db/user/all                                         查全部用户
 *   GET  /db/user/older?age=20                                查年龄>N 的用户
 *   DELETE /db/user/{id}                                      删除用户
 */
@RestController
@RequestMapping("/db/user")
@ConditionalOnProperty(prefix = "terranangel.datasource.postgres", name = "enabled", havingValue = "true")
public class UserDbController {

    private final UserAppService userAppService;

    public UserDbController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    /** 注册用户 */
    @PostMapping("/register")
    public User register(@RequestParam String username,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false, defaultValue = "18") Integer age) {
        return userAppService.register(username, email, age);
    }

    /** 给用户下单（演示聚合一致性） */
    @PostMapping("/{id}/order")
    public User placeOrder(@PathVariable Long id,
                           @RequestParam String orderNo,
                           @RequestParam BigDecimal amount) {
        return userAppService.placeOrder(id, orderNo, amount);
    }

    /** 按用户名查用户 */
    @GetMapping("/name/{username}")
    public User findByUsername(@PathVariable String username) {
        return userAppService.findByUsername(username);
    }

    /** 查所有用户 */
    @GetMapping("/all")
    public List<User> listAll() {
        return userAppService.listAll();
    }

    /** 查年龄大于 N 的用户 */
    @GetMapping("/older")
    public List<User> listOlderThan(@RequestParam Integer age) {
        return userAppService.listOlderThan(age);
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        userAppService.delete(id);
        return "删除成功: " + id;
    }
}
