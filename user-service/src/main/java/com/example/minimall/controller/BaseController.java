package com.example.minimall.controller;

import com.example.minimall.common.Result;

/** Controller 基类，统一封装成功/失败响应 */
public class BaseController {

    /** 封装成功响应 */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /** 封装默认错误码的错误响应 */
    protected <T> Result<T> error(String message) {
        return Result.error(message);
    }

    /** 封装自定义错误码的错误响应 */
    protected <T> Result<T> error(Integer code, String message) {
        return Result.error(code, message);
    }
}
