package com.terranangel.api;

import java.io.Serializable;

/**
 * 用户数据传输对象（复杂对象示例）。
 * <p>
 * 放在共享 api 模块，服务端和客户端两端都依赖它，保证字段一致。
 * 用于验证"通过微服务传复杂对象"。
 */
public class UserDTO implements Serializable {

    private Long id;
    private String name;
    private Integer age;
    private String email;

    public UserDTO() {
    }

    public UserDTO(Long id, String name, Integer age, String email) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserDTO{id=" + id + ", name='" + name + "', age=" + age + ", email='" + email + "'}";
    }
}
