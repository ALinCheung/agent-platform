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
 * Webhook触发任务执行流程端到端测试
 * 测试Webhook接收事件 → 触发任务执行的完整流程
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("Webhook触发执行流程测试")
class WebhookExecutionFlowTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Webhook流程 - 创建Webhook任务并通过Webhook触发执行")
    void webhookFlow_createTaskAndTriggerViaWebhook() throws Exception {
        // 创建Webhook类型任务
        Task task = Task.builder()
                .name("pw-test-webhook-flow-" + System.currentTimeMillis())
                .command("echo webhook-triggered")
                .triggerType(TriggerType.WEBHOOK)
                .webhookPath("/hook/test-flow-" + System.currentTimeMillis())
                .timeoutSeconds(300)
                .maxRetries(0)
                .retryIntervalSeconds(60)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

        ResponseEntity<Task> createResp = restTemplate.postForEntity("/api/tasks", createReq, Task.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode(), "创建Webhook任务应返回201");

        // 通过Webhook触发执行
        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
        String webhookBody = "{\"message\": \"test webhook\"}";
        HttpEntity<String> webhookReq = new HttpEntity<>(webhookBody, webhookHeaders);

        ResponseEntity<Map> webhookResp = restTemplate.postForEntity(
                "/webhook" + task.getWebhookPath(), webhookReq, Map.class);

        assertEquals(HttpStatus.ACCEPTED, webhookResp.getStatusCode(), "Webhook触发应返回202");
        assertNotNull(webhookResp.getBody().get("executionId"), "应返回执行记录ID");
    }

    @Test
    @DisplayName("Webhook流程 - 带密钥的Webhook验证")
    void webhookFlow_withSecret_validatesCorrectly() throws Exception {
        // 创建带密钥的Webhook任务
        Task task = Task.builder()
                .name("pw-test-webhook-secret-" + System.currentTimeMillis())
                .command("echo webhook-with-secret")
                .triggerType(TriggerType.WEBHOOK)
                .webhookPath("/hook/test-secret-" + System.currentTimeMillis())
                .webhookSecret("test-secret-key-123")
                .timeoutSeconds(300)
                .maxRetries(0)
                .retryIntervalSeconds(60)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

        ResponseEntity<Task> createResp = restTemplate.postForEntity("/api/tasks", createReq, Task.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode(), "创建Webhook任务应返回201");

        // 不带密钥访问 - 应返回401
        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> noSecretReq = new HttpEntity<>("{}", webhookHeaders);

        ResponseEntity<Map> noSecretResp = restTemplate.postForEntity(
                "/webhook" + task.getWebhookPath(), noSecretReq, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, noSecretResp.getStatusCode(), "不带密钥应返回401");

        // 带错误密钥访问 - 应返回401
        webhookHeaders.set("X-Webhook-Secret", "wrong-secret");
        HttpEntity<String> wrongSecretReq = new HttpEntity<>("{}", webhookHeaders);

        ResponseEntity<Map> wrongSecretResp = restTemplate.postForEntity(
                "/webhook" + task.getWebhookPath(), wrongSecretReq, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, wrongSecretResp.getStatusCode(), "错误密钥应返回401");

        // 带正确密钥访问 - 应返回202
        webhookHeaders.set("X-Webhook-Secret", "test-secret-key-123");
        HttpEntity<String> correctSecretReq = new HttpEntity<>("{}", webhookHeaders);

        ResponseEntity<Map> correctSecretResp = restTemplate.postForEntity(
                "/webhook" + task.getWebhookPath(), correctSecretReq, Map.class);

        assertEquals(HttpStatus.ACCEPTED, correctSecretResp.getStatusCode(), "正确密钥应返回202");
    }

    @Test
    @DisplayName("Webhook流程 - 不存在的路径返回404")
    void webhookFlow_nonExistentPath_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/webhook/non-existent-path-" + System.currentTimeMillis(), request, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "不存在的Webhook路径应返回404");
    }
}
