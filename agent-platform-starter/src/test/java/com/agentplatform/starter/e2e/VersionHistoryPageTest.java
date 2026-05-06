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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 版本历史页面端到端测试
 * 测试版本列表、版本对比、回滚操作
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("版本历史页面测试")
class VersionHistoryPageTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("版本历史 - 获取任务版本列表")
    void versionList_returnsVersions() throws Exception {
        // 创建任务
        Task task = Task.builder()
                .name("pw-test-versions-" + System.currentTimeMillis())
                .command("echo version-test")
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

        // 获取版本列表
        ResponseEntity<List> versionsResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId + "/versions", List.class);

        assertEquals(HttpStatus.OK, versionsResp.getStatusCode(), "获取版本列表应返回200");
        assertNotNull(versionsResp.getBody(), "版本列表不应为空");
        assertFalse(versionsResp.getBody().isEmpty(), "应至少有一个初始版本");
    }

    @Test
    @DisplayName("版本历史 - 更新任务产生新版本")
    void updateTask_createsNewVersion() throws Exception {
        // 创建任务
        Task task = Task.builder()
                .name("pw-test-ver-update-" + System.currentTimeMillis())
                .command("echo original")
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

        // 更新任务
        task.setCommand("echo updated");
        HttpEntity<String> updateReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);
        restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.PUT, updateReq, Task.class);

        // 获取版本列表
        ResponseEntity<List> versionsResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId + "/versions", List.class);

        assertEquals(2, versionsResp.getBody().size(), "更新后应有2个版本");
    }

    @Test
    @DisplayName("版本回滚 - 回滚到指定版本")
    void rollbackToVersion_returns200() throws Exception {
        // 创建任务
        Task task = Task.builder()
                .name("pw-test-rollback-" + System.currentTimeMillis())
                .command("echo original")
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

        // 更新任务
        task.setCommand("echo updated-for-rollback");
        HttpEntity<String> updateReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);
        restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.PUT, updateReq, Task.class);

        // 回滚到版本1
        ResponseEntity<Map> rollbackResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/versions/rollback?version=1", null, Map.class);

        assertEquals(HttpStatus.OK, rollbackResp.getStatusCode(), "回滚应返回200");
        assertEquals("回滚成功", rollbackResp.getBody().get("message"), "应返回回滚成功消息");

        // 验证任务已回滚
        ResponseEntity<Task> getResp = restTemplate.getForEntity("/api/tasks/" + taskId, Task.class);
        assertEquals("echo original", getResp.getBody().getCommand(), "任务命令应已回滚");
    }

    @Test
    @DisplayName("版本回滚 - 回滚不存在的版本返回400")
    void rollbackToNonExistentVersion_returns400() throws Exception {
        // 创建任务
        Task task = Task.builder()
                .name("pw-test-rollback-fail-" + System.currentTimeMillis())
                .command("echo test")
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

        // 尝试回滚到不存在的版本
        ResponseEntity<Map> rollbackResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/versions/rollback?version=999", null, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, rollbackResp.getStatusCode(), "回滚不存在的版本应返回400");
    }
}
