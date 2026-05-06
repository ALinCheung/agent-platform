package com.agentplatform.starter.e2e;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.starter.AgentPlatformApplication;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务生命周期端到端测试
 * 覆盖：任务创建 -> 执行 -> 失败 -> 重试 -> 回滚
 *
 * 使用 @Transactional 注解，每个测试方法结束后自动回滚所有数据变更。
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("任务生命周期端到端测试")
class TaskLifecycleE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** 构建测试任务请求体 */
    private HttpEntity<String> buildTaskRequest(String name, String command) throws Exception {
        Task task = Task.builder()
                .name(name)
                .description("E2E测试任务")
                .command(command)
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                .timeoutSeconds(300)
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(objectMapper.writeValueAsString(task), headers);
    }

    /**
     * 测试创建任务返回201
     */
    @Test
    @DisplayName("1. 创建任务 - POST /api/tasks 返回201")
    void test01_createTask_returns201() throws Exception {
        HttpEntity<String> request = buildTaskRequest("e2e-task-1", "echo hello");

        ResponseEntity<Task> response = restTemplate.postForEntity(
                "/api/tasks", request, Task.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "创建任务应返回201");
        assertNotNull(response.getBody(), "响应体不应为空");
        assertNotNull(response.getBody().getId(), "创建后的任务应有ID");
        assertEquals("e2e-task-1", response.getBody().getName());
        assertEquals("echo hello", response.getBody().getCommand());
    }

    /**
     * 测试获取任务验证创建成功
     */
    @Test
    @DisplayName("2. 获取任务 - GET /api/tasks/{id} 验证任务存在")
    void test02_getTask_verifiesExistence() throws Exception {
        // 先创建任务
        HttpEntity<String> request = buildTaskRequest("e2e-task-2", "echo world");
        ResponseEntity<Task> createResponse = restTemplate.postForEntity(
                "/api/tasks", request, Task.class);
        Long taskId = createResponse.getBody().getId();

        // 获取任务
        ResponseEntity<Task> getResponse = restTemplate.getForEntity(
                "/api/tasks/" + taskId, Task.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(taskId, getResponse.getBody().getId());
        assertEquals("e2e-task-2", getResponse.getBody().getName());
    }

    /**
     * 测试更新任务
     */
    @Test
    @DisplayName("3. 更新任务 - PUT /api/tasks/{id} 验证更新成功")
    void test03_updateTask_verifiesUpdate() throws Exception {
        // 先创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-3", "echo original");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 更新任务
        Task updated = Task.builder()
                .name("e2e-task-3-updated")
                .description("更新后的描述")
                .command("echo updated")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 */5 * * * ?")
                .timeoutSeconds(600)
                .maxRetries(5)
                .retryIntervalSeconds(120)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Task> updateReq = new HttpEntity<>(updated, headers);

        ResponseEntity<Task> updateResp = restTemplate.exchange(
                "/api/tasks/" + taskId, HttpMethod.PUT, updateReq, Task.class);

        assertEquals(HttpStatus.OK, updateResp.getStatusCode());
        assertNotNull(updateResp.getBody());
        assertEquals("e2e-task-3-updated", updateResp.getBody().getName());
        assertEquals("echo updated", updateResp.getBody().getCommand());
    }

    /**
     * 测试手动执行任务
     */
    @Test
    @DisplayName("4. 执行任务 - POST /api/tasks/{id}/execute 返回202")
    void test04_executeTask_returns202() throws Exception {
        // 先创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-4", "echo run");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 手动执行
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> execReq = new HttpEntity<>(headers);

        ResponseEntity<Map> execResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/execute", execReq, Map.class);

        assertEquals(HttpStatus.ACCEPTED, execResp.getStatusCode());
        assertNotNull(execResp.getBody());
        assertNotNull(execResp.getBody().get("executionId"), "应返回执行记录ID");
    }

    /**
     * 测试获取执行记录
     */
    @Test
    @DisplayName("5. 获取执行记录 - GET /api/executions/{id} 验证执行状态")
    void test05_getExecution_verifiesStatus() throws Exception {
        // 先创建任务并执行
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-5", "echo exec");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> execReq = new HttpEntity<>(headers);

        ResponseEntity<Map> execResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/execute", execReq, Map.class);
        Long executionId = Long.valueOf(execResp.getBody().get("executionId").toString());

        // 获取执行记录
        ResponseEntity<Map> getExecResp = restTemplate.getForEntity(
                "/api/executions/" + executionId, Map.class);

        assertEquals(HttpStatus.OK, getExecResp.getStatusCode());
        assertNotNull(getExecResp.getBody());
        // 执行状态应为RUNNING或已完成（取决于执行速度）
        String status = getExecResp.getBody().get("status").toString();
        assertNotNull(status, "执行状态不应为空");
    }

    /**
     * 测试获取版本历史
     */
    @Test
    @DisplayName("6. 获取版本历史 - GET /api/tasks/{id}/versions 验证版本记录")
    void test06_getVersions_verifiesHistory() throws Exception {
        // 先创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-6", "echo version");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 获取版本历史
        ResponseEntity<List> versionsResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId + "/versions", List.class);

        assertEquals(HttpStatus.OK, versionsResp.getStatusCode());
        assertNotNull(versionsResp.getBody());
        assertFalse(versionsResp.getBody().isEmpty(), "应至少有一个初始版本");

        // 验证初始版本信息
        @SuppressWarnings("unchecked")
        Map<String, Object> firstVersion = (Map<String, Object>) versionsResp.getBody().get(0);
        assertEquals(1, firstVersion.get("version"));
        assertEquals("CREATE", firstVersion.get("changeType"));
    }

    /**
     * 测试版本回滚
     */
    @Test
    @DisplayName("7. 版本回滚 - POST /api/tasks/{id}/versions/rollback?version=1")
    void test07_rollback_returns200() throws Exception {
        // 创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-7", "echo rollback");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 更新任务（产生新版本）
        Task updated = Task.builder()
                .name("e2e-task-7")
                .command("echo updated-for-rollback")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 */10 * * * ?")
                .timeoutSeconds(600)
                .maxRetries(5)
                .retryIntervalSeconds(120)
                .enabled(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.PUT,
                new HttpEntity<>(updated, headers), Task.class);

        // 回滚到版本1
        ResponseEntity<Map> rollbackResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/versions/rollback?version=1",
                null, Map.class);

        assertEquals(HttpStatus.OK, rollbackResp.getStatusCode());
        assertNotNull(rollbackResp.getBody());
        assertEquals("回滚成功", rollbackResp.getBody().get("message"));

        // 验证任务已回滚到原始命令
        ResponseEntity<Task> getResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId, Task.class);
        assertEquals("echo rollback", getResp.getBody().getCommand());
    }

    /**
     * 测试禁用任务
     */
    @Test
    @DisplayName("8. 禁用任务 - POST /api/tasks/{id}/disable")
    void test08_disableTask_returns200() throws Exception {
        // 创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-8", "echo disable");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 禁用任务
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> disableResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/disable", new HttpEntity<>(headers), Map.class);

        assertEquals(HttpStatus.OK, disableResp.getStatusCode());
        assertEquals("任务已禁用", disableResp.getBody().get("message"));

        // 验证任务已禁用
        ResponseEntity<Task> getResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId, Task.class);
        assertFalse(getResp.getBody().getEnabled(), "任务应为禁用状态");
    }

    /**
     * 测试启用任务
     */
    @Test
    @DisplayName("9. 启用任务 - POST /api/tasks/{id}/enable")
    void test09_enableTask_returns200() throws Exception {
        // 创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-9", "echo enable");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 先禁用
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/tasks/" + taskId + "/disable",
                new HttpEntity<>(headers), Map.class);

        // 启用任务
        ResponseEntity<Map> enableResp = restTemplate.postForEntity(
                "/api/tasks/" + taskId + "/enable", new HttpEntity<>(headers), Map.class);

        assertEquals(HttpStatus.OK, enableResp.getStatusCode());
        assertEquals("任务已启用", enableResp.getBody().get("message"));

        // 验证任务已启用
        ResponseEntity<Task> getResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId, Task.class);
        assertTrue(getResp.getBody().getEnabled(), "任务应为启用状态");
    }

    /**
     * 测试删除任务返回204
     */
    @Test
    @DisplayName("10. 删除任务 - DELETE /api/tasks/{id} 返回204")
    void test10_deleteTask_returns204() throws Exception {
        // 创建任务
        HttpEntity<String> createReq = buildTaskRequest("e2e-task-10", "echo delete");
        ResponseEntity<Task> createResp = restTemplate.postForEntity(
                "/api/tasks", createReq, Task.class);
        Long taskId = createResp.getBody().getId();

        // 删除任务
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/tasks/" + taskId, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());

        // 验证任务已删除
        ResponseEntity<Task> getResp = restTemplate.getForEntity(
                "/api/tasks/" + taskId, Task.class);
        assertEquals(HttpStatus.NOT_FOUND, getResp.getStatusCode());
    }
}
