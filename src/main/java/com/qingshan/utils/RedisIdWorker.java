package com.qingshan.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.qingshan.utils.RedisConstants.INCR_KEY;

/**
 * Redis ID 自生成策略
 */
@Component
public class RedisIdWorker {

    // 2022年1月1日的时间戳，以此为初始值
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成订单id信息
     * @param keyPrefix 业务前缀，用于填充redis中的key
     * @return 生成的订单id
     */
    public long nextId(String keyPrefix) {
        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 生成序列号
        // 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // Redis自增长实现序列号包装
        long count = stringRedisTemplate.opsForValue().increment(INCR_KEY + keyPrefix + ":" + date);

        System.out.println(timestamp);
        System.out.println(count);

        // 采用位运算接收并返回拼接的id
        return timestamp << 32 | count;
    }
}
