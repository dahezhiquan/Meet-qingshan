package com.qingshan;

import com.qingshan.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class QingShanApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    /**
     * 添加热点商户到Redis缓存的测试类
     */
    @Test
    public void testSaveShop() {
        shopService.saveShopToRedis(1L, 10L);
    }
}
