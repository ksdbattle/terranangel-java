package com.terranangel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 纯单元测试：不启动 Spring 容器，直接 new 业务对象测逻辑。
 * <p>
 * 这是单元测试的"最纯净"形态——不依赖任何框架，测的就是业务方法的逻辑。
 * 速度极快（毫秒级），因为没有任何容器初始化。
 */
class HelloServiceTest {

    private final HelloService service = new HelloService();

    @Test
    void greet_withName_returnsHelloName() {
        assertEquals("Hello, Bob!", service.greet("Bob"));
    }

    @Test
    void greet_withBlankName_returnsStranger() {
        assertEquals("Hello, stranger!", service.greet(""));
        assertEquals("Hello, stranger!", service.greet(null));
        assertEquals("Hello, stranger!", service.greet("   "));
    }

    @Test
    void rate_withHighScore_returnsExcellent() {
        assertEquals("excellent", service.rate(90));
        assertEquals("excellent", service.rate(100));
    }

    @Test
    void rate_withMediumScore_returnsPass() {
        assertEquals("pass", service.rate(60));
        assertEquals("pass", service.rate(89));
    }

    @Test
    void rate_withLowScore_returnsFail() {
        assertEquals("fail", service.rate(0));
        assertEquals("fail", service.rate(59));
    }

    @Test
    void rate_withOutOfRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.rate(-1));
        assertThrows(IllegalArgumentException.class, () -> service.rate(101));
    }
}
