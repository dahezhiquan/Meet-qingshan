package com.qingshan.service;

import com.qingshan.dto.Result;
import com.qingshan.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 查询商户服务类接口
 */
public interface IShopService extends IService<Shop> {
    // 根据id查询商户信息
    public Result queryById(Long id);
}
