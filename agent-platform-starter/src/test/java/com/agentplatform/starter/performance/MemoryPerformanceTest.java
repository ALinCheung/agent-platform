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
 * 内存使用性能测试
 * 测试无内存泄漏
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("内存使用性能测试")
class MemoryPerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("内存监控 - 多次查询后内存不显著增长")
    void memoryUsage_doesNotGrowSignificantly_afterRepeatedQueries() {
        // 记录初始内存
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // 执行大量API调用
        for (int i = 0; i < 100; i++) {
            restTemplate.getForEntity("/api/stats/overview", Map.class);
            restTemplate.getForEntity("/api/stats/tasks", Map.class);
            restTemplate.getForEntity("/api/stats/executions", Map.class);
        }

        // 强制GC
        runtime.gc();

        // 记录最终内存
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryGrowth = finalMemory - initialMemory;

        // 内存增长不应超过50MB
        long maxGrowthBytes = 50 * 1024 * 1024;
        assertTrue(memoryGrowth < maxGrowthBytes,
                "内存增长不应超过50MB，当前增长: " + (memoryGrowth / 1024 / 1024) + "MB");
    }

    @Test
    @DisplayName("内存监控 - 系统资源接口返回合理值")
    void resourceMonitor_returnsReasonableValues() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/system/resources", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "资源监控接口应返回200");
        assertNotNull(response.getBody(), "资源信息不应为空");

        // 验证内存值合理
        double memoryUsage = ((Number) response.getBody().get("memoryUsagePercent")).doubleValue();
        assertTrue(memoryUsage > 0 && memoryUsage < 100,
                "内存使用率应在0-100之间，当前: " + memoryUsage);

        int maxMemory = ((Number) response.getBody().get("maxMemoryMb")).intValue();
        assertTrue(maxMemory > 0, "最大内存应大于0");

        int usedMemory = ((Number) response.getBody().get("usedMemoryMb")).intValue();
        assertTrue(usedMemory > 0 && usedMemory <= maxMemory,
                "已用内存应在合理范围内");
    }

    @Test
    @DisplayName("内存监控 - 健康检查接口稳定")
    void healthCheck_remainsStable_afterMultipleCalls() {
        for (int i = 0; i < 50; i++) {
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/system/health", Map.class);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "健康检查应始终返回200");
            assertEquals("UP", response.getBody().get("status"), "系统状态应始终为UP");
        }
    }
}
