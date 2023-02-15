package com.qingshan.utils;

/**
 * Redis存储常量定义池
 */
public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "qingshan:login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "qingshan:login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    // 缓存null值的过期时间
    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "qingshan:cache:shop:";

    public static final String CACHE_SHOP_TYPE_KEY = "qingshan:cache:shoptype";

    public static final Long CACHE_SHOP_TYPE_TTL = 120L;

    // 解决缓存击穿问题的锁前缀
    public static final String LOCK_SHOP_KEY = "qingshan:lock:shop:";

    // Redis ID 自生成策略序列号的前缀
    public static final String INCR_KEY = "qiangshan:icr:";
    public static final Long LOCK_SHOP_TTL = 10L;

    // 实现的分布式锁的前缀
    public static final String LOCK_KEY = "qingshan:lock:";

    // 秒杀券的库存信息
    public static final String SECKILL_STOCK_KEY = "qingshan:seckill:stock:";
    public static final String BLOG_LIKED_KEY = "qingshan:blog:liked:";

    public static final String FOLLOW_KEY = "qingshan:follow:";
    public static final String FEED_KEY = "qingshan:feed:";
    // public static final String SHOP_GEO_KEY = "qingshan:shop:geo:";

    // 签到key前缀
    public static final String USER_SIGN_KEY = "qingshan:sign:";
}
