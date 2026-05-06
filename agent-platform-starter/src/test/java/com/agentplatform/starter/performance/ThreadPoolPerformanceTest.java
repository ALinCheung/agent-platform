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
 * 线程池性能测试
 * 测试满载排队、不死锁
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("线程池性能测试")
class ThreadPoolPerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** 提交任务数量 - 超过线程池容量 */
    private static final int TASK_COUNT = 20;

    /** 超时时间（秒） */
    private static final int TIMEOUT_SECONDS = 60;

    @Test
    @DisplayName("线程池满载 - 提交超过线程池容量的任务，验证不死锁")
    void threadPool_doesNotDeadlock_underFullLoad() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(TASK_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < TASK_COUNT; i++) {
            final int index = i;
            Future<Boolean> future = executor.submit(() -> {
                startLatch.await();
                try {
                    Task task = Task.builder()
                            .name("pool-test-" + index + "-" + System.currentTimeMillis())
                            .command("echo pool-test-" + index)
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

                    ResponseEntity<Map> resp = restTemplate.postForEntity("/api/tasks", req, Map.class);

                    if (resp.getStatusCode() == HttpStatus.CREATED) {
                        Long taskId = Long.valueOf(resp.getBody().get("id").toString());

                        // 提交执行
                        HttpEntity<Void> execReq = new HttpEntity<>(headers);
                        ResponseEntity<Map> execResp = restTemplate.postForEntity(
                                "/api/tasks/" + taskId + "/execute", execReq, Map.class);

                        if (execResp.getStatusCode() == HttpStatus.ACCEPTED) {
                            successCount.incrementAndGet();
                            return true;
                        }
                    }
                    errorCount.incrementAndGet();
                    return false;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    return false;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }

        // 统一开始
        startLatch.countDown();

        // 等待完成（应不死锁）
        boolean allDone = doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(allDone, "所有任务应在超时时间内完成（不死锁）");

        // 验证大部分任务成功
        assertTrue(successCount.get() > 0, "至少应有部分任务成功提交");

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "线程池应正常关闭");
    }

    @Test
    @DisplayName("线程池排队 - 验证任务排队机制正常工作")
    void threadPool_queuing_worksCorrectly() throws Exception {
        // 快速提交多个任务
        int submitCount = 5;
        List<Long> executionIds = new ArrayList<>();

        for (int i = 0; i < submitCount; i++) {
            Task task = Task.builder()
                    .name("queue-test-" + i + "-" + System.currentTimeMillis())
                    .command("echo queue-test-" + i)
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

            ResponseEntity<Map> createResp = restTemplate.postForEntity("/api/tasks", req, Map.class);
            Long taskId = Long.valueOf(createResp.getBody().get("id").toString());

            HttpEntity<Void> execReq = new HttpEntity<>(headers);
            ResponseEntity<Map> execResp = restTemplate.postForEntity(
                    "/api/tasks/" + taskId + "/execute", execReq, Map.class);

            if (execResp.getStatusCode() == HttpStatus.ACCEPTED) {
                executionIds.add(Long.valueOf(execResp.getBody().get("executionId").toString()));
            }
        }

        // 验证所有任务都获得了执行ID
        assertEquals(submitCount, executionIds.size(), "所有任务应都获得执行ID");

        // 验证执行ID不重复
        assertEquals(submitCount, executionIds.stream().distinct().count(), "执行ID不应重复");
    }
}
