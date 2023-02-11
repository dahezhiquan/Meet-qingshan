package com.qingshan.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 逻辑过期时间的实体支持
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
