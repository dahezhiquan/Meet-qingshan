package com.qingshan.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.qingshan.utils.RedisConstants.*;

/**
 * redis工具类
 *
 * @author dahezhiquan
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 添加key到Redis中，以TTL过期的方式添加
     *
     * @param key   键
     * @param value 值
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 添加key到Redis中，以逻辑过期的方式添加
     *
     * @param key   键
     * @param value 值
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期对象
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value));
    }

    /**
     * 根据id获取缓存数据，顺便解决缓存穿透问题
     *
     * @param keyPrefix  存入Reids中的key前缀字符串
     * @param id         查询的id
     * @param type       实体对象的类型
     * @param dbFallback 函数式接口对象，对应反馈查询数据库的操作
     * @param time       写入数据到Redis中的时间
     * @param unit       写入数据到Redis中的时间类型
     * @param <R>        查询对象类型泛型
     * @param <ID>       查询的ID的类型泛型
     * @return 查询数据对象
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断缓存查询结果是否真实存在
        if (StringUtils.isNotBlank(json)) {
            // 此缓存存在，直接返回结果
            return JSONUtil.toBean(json, type);
        }

        // 判断命中的是否为空值 ""，防止缓存穿透现象
        if ("".equals(json)) {
            return null;
        }

        // 缓存中信息不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 查询数据库不存在，返回错误
        if (r == null) {
            // 将null值写入Redis，防止缓存穿透问题
            this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 查询数据库存在，写入数据到Redis中
        this.set(key, r, time, unit);
        // 返回数据给前端
        return r;
    }

    /**
     * 根据id获取缓存数据，顺便解决缓存穿透 + 缓存击穿问题
     *
     * @param keyPrefix  存入Reids中的key前缀字符串
     * @param id         查询的id
     * @param type       实体对象的类型
     * @param dbFallback 函数式接口对象，对应反馈查询数据库的操作
     * @param time       写入数据到Redis中的时间
     * @param unit       写入数据到Redis中的时间类型
     * @param <R>        查询对象类型泛型
     * @param <ID>       查询的ID的类型泛型
     * @return 查询数据对象
     */
    public  <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断缓存查询结果是否存在
        if (StringUtils.isNotBlank(json)) {
            // 缓存存在，直接返回结果
            return JSONUtil.toBean(json, type);
        }

        // 判断命中的是否为空值 ""，防止缓存穿透
        if ("".equals(json)) {
            return null;
        }

        R r = null;
        try {
            // 实现缓存重建
            // 获取互斥锁
            boolean isLock = tryLock(LOCK_SHOP_KEY + id);
            // 判断是否取锁成功
            if (!isLock) {
                // 失败，则进入休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }

            // 缓存中信息不存在，根据id查询数据库
            r = dbFallback.apply(id);

            // 查询数据库不存在，返回错误
            if (r == null) {
                // 将null值写入Redis，防止缓存穿透问题
                this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 查询数据库存在，写入数据到Redis中
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            unlock(LOCK_SHOP_KEY + id);
        }
        // 返回数据给前端
        return r;
    }


    /**
     * 尝试获取锁，解决缓存击穿问题方案
     *
     * @param key key
     */
    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    /**
     * 删除锁，解决缓存击穿问题方案
     *
     * @param key key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


    /**
     * 根据id提前预热热点key的数据
     *
     * @param keyPrefix     存入Reids中的key前缀字符串
     * @param id            热点数据id
     * @param expireSeconds 逻辑过期时间
     * @param unit          逻辑过期时间单位
     * @param dbFallback    函数式接口对象，对应反馈查询数据库的操作
     * @param <R>           预热对象类型泛型
     * @param <ID>          预热对象id
     */
    public <R, ID> void saveDataToRedis(String keyPrefix, ID id, Long expireSeconds, TimeUnit unit, Function<ID, R> dbFallback) {
        // 根据id查询数据库
        R r = dbFallback.apply(id);
        this.setWithLogicalExpire(keyPrefix + id, r, expireSeconds, unit);
    }
}
