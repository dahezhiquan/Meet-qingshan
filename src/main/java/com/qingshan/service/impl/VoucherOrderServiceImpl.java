package com.qingshan.service.impl;

import com.qingshan.dto.Result;
import com.qingshan.entity.VoucherOrder;
import com.qingshan.mapper.VoucherOrderMapper;
import com.qingshan.service.ISeckillVoucherService;
import com.qingshan.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.utils.RedisIdWorker;
import com.qingshan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.qingshan.utils.RedisConstants.LOCK_KEY;

/**
 * 秒杀服务实现类
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 引入秒杀Lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 创建异步下单的阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 创建异步下单的线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * 设置@PostConstruct注解，表示在项目初始化完毕之后即执行VoucherOrderHandler方法
     * 保证异步下单的流畅性
     */
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 异步执行下单任务的方法
     */
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            // 时时刻刻检测是否有待下单的任务
            while (true) {
                // 获取队列中的订单信息
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 执行处理订单的方法
                    handlerVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    /**
     * 处理订单
     *
     * @param voucherOrder 订单信息
     */
    private void handlerVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 这里使用Redisson的锁改进我们自己实现的锁🔒
        RLock lock = redissonClient.getLock(LOCK_KEY + "order:" + userId);

        // 获取锁
        boolean isLock = lock.tryLock();
        // 获取锁失败，代表当前用户在多次抢券
        if (!isLock) {
            log.error("不允许重复抢券！");
        }

        try {
            // 保存新订单到数据库中
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 手动释放锁
            lock.unlock();
        }
    }


    // 主线程拿到的代理对象
    private IVoucherOrderService proxy;

    /**
     * 秒杀实现，优化版，redis判断库存 + 消息队列
     *
     * @param voucherId 秒杀券的ID
     * @return Result
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户信息
        Long userId = UserHolder.getUser().getId();
        // 执行Lua脚本，判断有无抢券资格
        Long resultLong = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );

        assert resultLong != null;
        int result = resultLong.intValue();
        // 没有购买资格，返回异常
        if (result != 0) {
            return Result.fail(result == 1 ? "库存不足！" : "不能重复抢券哦！");
        }

        // 有购买资格，将下单的信息保存到阻塞队列中
        // 获取订单ID
        long orderId = redisIdWorker.nextId("order");
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);  // 订单ID
        voucherOrder.setUserId(userId);  // 用户ID
        voucherOrder.setVoucherId(voucherId);  // 代金券ID
        orderTasks.add(voucherOrder);  // 放入阻塞队列
        // 为了防止事务失效，这里使用代理对象调用方法
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 返回订单id
        return Result.ok(orderId);
    }

    /**
     * 创建新订单，基于悲观锁实现一人一单功能
     *
     * @param voucherOrder 待创建订单的对象
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 得到用户ID
        Long userId = voucherOrder.getUserId();
        System.out.println(userId);

        // 判断用户是否已经抢过该优惠券
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("已经抢过啦！");
            return;
        }

        // 扣减库存
        boolean isDeduction = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!isDeduction) {
            log.error("库存不足啦！");
            return;
        }
        // 保存订单
        save(voucherOrder);
    }


    /**
     * 秒杀实现，普通版
     *
     * @param voucherId 秒杀券的ID
     * @return Result
     */
    /*
    @Deprecated
    public Result seckillVoucher_no(Long voucherId) {
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

        // 创建锁对象
        // 我们自己实现的锁
        // SimpleRedisLock lock = new SimpleRedisLock("order" + userId, stringRedisTemplate);

        // 这里使用Redisson的锁改进我们自己实现的锁🔒
        RLock lock = redissonClient.getLock(LOCK_KEY + "order:" + userId);

        // 获取锁
        boolean isLock = lock.tryLock();
        // 获取锁失败，代表当前用户在多次抢券
        if (!isLock) {
            return Result.fail("您已经抢过了哦~");
        }

        try {
            // 为了防止事务失效，这里使用代理对象调用方法
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // 返回订单id
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 手动释放锁
            lock.unlock();
        }
    }
    */
}
