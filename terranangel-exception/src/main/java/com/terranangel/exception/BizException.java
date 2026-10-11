package com.terranangel.exception;

import java.io.Serializable;

/**
 * 业务异常。
 * <p>
 * 业务代码用它来表达"业务规则不满足"，例如数据不存在、重复提交、
 * 状态不允许流转等。它是所有业务语义异常的基类，本身与框架无关，
 * 任何项目通用。
 * <p>
 * 异常类名即是最好的"错误码"——看到 {@link BizException} 就能立刻
 * 定位到业务问题，无需额外的错误码体系。正常业务处理失败统一走 500。
 */
public class BizException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    public BizException(Throwable cause) {
        super(cause);
    }
}
