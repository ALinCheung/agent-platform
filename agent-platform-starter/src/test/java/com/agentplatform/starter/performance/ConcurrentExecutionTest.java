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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发执行性能测试
 * 测试系统在高并发场景下的稳定性和正确性
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("并发执行性能测试")
class ConcurrentExecutionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** 并发任务数量 */
    private static final int CONCURRENT_COUNT = 10;

    /** 线程池大小 */
    private static final int THREAD_POOL_SIZE = 10;

    /** 超时时间（秒） */
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * 并发提交10个任务，验证所有任务都获得执行ID
     */
    @Test
    @DisplayName("并发提交任务 - 10个任务同时提交，验证全部获得执行ID")
    void concurrentTaskSubmission() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<Long>> futures = new ArrayList<>();

        // 准备并发任务
        for (int i = 0; i < CONCURRENT_COUNT; i++) {
            final int index = i;
            Future<Long> future = executor.submit(() -> {
                startLatch.await(); // 等待统一开始信号

                try {
                    // 创建任务
                    Task task = Task.builder()
                            .name("concurrent-task-" + index + "-" + System.currentTimeMillis())
                            .description("并发测试任务")
                            .command("echo concurrent-test-" + index)
                            .triggerType(TriggerType.CRON)
                            .cronExpression("0 0 * * * ?")
                            .timeoutSeconds(300)
                            .maxRetries(0)
                            .retryIntervalSeconds(60)
                            .enabled(true)
                            .build();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    String json = objectMapper.writeValueAsString(task);
                    HttpEntity<String> createReq = new HttpEntity<>(json, headers);

                    // 创建任务
                    ResponseEntity<Map> createResp = restTemplate.postForEntity(
                            "/api/tasks", createReq, Map.class);

                    if (createResp.getStatusCode() != HttpStatus.CREATED) {
                        return null;
                    }

                    Long taskId = Long.valueOf(createResp.getBody().get("id").toString());

                    // 执行任务
                    HttpEntity<Void> execReq = new HttpEntity<>(headers);
                    ResponseEntity<Map> execResp = restTemplate.postForEntity(
                            "/api/tasks/" + taskId + "/execute", execReq, Map.class);

                    if (execResp.getStatusCode() == HttpStatus.ACCEPTED) {
                        successCount.incrementAndGet();
                        return Long.valueOf(execResp.getBody().get("executionId").toString());
                    }
                    return null;
                } catch (Exception e) {
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }

        // 统一开始
        startLatch.countDown();

        // 等待所有任务完成
        boolean allDone = doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(allDone, "所有任务应在超时时间内完成");

        // 收集结果
        List<Long> executionIds = new ArrayList<>();
        for (Future<Long> future : futures) {
            Long id = future.get(5, TimeUnit.SECONDS);
            if (id != null) {
                executionIds.add(id);
            }
        }

        // 验证结果
        assertEquals(CONCURRENT_COUNT, successCount.get(),
                "所有" + CONCURRENT_COUNT + "个任务应全部成功获得执行ID");
        assertEquals(CONCURRENT_COUNT, executionIds.size(),
                "应获得" + CONCURRENT_COUNT + "个执行ID");

        // 验证执行ID不重复
        assertEquals(CONCURRENT_COUNT, executionIds.stream().distinct().count(),
                "执行ID不应重复");

        executor.shutdown();
    }

    /**
     * 并发查询统计接口，验证无错误
     */
    @Test
    @DisplayName("并发查询统计 - 同时查询多个统计接口，验证无错误")
    void concurrentStatsQueries() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_COUNT * 3); // 3个统计接口
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // 定义统计接口
        String[] endpoints = {
                "/api/stats/overview",
                "/api/stats/tasks",
                "/api/stats/executions"
        };

        // 并发查询
        for (int i = 0; i < CONCURRENT_COUNT; i++) {
            for (String endpoint : endpoints) {
                final String ep = endpoint;
                executor.submit(() -> {
                    startLatch.await();
                    try {
                        ResponseEntity<Map> response = restTemplate.getForEntity(ep, Map.class);
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        // 统一开始
        startLatch.countDown();

        // 等待完成
        boolean allDone = doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(allDone, "所有查询应在超时时间内完成");

        // 验证无错误
        assertEquals(0, errorCount.get(), "并发查询统计接口不应有错误");
        assertEquals(CONCURRENT_COUNT * 3, successCount.get(),
                "所有查询应全部成功");

        executor.shutdown();
    }

    /**
     * 提交任务后检查资源监控，验证系统资源正常
     */
    @Test
    @DisplayName("负载下资源监控 - 提交任务后检查系统资源")
    void resourceUsageUnderLoad() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_COUNT);

        // 先提交一批任务
        for (int i = 0; i < CONCURRENT_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                startLatch.await();
                try {
                    Task task = Task.builder()
                            .name("load-test-task-" + index + "-" + System.currentTimeMillis())
                            .command("echo load-test-" + index)
                            .triggerType(TriggerType.CRON)
                            .cronExpression("0 0 * * * ?")
                            .timeoutSeconds(300)
                            .maxRetries(0)
                            .retryIntervalSeconds(60)
                            .enabled(true)
                            .build();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    String json = objectMapper.writeValueAsString(task);
                    HttpEntity<String> req = new HttpEntity<>(json, headers);

                    restTemplate.postForEntity("/api/tasks", req, Map.class);
                } catch (Exception e) {
                    // 忽略创建失败
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // 检查系统资源
        ResponseEntity<Map> resourceResp = restTemplate.getForEntity(
                "/api/system/resources", Map.class);

        assertEquals(HttpStatus.OK, resourceResp.getStatusCode(), "资源监控接口应返回200");
        assertNotNull(resourceResp.getBody(), "资源信息不应为空");

        // 验证资源信息字段存在
        assertTrue(resourceResp.getBody().containsKey("maxMemoryMb"),
                "应包含maxMemoryMb字段");
        assertTrue(resourceResp.getBody().containsKey("usedMemoryMb"),
                "应包含usedMemoryMb字段");
        assertTrue(resourceResp.getBody().containsKey("freeMemoryMb"),
                "应包含freeMemoryMb字段");
        assertTrue(resourceResp.getBody().containsKey("freeDiskMb"),
                "应包含freeDiskMb字段");
        assertTrue(resourceResp.getBody().containsKey("memoryUsagePercent"),
                "应包含memoryUsagePercent字段");

        // 验证内存使用率合理（不超过95%）
        double memoryUsage = ((Number) resourceResp.getBody().get("memoryUsagePercent")).doubleValue();
        assertTrue(memoryUsage < 95.0,
                "内存使用率不应超过95%，当前: " + memoryUsage + "%");

        // 检查健康状态
        ResponseEntity<Map> healthResp = restTemplate.getForEntity(
                "/api/system/health", Map.class);

        assertEquals(HttpStatus.OK, healthResp.getStatusCode(), "健康检查接口应返回200");
        assertEquals("UP", healthResp.getBody().get("status"), "系统状态应为UP");

        executor.shutdown();
    }
}
