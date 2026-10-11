package com.terranangel;

/**
 * 一个普通的业务服务，用来演示单元测试怎么写。
 * <p>
 * 故意写得简单：不依赖 Spring 容器、不依赖 redis，
 * 这样单元测试可以"纯净"地测它的逻辑，不需要启动整个应用。
 */
public class HelloService {

    /**
     * 根据传入的名字生成问候语。
     */
    public String greet(String name) {
        if (name == null || name.isBlank()) {
            return "Hello, stranger!";
        }
        return "Hello, " + name + "!";
    }

    /**
     * 一个带 if/else 逻辑的方法，方便测试各种分支。
     */
    public String rate(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be 0-100, but was " + score);
        }
        if (score >= 90) {
            return "excellent";
        }
        if (score >= 60) {
            return "pass";
        }
        return "fail";
    }
}
