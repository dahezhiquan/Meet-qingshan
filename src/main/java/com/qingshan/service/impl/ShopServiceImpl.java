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

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.qingshan.utils.RedisConstants.CACHE_SHOP_TTL;

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
        // 不存在，查询数据库
        Shop shop = getById(id);
        // 查询数据库不存在，返回错误
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 查询数据库存在，写入数据到Redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 返回数据给前端
        return Result.ok(shop);
    }
}
