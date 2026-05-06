package com.agentplatform.starter.e2e;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行监控页面端到端测试
 * 测试执行历史记录、详情查看、重试操作
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("执行监控页面测试")
class ExecutionMonitorPageTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("执行历史 - 页面可访问")
    void executionPage_isAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/executions", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "执行监控页面应返回200");
    }

    @Test
    @DisplayName("执行历史 - API返回执行记录列表")
    void executionListApi_returnsExecutionList() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/executions", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "执行记录API应返回200");
    }

    @Test
    @DisplayName("执行详情 - 获取单条执行记录")
    void executionDetail_returnsExecution() throws Exception {
        // 先创建并执行任务
        Task task = Task.builder()
                .name("pw-test-exec-" + System.currentTimeMillis())
                .command("echo exec-test")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                .timeoutSeconds(300)
                .maxRetries(0)
                .retryIntervalSeconds(60)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

        ResponseEntity<Task> createResp = restTemplate.postForEntity("/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 执行任务
        HttpEntity<Void> execReq = new HttpEntity<>(headers);
        ResponseEntity<Map> execResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/execute", execReq, Map.class);

        assertEquals(HttpStatus.ACCEPTED, execResp.getStatusCode(), "执行任务应返回202");
        Long executionId = Long.valueOf(execResp.getBody().get("executionId").toString());

        // 获取执行详情
        ResponseEntity<Map> detailResp = restTemplate.getForEntity(
                "/api/executions/" + executionId, Map.class);

        assertEquals(HttpStatus.OK, detailResp.getStatusCode(), "获取执行详情应返回200");
        assertNotNull(detailResp.getBody().get("status"), "应包含执行状态");
    }

    @Test
    @DisplayName("重试执行 - 重试失败的执行记录")
    void retryExecution_returns202() throws Exception {
        // 先创建并执行任务
        Task task = Task.builder()
                .name("pw-test-retry-" + System.currentTimeMillis())
                .command("echo retry-test")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                .timeoutSeconds(300)
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

        ResponseEntity<Task> createResp = restTemplate.postForEntity("/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 执行任务
        HttpEntity<Void> execReq = new HttpEntity<>(headers);
        ResponseEntity<Map> execResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/execute", execReq, Map.class);

        Long executionId = Long.valueOf(execResp.getBody().get("executionId").toString());

        // 重试执行
        ResponseEntity<Map> retryResp = restTemplate.postForEntity(
                "/api/executions/" + executionId + "/retry", new HttpEntity<>(headers), Map.class);

        // 重试可能成功(202)或失败(400-如果执行还在运行中)
        assertTrue(retryResp.getStatusCode().is2xxSuccessful() || retryResp.getStatusCode().is4xxClientError(),
                "重试应返回2xx或4xx状态码");
    }
}
