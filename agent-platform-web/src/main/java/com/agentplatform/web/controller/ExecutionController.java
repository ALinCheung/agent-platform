package com.agentplatform.web.controller;

import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.executor.service.RetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 执行记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionHistoryService executionHistoryService;
    private final RetryService retryService;

    /**
     * 获取执行历史
     */
    @GetMapping
    public ResponseEntity<List<TaskExecution>> listExecutions(
            @RequestParam(required = false) Long taskId) {
        if (taskId != null) {
            return ResponseEntity.ok(executionHistoryService.getByTaskId(taskId));
        }
        return ResponseEntity.ok(executionHistoryService.list());
    }

    /**
     * 获取执行详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskExecution> getExecution(@PathVariable Long id) {
        TaskExecution execution = executionHistoryService.getById(id);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(execution);
    }

    /**
     * 重试失败的执行
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryExecution(@PathVariable Long id) {
        try {
            Long newExecutionId = retryService.manualRetry(id);
            return ResponseEntity.accepted()
                    .body(Map.of("message", "重试已提交", "executionId", newExecutionId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
