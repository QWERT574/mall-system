package com.example.minimall.constants;

/**
 * 通用业务响应码与默认错误信息常量。
 *
 * <p>码值风格参考 HTTP 状态码：0 成功，1 业务错误，401/403/404/500 与 HTTP 语义对齐。
 */
public class ResponseCode {
    public static final int SUCCESS = 0;
    public static final int ERROR = 1;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;

    /** 根据响应码返回对应的中文默认提示；未知码值返回“未知错误”。 */
    public static String getMessage(int code) {
        switch (code) {
            case SUCCESS:
                return "操作成功";
            case ERROR:
                return "操作失败";
            case UNAUTHORIZED:
                return "未授权";
            case FORBIDDEN:
                return "禁止访问";
            case NOT_FOUND:
                return "资源不存在";
            case SERVER_ERROR:
                return "服务器错误";
            default:
                return "未知错误";
        }
    }
}
