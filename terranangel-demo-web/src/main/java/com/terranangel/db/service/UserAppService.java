package com.terranangel.db.service;

import com.terranangel.db.entity.Order;
import com.terranangel.db.entity.User;
import com.terranangel.db.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户应用服务（DDD Application Service）。
 * <p>
 * 作用：编排业务用例（use case），事务边界在这里。
 * 只做"业务编排"，不写具体 SQL（那是 Repository 的事）。
 * <p>
 * 【面试点】DDD 分层：
 *   Controller → ApplicationService(应用服务，编排事务) → Domain(领域模型/Repository) → DB
 * <p>
 * 【开关】与数据源装配保持一致：terranangel.datasource.postgres.enabled=true 才创建本 Bean，
 * 否则（默认 false）不装配，避免注入不存在的 UserRepository。
 */
@Service
@ConditionalOnProperty(prefix = "terranangel.datasource.postgres", name = "enabled", havingValue = "true")
public class UserAppService {

    private final UserRepository userRepository;

    public UserAppService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 注册用户（写操作，加事务）。
     * 演示：保存一个聚合根 User。
     */
    @Transactional
    public User register(String username, String email, Integer age) {
        User user = new User(username, email, age);
        return userRepository.save(user);
    }

    /**
     * 给用户下一单（演示聚合内一致性：通过 User.addOrder 添加订单，而不是直接操作 Order）。
     * 体现 DDD：领域行为封装在聚合根内，外部通过业务方法操作。
     */
    @Transactional
    public User placeOrder(Long userId, String orderNo, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        // 领域行为：订单加到用户聚合里，维护内外部一致性
        user.addOrder(new Order(orderNo, amount));
        // 级联保存（cascade = ALL，保存 User 时自动保存关联的 Order）
        return userRepository.save(user);
    }

    /**
     * 按用户名查用户（读操作，可加只读事务或直接查）。
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
    }

    /**
     * 查询所有用户。
     */
    @Transactional(readOnly = true)
    public List<User> listAll() {
        return userRepository.findAll();
    }

    /**
     * 查询年龄大于某值的用户（演示 Repository 的 @Query 自定义查询）。
     */
    @Transactional(readOnly = true)
    public List<User> listOlderThan(Integer minAge) {
        return userRepository.findOlderThan(minAge);
    }

    /**
     * 删除用户（级联删除其订单）。
     */
    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }
}
