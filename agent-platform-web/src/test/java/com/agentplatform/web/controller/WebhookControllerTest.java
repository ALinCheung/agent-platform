package com.agentplatform.web.controller;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.executor.service.TaskExecutionQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Webhook控制器单元测试
 * 测试Webhook接收、密钥验证、参数注入等功能
 */
@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private TaskExecutionQueue taskExecutionQueue;

    /**
     * 测试不存在的Webhook路径返回404
     */
    @Test
    @DisplayName("Webhook路径不存在时返回404")
    void handleWebhook_returns404_forNonExistentPath() throws Exception {
        when(taskService.findByWebhookPath("nonexistent")).thenReturn(null);

        mockMvc.perform(post("/webhook/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Webhook路径不存在: nonexistent"));

        verify(taskService).findByWebhookPath("nonexistent");
        verifyNoInteractions(taskExecutionQueue);
    }

    /**
     * 测试密钥不匹配时返回401
     */
    @Test
    @DisplayName("Webhook密钥不匹配时返回401")
    void handleWebhook_returns401_whenSecretMismatch() throws Exception {
        Task task = Task.builder()
                .id(1L).name("test-task").command("echo test")
                .triggerType(TriggerType.WEBHOOK).webhookPath("myhook")
                .webhookSecret("correct-secret")
                .timeoutSeconds(300).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.findByWebhookPath("myhook")).thenReturn(task);

        mockMvc.perform(post("/webhook/myhook")
                        .header("X-Webhook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("密钥验证失败"));

        verify(taskService).findByWebhookPath("myhook");
        verifyNoInteractions(taskExecutionQueue);
    }

    /**
     * 测试有效请求返回202
     */
    @Test
    @DisplayName("有效Webhook请求返回202并提交执行")
    void handleWebhook_returns202_forValidRequest() throws Exception {
        Task task = Task.builder()
                .id(1L).name("test-task").command("echo test")
                .triggerType(TriggerType.WEBHOOK).webhookPath("myhook")
                .webhookSecret("my-secret")
                .timeoutSeconds(300).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.findByWebhookPath("myhook")).thenReturn(task);
        when(taskExecutionQueue.submit(any(Task.class))).thenReturn(100L);

        mockMvc.perform(post("/webhook/myhook")
                        .header("X-Webhook-Secret", "my-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("任务已提交执行"))
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.executionId").value(100));

        verify(taskService).findByWebhookPath("myhook");
        verify(taskExecutionQueue).submit(any(Task.class));
    }

    /**
     * 测试Webhook注入prompt参数
     */
    @Test
    @DisplayName("Webhook请求体中的prompt被注入到任务命令")
    void handleWebhook_injectsPromptFromBody() throws Exception {
        Task task = Task.builder()
                .id(1L).name("test-task").command("echo default")
                .triggerType(TriggerType.WEBHOOK).webhookPath("myhook")
                .timeoutSeconds(300).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.findByWebhookPath("myhook")).thenReturn(task);
        when(taskExecutionQueue.submit(any(Task.class))).thenReturn(101L);

        String body = objectMapper.writeValueAsString(Map.of("prompt", "custom prompt text"));

        mockMvc.perform(post("/webhook/myhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").value(101));

        // 验证提交的任务使用了注入的prompt而非原始命令
        verify(taskExecutionQueue).submit(argThat(t ->
                "custom prompt text".equals(t.getCommand())));
    }

    /**
     * 测试任务没有设置密钥时可以直接访问
     */
    @Test
    @DisplayName("任务无密钥时Webhook无需验证即可访问")
    void handleWebhook_worksWithoutSecret_whenTaskHasNoSecret() throws Exception {
        Task task = Task.builder()
                .id(1L).name("test-task").command("echo test")
                .triggerType(TriggerType.WEBHOOK).webhookPath("openhook")
                .webhookSecret(null)
                .timeoutSeconds(300).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.findByWebhookPath("openhook")).thenReturn(task);
        when(taskExecutionQueue.submit(any(Task.class))).thenReturn(102L);

        mockMvc.perform(post("/webhook/openhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("任务已提交执行"))
                .andExpect(jsonPath("$.executionId").value(102));

        verify(taskExecutionQueue).submit(any(Task.class));
    }
}
