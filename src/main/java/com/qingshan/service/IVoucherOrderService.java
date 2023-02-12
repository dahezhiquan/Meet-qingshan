package com.qingshan.service;

import com.qingshan.dto.Result;
import com.qingshan.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 秒杀服务接口类
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
}
