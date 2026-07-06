package com.example.minimall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级限流注解，配合 {@code RateLimitInterceptor} 使用。
 *
 * <p>默认在 {@code timeout} 秒内最多允许 {@code limit} 次调用；
 * 实际窗口与计数方式以拦截器实现为准。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 时间窗口内允许的最大请求数。 */
    int limit() default 10;
    /** 限流时间窗口长度（秒）。 */
    long timeout() default 60;
}
