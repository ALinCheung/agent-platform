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
 * 任务管理页面端到端测试
 * 测试任务列表、创建、编辑操作的API接口
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("任务管理页面测试")
class TaskManagementPageTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("任务列表 - 页面可访问")
    void taskListPage_isAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/tasks", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "任务列表页面应返回200");
    }

    @Test
    @DisplayName("任务列表 - API返回任务列表")
    void taskListApi_returnsTaskList() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tasks", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "任务列表API应返回200");
    }

    @Test
    @DisplayName("创建任务 - 通过API创建任务")
    void createTask_viaApi_returns201() throws Exception {
        Task task = Task.builder()
                .name("pw-test-create-" + System.currentTimeMillis())
                .description("页面测试任务")
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
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

        ResponseEntity<Task> response = restTemplate.postForEntity("/api/tasks", request, Task.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "创建任务应返回201");
        assertNotNull(response.getBody().getId(), "创建后的任务应有ID");
    }

    @Test
    @DisplayName("编辑任务 - 通过API更新任务")
    void updateTask_viaApi_returns200() throws Exception {
        // 先创建任务
        Task task = Task.builder()
                .name("pw-test-update-" + System.currentTimeMillis())
                .description("待更新任务")
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
        task.setDescription("已更新的任务");
        HttpEntity<String> updateReq = new HttpEntity<>(objectMapper.writeValueAsString(task), headers);

        ResponseEntity<Task> updateResp = restTemplate.exchange(
                "/api/tasks/" + taskId, HttpMethod.PUT, updateReq, Task.class);

        assertEquals(HttpStatus.OK, updateResp.getStatusCode(), "更新任务应返回200");
        assertEquals("echo updated", updateResp.getBody().getCommand(), "命令应已更新");
    }

    @Test
    @DisplayName("删除任务 - 通过API删除任务")
    void deleteTask_viaApi_returns204() throws Exception {
        // 先创建任务
        Task task = Task.builder()
                .name("pw-test-delete-" + System.currentTimeMillis())
                .command("echo delete")
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

        // 删除任务
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/tasks/" + taskId, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode(), "删除任务应返回204");
    }
}
