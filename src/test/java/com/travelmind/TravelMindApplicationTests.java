package com.travelmind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用启动测试
 */
@SpringBootTest(properties = "travelmind.cli.enabled=false")
class TravelMindApplicationTests {

    @Test
    void contextLoads() {
        // 测试 Spring 上下文能否正常加载
    }
}
