package com.terranangel.web;

import com.terranangel.context.TraceContext;
import com.terranangel.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 【为什么需要它】没有它的话，业务抛出的异常会变成 Spring 默认的 500 错误页，
 * 前端拿到的是 HTML，没法解析。
 * <p>
 * 【统一错误体】所有异常都转成同一个结构，并带上 traceId——
 * 用户报障时报这个 ID，就能在日志里定位到那一次请求：
 * <pre>
 *   { "code": 500, "message": "商品不存在", "traceId": "a1b2c3d4e5f6a7b8" }
 * </pre>
 * <p>
 * 【匹配规则】Spring 会优先选择最具体的异常类型对应的方法，
 * 所以 ResponseStatusException 会被下面专门的处理器接住，不会落到兜底里。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常。
     * BizException 是业务语义异常的基类（数据不存在、状态不允许等），
     * 属于"可预期的失败"，用 warn 级别记录，不打堆栈。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 显式指定了 HTTP 状态的异常（如登录失败的 401）。
     * 必须单独处理：否则会被下面的兜底捕获成 500，把 401 吃掉。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        log.warn("请求被拒绝: {} {}", status.value(), e.getReason());
        return build(status, e.getReason());
    }

    /**
     * 参数校验失败（@Valid 不通过）。
     * 把所有字段的错误消息拼起来返回，前端可以直接展示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 静态资源不存在（典型场景：浏览器自动请求 /favicon.ico）。
     * <p>
     * 【为什么必须单独处理】
     * NoResourceFoundException 继承的是 ErrorResponseException，不是
     * ResponseStatusException，所以不会命中上面那个处理器，而是掉进下面的兜底，
     * 变成 500「系统繁忙」。这有两处不对：
     *   1. 语义错了——客户端要了一个不存在的东西，正确答案是 404；
     *   2. 噪音——每个浏览器标签、每个页面都会请求 favicon，
     *      用 error + 完整堆栈记录会把日志刷到没法看。
     * 所以这里返回 404，并且只在 DEBUG 级别记一行（生产日志不再出现）。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("静态资源不存在: {}", e.getResourcePath());
        return build(HttpStatus.NOT_FOUND, "静态资源不存在: " + e.getResourcePath());
    }

    /**
     * 兜底：未预期的异常。
     * 这里必须打完整堆栈（error 级别），否则线上无法定位；
     * 但返回给前端的消息要模糊化，避免把内部结构暴露出去。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("系统异常", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        body.put("traceId", TraceContext.getOrEmpty());
        return ResponseEntity.status(status).body(body);
    }
}
