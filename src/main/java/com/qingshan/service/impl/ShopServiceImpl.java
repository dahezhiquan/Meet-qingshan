package com.qingshan.service.impl;

import com.qingshan.dto.Result;
import com.qingshan.entity.Shop;
import com.qingshan.mapper.ShopMapper;
import com.qingshan.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.*;

/**
 * 查询商户服务类接口实现类
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据id查询商户信息
     *
     * @param id 商户id
     * @return Result
     */
    @Override
    public Result queryById(Long id) {
        // 根据id查询商户信息，解决 缓存穿透 + 缓存击穿 问题
        Shop shop = cacheClient.
                queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("商户信息不存在！");
        }
        // 返回数据给前端
        return Result.ok(shop);
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

}
