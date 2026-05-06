package com.agentplatform.starter.e2e;

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
 * 仪表盘页面端到端测试
 * 测试任务统计、分类统计和历史趋势图表的数据接口
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("仪表盘页面测试")
class DashboardPageTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("仪表盘 - 页面可访问")
    void dashboardPage_isAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "首页应返回200");
        assertNotNull(response.getBody(), "页面内容不应为空");
    }

    @Test
    @DisplayName("仪表盘 - 统计概览接口返回数据")
    void dashboardStats_overview_returnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/stats/overview", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "统计概览接口应返回200");
        assertNotNull(response.getBody(), "统计数据不应为空");
    }

    @Test
    @DisplayName("仪表盘 - 任务统计接口返回数据")
    void dashboardStats_taskStats_returnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/stats/tasks", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "任务统计接口应返回200");
    }

    @Test
    @DisplayName("仪表盘 - 执行统计接口返回数据")
    void dashboardStats_executionStats_returnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/stats/executions", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "执行统计接口应返回200");
    }

    @Test
    @DisplayName("仪表盘 - 历史趋势接口返回数据")
    void dashboardStats_historyTrend_returnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/stats/history?days=7", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "历史趋势接口应返回200");
    }
}
