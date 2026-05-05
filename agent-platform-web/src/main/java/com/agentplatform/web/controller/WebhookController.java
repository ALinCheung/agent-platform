package com.agentplatform.web.controller;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.executor.service.TaskExecutionQueue;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.Map;

/**
 * Webhook控制器
 * 接收外部事件触发任务执行
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final TaskService taskService;
    private final TaskExecutionQueue taskExecutionQueue;

    /**
     * 接收Webhook事件
     * POST /webhook/{path}
     */
    @PostMapping("/{path}")
    public ResponseEntity<?> handleWebhook(
            @PathVariable String path,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        log.info("收到Webhook请求: path={}", path);

        // 查找匹配的任务
        Task task = taskService.findByWebhookPath(path);
        if (task == null) {
            log.warn("Webhook路径不存在: {}", path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Webhook路径不存在: " + path));
        }

        // 验证密钥
        if (task.getWebhookSecret() != null && !task.getWebhookSecret().isBlank()) {
            if (secret == null || !secret.equals(task.getWebhookSecret())) {
                log.warn("Webhook密钥验证失败: taskId={}", task.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "密钥验证失败"));
            }
        }

        // 构建命令（注入参数）
        String command = task.getCommand();
        if (body != null && body.containsKey("prompt")) {
            command = String.valueOf(body.get("prompt"));
            log.info("注入Webhook参数: taskId={}, prompt={}", task.getId(), command);
        }

        // 创建临时任务副本执行
        Task execTask = Task.builder()
                .id(task.getId())
                .name(task.getName())
                .command(command)
                .timeoutSeconds(task.getTimeoutSeconds())
                .build();

        // 提交执行
        Long executionId = taskExecutionQueue.submit(execTask);

        log.info("Webhook任务已提交: taskId={}, executionId={}", task.getId(), executionId);
        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "任务已提交执行",
                        "taskId", task.getId(),
                        "executionId", executionId
                ));
    }
}
