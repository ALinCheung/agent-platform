package com.agentplatform.web.controller;

import com.agentplatform.core.entity.ExecutionLog;
import com.agentplatform.core.entity.Subtask;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.LogType;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.ExecutionLogService;
import com.agentplatform.core.service.SubtaskService;
import com.agentplatform.executor.service.RetryService;
import com.agentplatform.executor.service.TaskExecutionQueue;
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
    private final SubtaskService subtaskService;
    private final ExecutionLogService executionLogService;
    private final TaskExecutionQueue taskExecutionQueue;

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

    /**
     * 终止正在执行的任务
     */
    @PostMapping("/{id}/terminate")
    public ResponseEntity<?> terminateExecution(@PathVariable Long id) {
        boolean terminated = taskExecutionQueue.terminateExecution(id);
        if (terminated) {
            return ResponseEntity.ok(Map.of("message", "执行已终止"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "无法终止执行，进程可能已结束"));
        }
    }

    /**
     * 查询执行的子任务列表
     */
    @GetMapping("/{executionId}/subtasks")
    public ResponseEntity<List<Subtask>> getSubtasks(@PathVariable Long executionId) {
        return ResponseEntity.ok(subtaskService.getByExecutionId(executionId));
    }

    /**
     * 查询子任务统计
     */
    @GetMapping("/{executionId}/subtasks/stats")
    public ResponseEntity<Map<String, Object>> getSubtaskStats(@PathVariable Long executionId) {
        return ResponseEntity.ok(subtaskService.getStatsByExecutionId(executionId));
    }

    /**
     * 查询子任务详情
     */
    @GetMapping("/subtasks/{subtaskId}")
    public ResponseEntity<Subtask> getSubtask(@PathVariable Long subtaskId) {
        Subtask subtask = subtaskService.getById(subtaskId);
        if (subtask == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(subtask);
    }

    /**
     * 查询执行日志
     */
    @GetMapping("/{executionId}/logs")
    public ResponseEntity<List<ExecutionLog>> getExecutionLogs(
            @PathVariable Long executionId,
            @RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            LogType logType = LogType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(executionLogService.getByExecutionIdAndType(executionId, logType));
        }
        return ResponseEntity.ok(executionLogService.getByExecutionId(executionId));
    }

    /**
     * 查询子任务日志
     */
    @GetMapping("/subtasks/{subtaskId}/logs")
    public ResponseEntity<List<ExecutionLog>> getSubtaskLogs(@PathVariable Long subtaskId) {
        return ResponseEntity.ok(executionLogService.getBySubtaskId(subtaskId));
    }
}
