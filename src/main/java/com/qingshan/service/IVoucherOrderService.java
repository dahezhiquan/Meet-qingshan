package com.qingshan.service;

import com.qingshan.dto.Result;
import com.qingshan.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 秒杀服务接口类
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀业务
     * @param voucherId 优惠券id
     * @return Result
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 新增订单业务
     * @param voucherId 优惠券id
     * @return Result
     */
    Result createVoucherOrder(Long voucherId);
}
