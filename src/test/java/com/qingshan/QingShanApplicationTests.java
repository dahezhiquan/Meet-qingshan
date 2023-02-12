package com.qingshan;

import com.qingshan.utils.RedisIdWorker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class QingShanApplicationTests {
    @Resource
    private RedisIdWorker redisIdWorker;

    /**
     * 生成订单id信息单元测试
     */
    @Test
    public void testIdWorker() {
        long test = redisIdWorker.nextId("test");
        System.out.println(test);
    }
}
