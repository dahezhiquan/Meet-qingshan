package com.qingshan.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.LOCK_KEY;

public class SimpleRedisLock implements ILock {

    private String name;

    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁的过期时间，防止死锁
     * @return 是否获取成功锁🔒
     */
    @Override
    public boolean tryLock(long timeoutSec) {

        // 获取线程标识
        long threadId = Thread.currentThread().getId();

        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY + name, threadId + "", timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        stringRedisTemplate.delete(LOCK_KEY + name);
    }
}
