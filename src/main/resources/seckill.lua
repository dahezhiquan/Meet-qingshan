-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]

-- 库存key
local stockKey = 'qingshan:seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'qingshan:seckill:order:' .. voucherId

-- 业务
-- 判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end

-- 判断用户是否下单
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 存在，表示用户已经下过单了，此时不允许用户重复下单
    return 2
end

-- 扣减库存
redis.call('incrby', stockKey, -1)

-- 下单，保存用户到已经下单用户set集合中
redis.call('sadd', orderKey, userId)

-- 有抢券资格返回0
return 0