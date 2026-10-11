package com.terranangel.db.repository;

import com.terranangel.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓库（DDD Repository 层，接口即 DAO）。
 * <p>
 * Spring Data JPA：只要继承 JpaRepository，就自动获得全套 CRUD 方法
 * （save/findById/findAll/deleteById 等），不用写实现。
 * <p>
 * 同时可以按"方法名规范"或 @Query 定义自定义查询。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按用户名精确查询（方法名规范自动生成 SQL：findByUsername） */
    Optional<User> findByUsername(String username);

    /** 按邮箱模糊查询 */
    List<User> findByEmailContaining(String email);

    /** 按年龄范围查询 */
    List<User> findByAgeBetween(Integer minAge, Integer maxAge);

    /** 自定义 JPQL 查询：查出年龄大于某值的用户（演示 @Query 写法） */
    @Query("SELECT u FROM User u WHERE u.age > :minAge")
    List<User> findOlderThan(@Param("minAge") Integer minAge);
}
