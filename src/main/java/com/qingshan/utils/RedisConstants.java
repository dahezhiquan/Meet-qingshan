package com.qingshan.utils;

/**
 * Redis存储常量定义池
 */
public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "qingshan:login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "qingshan:login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "qingshan:cache:shop:";

    public static final String LOCK_SHOP_KEY = "qingshan:lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "qingshan:seckill:stock:";
    public static final String BLOG_LIKED_KEY = "qingshan:blog:liked:";
    public static final String FEED_KEY = "qingshan:feed:";
    public static final String SHOP_GEO_KEY = "qingshan:shop:geo:";
    public static final String USER_SIGN_KEY = "qingshan:sign:";
}
