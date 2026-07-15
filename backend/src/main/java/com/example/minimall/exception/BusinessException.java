package com.example.minimall.exception;

/**
 * 统一业务异常：Controller/Service 遇到可预期的业务错误时抛出，
 * 由 GlobalExceptionHandler 集中处理，返回规范化的 JSON 错误响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    /** 使用自定义业务码构造异常。 */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 使用默认业务码（1）构造异常。 */
    public BusinessException(String message) {
        this(1, message);
    }

    /** 获取业务错误码。 */
    public int getCode() {
        return code;
    }
}
