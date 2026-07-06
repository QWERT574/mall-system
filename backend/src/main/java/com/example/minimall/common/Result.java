package com.example.minimall.common;

/**
 * 统一接口返回结果封装。
 *
 * <p>code = 0 表示成功，1 表示通用错误；data 字段用于承载业务数据。
 */
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 构造一个 code=0、message=success 的成功结果。 */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    /** 构造一个 code=1 的通用错误结果。 */
    public static <T> Result<T> error(String message) {
        return new Result<>(1, message, null);
    }

    /** 构造一个自定义错误码的错误结果。 */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
}
