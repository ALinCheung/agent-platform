package com.agentplatform.executor.service;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.executor.handler.FailureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.Map;

/**
 * 重试服务
 * 支持自动重试和手动重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final TaskService taskService;
    private final ExecutionHistoryService executionHistoryService;
    private final TaskExecutionQueue taskExecutionQueue;
    private final FailureHandler failureHandler;
    private final ThreadPoolTaskScheduler schedulerTaskScheduler;

    /** 正在重试的任务 */
    private final Map<Long, ScheduledFuture<?>> retrySchedules = new ConcurrentHashMap<>();

    /**
     * 自动重试
     * 如果任务配置了重试，且失败可重试，则调度重试
     */
    public void autoRetry(Task task, TaskExecution execution) {
        if (!failureHandler.shouldRetry(task, null, execution.getRetryCount())) {
            log.info("不满足重试条件: taskId={}", task.getId());
            return;
        }

        int newRetryCount = execution.getRetryCount() + 1;
        int retryInterval = task.getRetryIntervalSeconds() != null ? task.getRetryIntervalSeconds() : 60;

        log.info("调度自动重试: taskId={}, retryCount={}, intervalSeconds={}",
                task.getId(), newRetryCount, retryInterval);

        // 创建重试执行记录
        TaskExecution retryExecution = executionHistoryService.createExecution(task.getId(), execution.getTaskVersionId());

        // 调度延迟重试
        ScheduledFuture<?> future = schedulerTaskScheduler.schedule(
                () -> executeRetry(task, retryExecution),
                Instant.now().plusSeconds(retryInterval)
        );

        retrySchedules.put(task.getId(), future);
    }

    /**
     * 手动重试
     * @param executionId 原执行记录ID
     * @return 新的执行记录ID
     */
    public Long manualRetry(Long executionId) {
        TaskExecution execution = executionHistoryService.getById(executionId);
        if (execution == null) {
            throw new RuntimeException("执行记录不存在: " + executionId);
        }

        Task task = taskService.getById(execution.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在: " + execution.getTaskId());
        }

        // 创建新的执行记录
        TaskExecution newExecution = executionHistoryService.createExecution(task.getId(), execution.getTaskVersionId());

        // 提交执行
        Long newExecutionId = taskExecutionQueue.submit(task);

        log.info("手动重试: originalExecutionId={}, newExecutionId={}", executionId, newExecutionId);
        return newExecutionId;
    }

    /**
     * 取消重试
     */
    public void cancelRetry(Long taskId) {
        ScheduledFuture<?> future = retrySchedules.remove(taskId);
        if (future != null) {
            future.cancel(false);
            log.info("取消重试调度: taskId={}", taskId);
        }
    }

    private void executeRetry(Task task, TaskExecution retryExecution) {
        try {
            log.info("执行自动重试: taskId={}, retryCount={}", task.getId(), retryExecution.getRetryCount());
            taskExecutionQueue.submit(task);
        } catch (Exception e) {
            log.error("自动重试执行失败: taskId={}", task.getId(), e);
        } finally {
            retrySchedules.remove(task.getId());
        }
    }
}
