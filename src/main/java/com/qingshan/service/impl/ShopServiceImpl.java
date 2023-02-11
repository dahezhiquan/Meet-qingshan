package com.qingshan.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qingshan.dto.Result;
import com.qingshan.entity.Shop;
import com.qingshan.mapper.ShopMapper;
import com.qingshan.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    /**
     * 根据id查询商户信息
     *
     * @param id 商户id
     * @return Result
     */
    @Override
    public Result queryById(Long id) {
        // 从redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 判断商户是否存在
        if (StringUtils.isNotBlank(shopJson)) {
            // 此商户缓存存在，直接返回结果
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }

        // 判断命中的是否为空值 ""
        if ("".equals(shopJson)) {
            return Result.fail("店铺信息不存在！");
        }

        // 不存在，查询数据库
        Shop shop = getById(id);
        // 查询数据库不存在，返回错误
        if (shop == null) {
            // 将null值写入Redis，防止缓存穿透问题
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在！");
        }
        // 查询数据库存在，写入数据到Redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
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
