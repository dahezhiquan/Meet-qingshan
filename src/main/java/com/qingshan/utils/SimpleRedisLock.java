package com.qingshan.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.LOCK_KEY;

/**
 * Redis实现的分布式锁
 */
public class SimpleRedisLock implements ILock {

    private String name;

    private StringRedisTemplate stringRedisTemplate;


    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    // 分布式锁的线程标识前缀，防止多个虚拟机线程ID一致，存入同样的redis分布式锁key中的巧合
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    // 引入Lua脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

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
        // 调用Lua脚本解决释放锁的原子性问题
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_KEY + name),
                ID_PREFIX + Thread.currentThread().getId());

    }
}
