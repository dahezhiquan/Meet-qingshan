package com.qingshan.service;

import com.qingshan.dto.Result;
import com.qingshan.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 查询商户类型服务接口
 */
public interface IShopTypeService extends IService<ShopType> {
    // 查询所有商户类型
    public Result queryShopType();
}
