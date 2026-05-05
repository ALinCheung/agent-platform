package com.agentplatform.web.controller;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskVersionService;
import com.agentplatform.core.service.impl.TaskRollbackServiceImpl;
import com.agentplatform.scheduler.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务版本控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks/{taskId}/versions")
@RequiredArgsConstructor
public class TaskVersionController {

    private final TaskVersionService taskVersionService;
    private final TaskRollbackServiceImpl taskRollbackService;
    private final SchedulerService schedulerService;

    /**
     * 获取任务版本历史
     */
    @GetMapping
    public ResponseEntity<List<TaskVersion>> getVersions(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskVersionService.getVersions(taskId));
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback")
    public ResponseEntity<?> rollback(
            @PathVariable Long taskId,
            @RequestParam int version) {
        try {
            Task rolledBack = taskRollbackService.rollback(taskId, version);

            // 重新注册调度
            schedulerService.reRegisterTask(rolledBack);

            return ResponseEntity.ok(Map.of(
                    "message", "回滚成功",
                    "taskId", taskId,
                    "version", version
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
