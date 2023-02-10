package com.qingshan.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qingshan.dto.Result;
import com.qingshan.entity.ShopType;
import com.qingshan.mapper.ShopTypeMapper;
import com.qingshan.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.qingshan.utils.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * 查询商户类型接口实现类
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询所有商户类型
     *
     * @return Result
     */
    @Override
    public Result queryShopType() {
        // 从redis查询商户类型缓存
        String shopTypeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        // 判断商户类型缓存是否存在
        if (StringUtils.isNotBlank(shopTypeJson)) {
            // 商户类型缓存存在，直接返回结果
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(shopTypeList);
        }
        // 不存在，查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        // 查询数据库不存在，返回错误
        if (shopTypeList == null) {
            return Result.fail("店铺类型不存在！");
        }
        // 查询数据库存在，写入数据到Redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypeList), CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        // 返回数据给前端
        return Result.ok(shopTypeList);
    }
}
