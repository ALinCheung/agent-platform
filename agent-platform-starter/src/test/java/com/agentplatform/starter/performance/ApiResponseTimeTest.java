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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API响应时间性能测试
 * 测试关键API接口响应时间 < 500ms
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("API响应时间测试")
class ApiResponseTimeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /** 响应时间阈值（毫秒） */
    private static final long RESPONSE_TIME_THRESHOLD = 500;

    @Test
    @DisplayName("任务列表API - 响应时间<500ms")
    void taskListApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/tasks", String.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "任务列表API响应时间应<500ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("统计概览API - 响应时间<500ms")
    void statsOverviewApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/stats/overview", Map.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "统计概览API响应时间应<500ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("任务统计API - 响应时间<500ms")
    void taskStatsApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/stats/tasks", Map.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "任务统计API响应时间应<500ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("执行统计API - 响应时间<500ms")
    void executionStatsApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/stats/executions", Map.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "执行统计API响应时间应<500ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("健康检查API - 响应时间<500ms")
    void healthCheckApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/system/health", Map.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "健康检查API响应时间应<500ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("资源监控API - 响应时间<500ms")
    void resourceMonitorApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/system/resources", Map.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "资源监控API响应时间应<500ms，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("执行记录API - 响应时间<500ms")
    void executionListApi_responseTime() {
        long start = System.currentTimeMillis();
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/executions", String.class);
        long duration = System.currentTimeMillis() - start;

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(duration < RESPONSE_TIME_THRESHOLD,
                "执行记录API响应时间应<500ms，实际: " + duration + "ms");
    }
}
