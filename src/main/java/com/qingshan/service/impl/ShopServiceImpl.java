package com.qingshan.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qingshan.dto.Result;
import com.qingshan.entity.Shop;
import com.qingshan.mapper.ShopMapper;
import com.qingshan.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.*;

/**
 * 查询商户服务类接口实现类
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商户信息
     *
     * @param id 商户id
     * @return Result
     */
    @Override
    public Result queryById(Long id) {
        // 缓存击穿 + 缓存穿透 解决方案
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("商户信息不存在！");
        }
        // 返回数据给前端
        return Result.ok(shop);
    }

    /**
     * 缓存击穿解决方案
     *
     * @param id 商户id
     * @return 商户对象
     */
    private Shop queryWithMutex(Long id) {
        // 从redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 判断商户缓存是否存在
        if (StringUtils.isNotBlank(shopJson)) {
            // 此商户缓存存在，直接返回结果
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中的是否为空值 ""，防止缓存穿透
        if ("".equals(shopJson)) {
            return null;
        }

        Shop shop = null;
        try {
            // 实现缓存重建
            // 获取互斥锁
            boolean isLock = tryLock(LOCK_SHOP_KEY + id);
            // 判断是否取锁成功
            if (!isLock) {
                // 失败，则进入休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 缓存中商户信息不存在，查询数据库
            shop = getById(id);


            // 模拟重建的延时
            Thread.sleep(200);


            // 查询数据库不存在，返回错误
            if (shop == null) {
                // 将null值写入Redis，防止缓存穿透问题
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 查询数据库存在，写入数据到Redis中
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            unlock(LOCK_SHOP_KEY + id);
        }
        // 返回数据给前端
        return shop;
    }

    /**
     * 更新商户信息，实现缓存与数据库的双写一致，需要基于事务
     *
     * @param shop 商户对象
     * @return Result
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空！");
        }
        // 更新数据库
        updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
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
     * 将热点商户加入到缓存中，进行预热
     *
     * @param id            商户id
     * @param expireSeconds 逻辑过期时间
     */
    public void saveShopToRedis(Long id, Long expireSeconds) {
        // 查询商户数据
        Shop shop = getById(id);
        // 封装逻辑过期时间对象
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 缓存穿透解决方案，已经集成到queryWithMutex方法
     *
     * @param id 商户id
     * @return 商户对象
     */
    public Shop queryWithPassThrough(Long id) {
        // 从redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 判断商户是否存在
        if (StringUtils.isNotBlank(shopJson)) {
            // 此商户缓存存在，直接返回结果
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中的是否为空值 ""
        if ("".equals(shopJson)) {
            return null;
        }

        // 不存在，查询数据库
        Shop shop = getById(id);
        // 查询数据库不存在，返回错误
        if (shop == null) {
            // 将null值写入Redis，防止缓存穿透问题
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 查询数据库存在，写入数据到Redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 返回数据给前端
        return shop;
    }
}
