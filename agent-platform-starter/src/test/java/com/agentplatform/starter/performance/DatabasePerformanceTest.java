package com.agentplatform.starter.performance;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.starter.AgentPlatformApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库性能测试
 * 测试大量记录插入、查询性能
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("数据库性能测试")
class DatabasePerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** 批量插入任务数量 */
    private static final int BATCH_SIZE = 50;

    @Test
    @DisplayName("批量插入 - 大量任务创建性能")
    void batchInsert_performance() throws Exception {
        long startTime = System.currentTimeMillis();
        List<Long> taskIds = new ArrayList<>();

        for (int i = 0; i < BATCH_SIZE; i++) {
            Task task = Task.builder()
                    .name("db-perf-" + i + "-" + System.currentTimeMillis())
                    .command("echo db-test-" + i)
                    .triggerType(TriggerType.CRON)
                    .cronExpression("0 0 * * * ?")
                    .timeoutSeconds(300)
                    .maxRetries(0)
                    .retryIntervalSeconds(60)
                    .enabled(true)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

            ResponseEntity<Task> resp = restTemplate.postForEntity("/api/tasks", req, Task.class);
            assertEquals(HttpStatus.CREATED, resp.getStatusCode(), "创建任务应返回201");
            taskIds.add(resp.getBody().getId());
        }

        long duration = System.currentTimeMillis() - startTime;

        // 验证所有任务创建成功
        assertEquals(BATCH_SIZE, taskIds.size(), "应创建" + BATCH_SIZE + "个任务");

        // 性能基准：50个任务应在30秒内完成
        assertTrue(duration < 30000,
                "批量插入" + BATCH_SIZE + "个任务应在30秒内完成，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("批量查询 - 大量任务查询性能")
    void batchQuery_performance() throws Exception {
        // 先创建一些任务
        for (int i = 0; i < 10; i++) {
            Task task = Task.builder()
                    .name("db-query-" + i + "-" + System.currentTimeMillis())
                    .command("echo query-test-" + i)
                    .triggerType(TriggerType.CRON)
                    .cronExpression("0 0 * * * ?")
                    .timeoutSeconds(300)
                    .maxRetries(0)
                    .retryIntervalSeconds(60)
                    .enabled(true)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);
            restTemplate.postForEntity("/api/tasks", req, Task.class);
        }

        // 测试查询性能
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            ResponseEntity<String> resp = restTemplate.getForEntity("/api/tasks", String.class);
            assertEquals(HttpStatus.OK, resp.getStatusCode(), "查询任务列表应返回200");
        }

        long duration = System.currentTimeMillis() - startTime;

        // 性能基准：100次查询应在10秒内完成
        assertTrue(duration < 10000,
                "100次任务列表查询应在10秒内完成，实际: " + duration + "ms");
    }

    @Test
    @DisplayName("索引有效性 - 按状态查询性能")
    void indexEffectiveness_statusQuery() {
        long startTime = System.currentTimeMillis();

        // 执行统计查询
        ResponseEntity<Map> statsResp = restTemplate.getForEntity("/api/stats/overview", Map.class);
        assertEquals(HttpStatus.OK, statsResp.getStatusCode(), "统计查询应返回200");

        long duration = System.currentTimeMillis() - startTime;

        // 性能基准：统计查询应在1秒内完成
        assertTrue(duration < 1000,
                "统计查询应在1秒内完成，实际: " + duration + "ms");
    }
}
