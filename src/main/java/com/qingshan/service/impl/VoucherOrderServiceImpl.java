package com.qingshan.service.impl;

import com.qingshan.dto.Result;
import com.qingshan.entity.SeckillVoucher;
import com.qingshan.entity.VoucherOrder;
import com.qingshan.mapper.VoucherOrderMapper;
import com.qingshan.service.ISeckillVoucherService;
import com.qingshan.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.utils.RedisIdWorker;
import com.qingshan.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 秒杀服务实现类
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;


    /**
     * 秒杀实现
     *
     * @param voucherId 秒杀券的ID
     * @return Result
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        // 判断秒杀是否已经开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始呢！");
        }
        // 判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束啦！");
        }

        // 判断库存是否充足
        // 库存不足！
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足啦！");
        }

        // 得到用户ID
        Long userId = UserHolder.getUser().getId();

        // 创建新订单，基于悲观锁实现一人一单功能
        synchronized (userId.toString().intern()) {
            // 为了防止事务失效，这里使用代理对象调用方法
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // 返回订单id
            return proxy.createVoucherOrder(voucherId);
        }
    }

    /**
     * 创建新订单，基于悲观锁实现一人一单功能
     *
     * @param voucherId 优惠券id
     * @return Result
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 得到用户ID
        Long userId = UserHolder.getUser().getId();

        // 判断用户是否已经抢过该优惠券
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("已经抢过啦！");
        }

        // 扣减库存
        boolean isDeduction = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!isDeduction) {
            return Result.fail("库存不足啦！");
        }

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return Result.ok(orderId);
    }

}
