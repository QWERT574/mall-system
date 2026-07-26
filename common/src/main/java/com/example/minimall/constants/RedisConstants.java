package com.example.minimall.constants;

/**
 * Redis 缓存键与过期时间常量。
 *
 * <p>统一维护商品、分类、用户等缓存 Key 前缀及通用 TTL（秒）。
 */
public class RedisConstants {
    public static final String PRODUCT_CACHE_KEY = "product:";
    public static final String CATEGORY_CACHE_KEY = "category:";
    public static final String USER_CACHE_KEY = "user:";
    public static final long DEFAULT_EXPIRE_TIME = 3600L;
    public static final long ONE_HOUR_EXPIRE_TIME = 3600L;
    public static final long ONE_DAY_EXPIRE_TIME = 86400L;
}
