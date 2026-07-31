package com.example.minimall.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器：统一捕获 Controller 层抛出的异常，并按类型转换为统一的 JSON 错误响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 参数校验失败 (POST JSON body) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, 1, "参数校验失败", errors);
    }

    /** 参数绑定失败 (GET query / form) */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException e) {
        log.warn("参数绑定失败: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : e.getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, 1, "参数校验失败", errors);
    }

    /** 缺少必需请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必需参数: {}", e.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, 1, "缺少必需参数: " + e.getParameterName());
    }

    /** 请求体解析失败（非法 JSON 等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, 1, "请求格式错误");
    }

    /** HTTP 方法不支持（GET 访问了 POST 等） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的 HTTP 方法: {}", e.getMethod());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, 1, "不支持的请求方法: " + e.getMethod());
    }

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [code={}]: {}", e.getCode(), e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    /** 参数非法 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, 1, e.getMessage());
    }

    /** Spring Security 权限拒绝 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限拒绝: {}", e.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, 403, "权限不足");
    }

    /** 数据库完整性约束违反（唯一键冲突、外键等） */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据库完整性约束违反", e);
        return buildResponse(HttpStatus.CONFLICT, 1, "数据冲突，操作失败");
    }

    /** 兜底异常处理（不应暴露 e.getMessage() 给客户端） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("未捕获异常", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, 1, "系统繁忙，请稍后重试");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, int code, String message) {
        return buildResponse(status, code, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, int code, String message, Object errors) {
        Map<String, Object> body = new HashMap<>(4);
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        if (errors != null) {
            body.put("errors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
