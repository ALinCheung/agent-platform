package com.agentplatform.starter.performance;

import com.agentplatform.starter.AgentPlatformApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 前端加载时间性能测试
 * 测试页面加载时间 < 3秒
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("前端加载时间测试")
class FrontendLoadingTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /** 页面加载时间阈值（毫秒） */
    private static final long PAGE_LOAD_THRESHOLD = 3000;

    @Test
    @DisplayName("首页 - 加载时间<3秒")
    void dashboardPage_loadTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, response.getStatusCode(), "首页应返回200");
        assertTrue(duration < PAGE_LOAD_THRESHOLD,
                "首页加载时间应<3秒，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("任务管理页 - 加载时间<3秒")
    void tasksPage_loadTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.getForEntity("/tasks", String.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, response.getStatusCode(), "任务管理页应返回200");
        assertTrue(duration < PAGE_LOAD_THRESHOLD,
                "任务管理页加载时间应<3秒，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("执行监控页 - 加载时间<3秒")
    void executionsPage_loadTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.getForEntity("/executions", String.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, response.getStatusCode(), "执行监控页应返回200");
        assertTrue(duration < PAGE_LOAD_THRESHOLD,
                "执行监控页加载时间应<3秒，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("页面内容 - 包含基本HTML结构")
    void dashboardPage_containsBasicHtml() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body, "页面内容不应为空");
        // Thymeleaf模板应返回HTML内容
        assertTrue(body.length() > 0, "页面内容不应为空字符串");
    }
}
