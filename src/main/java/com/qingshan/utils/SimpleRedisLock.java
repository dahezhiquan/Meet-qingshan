package com.qingshan.utils;

import cn.hutool.core.lang.UUID;
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


    // 分布式锁的线程标识前缀，防止多个虚拟机线程ID一致，存入同样的redis分布式锁key中的巧合
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁的过期时间，防止死锁
     * @return 是否获取成功锁🔒
     */
    @Override
    public boolean tryLock(long timeoutSec) {

        // 获取线程标识，随机UUID + 线程id
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY + name, threadId + "", timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        // 获取线程标识，随机UUID + 线程id
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取redis分布式锁中的标识
        String lockId = stringRedisTemplate.opsForValue().get(LOCK_KEY + name);
        // 判断标识是否一致（是否是自己的锁🔒）
        if (threadId.equals(lockId)) {
            // 释放锁
            stringRedisTemplate.delete(LOCK_KEY + name);
        }
    }
}
