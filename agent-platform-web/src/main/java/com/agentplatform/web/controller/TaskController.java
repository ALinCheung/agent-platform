package com.agentplatform.web.controller;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.ValidationResult;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskValidator;
import com.agentplatform.executor.service.TaskExecutionQueue;
import com.agentplatform.scheduler.service.SchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskValidator taskValidator;
    private final TaskExecutionQueue taskExecutionQueue;
    private final SchedulerService schedulerService;

    /**
     * 获取所有任务
     */
    @GetMapping
    public ResponseEntity<List<Task>> listTasks() {
        return ResponseEntity.ok(taskService.listAll());
    }

    /**
     * 获取单个任务
     */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    /**
     * 创建任务
     */
    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task) {
        ValidationResult validation = taskValidator.validate(task);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(validation);
        }

        Task created = taskService.createTask(task);

        // 如果是Cron任务，注册到调度器
        if (created.getTriggerType() == com.agentplatform.core.enums.TriggerType.CRON
                && created.getEnabled()) {
            schedulerService.registerTask(created);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        Task existing = taskService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        task.setId(id);
        ValidationResult validation = taskValidator.validate(task);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(validation);
        }

        Task updated = taskService.updateTask(id, task);

        // 重新注册调度
        schedulerService.reRegisterTask(updated);

        return ResponseEntity.ok(updated);
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        // 取消调度
        schedulerService.cancelTask(id);

        // 删除任务（级联删除关联数据）
        taskService.removeById(id);

        log.info("删除任务: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用任务
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<?> enableTask(@PathVariable Long id) {
        taskService.enableTask(id);
        Task task = taskService.getById(id);
        schedulerService.reRegisterTask(task);
        return ResponseEntity.ok(Map.of("message", "任务已启用"));
    }

    /**
     * 禁用任务
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<?> disableTask(@PathVariable Long id) {
        taskService.disableTask(id);
        schedulerService.cancelTask(id);
        return ResponseEntity.ok(Map.of("message", "任务已禁用"));
    }

    /**
     * 手动执行任务
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeTask(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        Long executionId = taskExecutionQueue.submit(task);
        return ResponseEntity.accepted()
                .body(Map.of("message", "任务已提交执行", "executionId", executionId));
    }

    /**
     * 验证任务配置
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<ValidationResult> validateTask(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        ValidationResult result = taskValidator.validate(task);
        return ResponseEntity.ok(result);
    }
}
