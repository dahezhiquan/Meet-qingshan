package com.qingshan.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁的过期时间，防止死锁
     * @return 是否获取成功锁🔒
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
