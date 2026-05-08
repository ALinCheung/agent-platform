package com.agentplatform.executor.service;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Subtask;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.LogType;
import com.agentplatform.core.enums.SubtaskStatus;
import com.agentplatform.core.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务执行队列
 * 管理任务的排队和执行，支持子任务分解和断点续执行
 */
@Slf4j
@Service
public class TaskExecutionQueue {

    private final ClaudeExecutorService claudeExecutorService;
    private final TaskService taskService;
    private final ExecutionHistoryService executionHistoryService;
    private final SubtaskService subtaskService;
    private final ExecutionLogService executionLogService;
    private final Executor executionPool;

    public TaskExecutionQueue(ClaudeExecutorService claudeExecutorService, TaskService taskService,
                              ExecutionHistoryService executionHistoryService,
                              SubtaskService subtaskService,
                              ExecutionLogService executionLogService,
                              @Qualifier("execution-pool") Executor executionPool) {
        this.claudeExecutorService = claudeExecutorService;
        this.taskService = taskService;
        this.executionHistoryService = executionHistoryService;
        this.subtaskService = subtaskService;
        this.executionLogService = executionLogService;
        this.executionPool = executionPool;
    }

    /** 正在执行的任务ID -> 执行记录ID */
    private final Map<Long, Long> runningTasks = new ConcurrentHashMap<>();

    /** 活跃执行计数 */
    private final AtomicInteger activeCount = new AtomicInteger(0);

    /**
     * 提交任务执行
     * 支持断点续执行：检查上次未完成的子任务继续执行
     */
    public Long submit(Task task) {
        // 检查任务是否正在执行
        if (runningTasks.containsKey(task.getId())) {
            log.warn("任务正在执行中，跳过: taskId={}", task.getId());
            return null;
        }

        // 检查上次执行是否有未完成的子任务（断点续执行）
        TaskExecution lastExecution = findLastExecution(task.getId());
        if (lastExecution != null && hasIncompleteSubtasks(lastExecution)) {
            // 检查 command 是否变更
            if (isCommandChanged(task, lastExecution)) {
                log.info("任务命令已变更，丢弃旧子任务重新分解: taskId={}", task.getId());
                subtaskService.skipPendingAndRunning(lastExecution.getId());
            } else {
                log.info("继续执行未完成的子任务: taskId={}, executionId={}", task.getId(), lastExecution.getId());
                executionLogService.appendLog(lastExecution.getId(), null, LogType.STEP, "调度触发：继续执行未完成的子任务");
                // 重置上次执行状态为 RUNNING
                lastExecution.setStatus(ExecutionStatus.RUNNING);
                lastExecution.setFinishedAt(null);
                executionHistoryService.updateById(lastExecution);

                runningTasks.put(task.getId(), lastExecution.getId());
                activeCount.incrementAndGet();

                executionPool.execute(() -> executeSubtaskFlow(task, lastExecution.getId()));
                return lastExecution.getId();
            }
        }

        // 创建新的执行记录
        var execution = executionHistoryService.createExecution(task.getId(), null);
        Long executionId = execution.getId();

        runningTasks.put(task.getId(), executionId);
        activeCount.incrementAndGet();

        executionPool.execute(() -> executeSubtaskFlow(task, executionId));

        log.info("任务已提交执行: taskId={}, executionId={}", task.getId(), executionId);
        return executionId;
    }

    /**
     * 子任务执行流程：分解 -> 逐步执行 -> 更新父任务状态
     */
    private void executeSubtaskFlow(Task task, Long executionId) {
        try {
            log.info("开始执行任务流程: taskId={}, executionId={}", task.getId(), executionId);

            // 检查是否有未完成的子任务（断点续执行场景）
            List<Subtask> existingSubtasks = subtaskService.getByExecutionId(executionId);
            boolean isResuming = !existingSubtasks.isEmpty();

            if (isResuming) {
                // 继续执行未完成的子任务
                log.info("断点续执行: executionId={}, 子任务数={}", executionId, existingSubtasks.size());
                executeExistingSubtasks(task, executionId, existingSubtasks);
            } else {
                // 新执行：先分解再执行
                List<Subtask> subtasks = claudeExecutorService.decomposeTask(task, executionId);

                if (subtasks == null || subtasks.isEmpty()) {
                    // 分解失败，回退为单任务模式
                    log.info("回退为单任务执行模式: taskId={}", task.getId());
                    executeSingleTaskFallback(task, executionId);
                } else {
                    // 批量创建子任务记录
                    subtaskService.batchCreate(executionId, subtasks);
                    // 重新获取带ID的子任务列表
                    List<Subtask> createdSubtasks = subtaskService.getByExecutionId(executionId);
                    executeExistingSubtasks(task, executionId, createdSubtasks);
                }
            }

            // 更新父任务状态（聚合子任务状态）
            updateParentTaskStatus(task.getId(), executionId);

        } catch (Exception e) {
            log.error("任务执行流程异常: taskId={}", task.getId(), e);
            executionLogService.appendLog(executionId, null, LogType.ERROR, "执行异常: " + e.getMessage());
            executionHistoryService.updateExecutionResult(
                    executionId, ExecutionStatus.FAILED, null, "执行异常: " + e.getMessage(),
                    null, null, null);
        } finally {
            runningTasks.remove(task.getId());
            activeCount.decrementAndGet();
        }
    }

    /**
     * 执行已有的子任务列表（用于断点续执行和新分解的子任务）
     */
    private void executeExistingSubtasks(Task task, Long executionId, List<Subtask> subtasks) {
        for (Subtask subtask : subtasks) {
            // 跳过已完成或已失败的子任务
            if (subtask.getStatus() == SubtaskStatus.COMPLETED
                    || subtask.getStatus() == SubtaskStatus.FAILED
                    || subtask.getStatus() == SubtaskStatus.SKIPPED) {
                continue;
            }

            // 更新子任务状态为 RUNNING
            subtaskService.updateStatus(subtask.getId(), SubtaskStatus.RUNNING, null, null);

            // 执行子任务
            ExecutionResult result = claudeExecutorService.executeSubtask(
                    subtask, task.getName(), subtasks.size(),
                    task.getTimeoutSeconds(), executionId);

            // 更新子任务状态
            if (result.getStatus() == ExecutionStatus.SUCCESS) {
                subtaskService.updateStatus(subtask.getId(), SubtaskStatus.COMPLETED,
                        result.getOutput(), null);
            } else {
                subtaskService.updateStatus(subtask.getId(), SubtaskStatus.FAILED,
                        result.getOutput(), result.getError());
                // 子任务失败，跳过后续子任务
                log.warn("子任务执行失败，跳过后续子任务: subtaskId={}", subtask.getId());
                executionLogService.appendLog(executionId, subtask.getId(), LogType.STEP,
                        "子任务失败，跳过后续子任务");
                skipRemainingSubtasks(executionId, subtask.getSeq());
                return;
            }
        }
    }

    /**
     * 跳过指定序号之后的所有子任务
     */
    private void skipRemainingSubtasks(Long executionId, int afterSeq) {
        List<Subtask> subtasks = subtaskService.getByExecutionId(executionId);
        for (Subtask subtask : subtasks) {
            if (subtask.getSeq() > afterSeq
                    && (subtask.getStatus() == SubtaskStatus.PENDING || subtask.getStatus() == SubtaskStatus.RUNNING)) {
                subtaskService.updateStatus(subtask.getId(), SubtaskStatus.SKIPPED, null, null);
            }
        }
    }

    /**
     * 回退模式：将原始 command 作为单个子任务执行
     */
    private void executeSingleTaskFallback(Task task, Long executionId) {
        executionLogService.appendLog(executionId, null, LogType.STEP, "回退为单任务执行模式");

        // 创建单个子任务
        Subtask singleSubtask = Subtask.builder()
                .executionId(executionId)
                .seq(1)
                .title("执行任务")
                .description(task.getCommand())
                .build();
        subtaskService.batchCreate(executionId, List.of(singleSubtask));
        Subtask subtask = subtaskService.getByExecutionId(executionId).get(0);

        // 更新为 RUNNING
        subtaskService.updateStatus(subtask.getId(), SubtaskStatus.RUNNING, null, null);

        // 执行
        ExecutionResult result = claudeExecutorService.executeSubtask(
                subtask, task.getName(), 1, task.getTimeoutSeconds(), executionId);

        if (result.getStatus() == ExecutionStatus.SUCCESS) {
            subtaskService.updateStatus(subtask.getId(), SubtaskStatus.COMPLETED, result.getOutput(), null);
        } else {
            subtaskService.updateStatus(subtask.getId(), SubtaskStatus.FAILED, result.getOutput(), result.getError());
        }
    }

    /**
     * 更新父任务状态（聚合子任务状态）
     */
    private void updateParentTaskStatus(Long taskId, Long executionId) {
        List<Subtask> subtasks = subtaskService.getByExecutionId(executionId);

        boolean allCompleted = subtasks.stream().allMatch(s -> s.getStatus() == SubtaskStatus.COMPLETED);
        boolean anyFailed = subtasks.stream().anyMatch(s -> s.getStatus() == SubtaskStatus.FAILED);
        boolean anySkipped = subtasks.stream().anyMatch(s -> s.getStatus() == SubtaskStatus.SKIPPED);

        ExecutionStatus parentStatus;
        if (allCompleted) {
            parentStatus = ExecutionStatus.SUCCESS;
        } else if (anyFailed) {
            parentStatus = ExecutionStatus.FAILED;
        } else if (anySkipped) {
            parentStatus = ExecutionStatus.TERMINATED;
        } else {
            parentStatus = ExecutionStatus.FAILED;
        }

        // 汇总输出
        StringBuilder outputBuilder = new StringBuilder();
        for (Subtask subtask : subtasks) {
            if (subtask.getOutput() != null && !subtask.getOutput().isEmpty()) {
                outputBuilder.append(String.format("[%d/%d] %s:\n%s\n\n",
                        subtask.getSeq(), subtasks.size(), subtask.getTitle(), subtask.getOutput()));
            }
        }

        String error = anyFailed ?
                subtasks.stream().filter(s -> s.getStatus() == SubtaskStatus.FAILED)
                        .map(Subtask::getError).findFirst().orElse(null) : null;

        TaskExecution execution = executionHistoryService.getById(executionId);
        if (execution != null) {
            execution.setStatus(parentStatus);
            execution.setOutput(outputBuilder.toString());
            execution.setError(error);
            execution.setFinishedAt(LocalDateTime.now());
            execution.setDurationMs(Duration.between(execution.getStartedAt(), LocalDateTime.now()).toMillis());
            executionHistoryService.updateById(execution);
        }

        executionLogService.appendLog(executionId, null, LogType.STATUS,
                "父任务状态更新为: " + parentStatus);
        log.info("父任务状态更新: taskId={}, executionId={}, status={}", taskId, executionId, parentStatus);
    }

    /**
     * 查找任务的最后一次执行记录
     */
    private TaskExecution findLastExecution(Long taskId) {
        List<TaskExecution> executions = executionHistoryService.getByTaskId(taskId);
        return executions.isEmpty() ? null : executions.get(0);
    }

    /**
     * 检查执行是否有未完成的子任务
     */
    private boolean hasIncompleteSubtasks(TaskExecution execution) {
        return subtaskService.hasIncompleteSubtasks(execution.getId());
    }

    /**
     * 检查任务命令是否与上次执行时不同
     */
    private boolean isCommandChanged(Task currentTask, TaskExecution lastExecution) {
        if (lastExecution.getTaskVersionId() != null) {
            return false;
        }
        return false;
    }

    /**
     * 终止指定执行
     */
    public boolean terminateExecution(Long executionId) {
        // 终止进程
        boolean terminated = claudeExecutorService.terminateProcess(executionId);

        // 更新执行状态
        executionHistoryService.updateExecutionResult(
                executionId, ExecutionStatus.TERMINATED, null, "用户手动终止",
                null, null, null);

        // 跳过未完成的子任务
        subtaskService.skipPendingAndRunning(executionId);

        executionLogService.appendLog(executionId, null, LogType.STATUS, "用户手动终止执行");

        // 从运行中移除
        runningTasks.entrySet().removeIf(entry -> entry.getValue().equals(executionId));
        activeCount.decrementAndGet();

        log.info("执行已终止: executionId={}", executionId);
        return terminated;
    }

    /**
     * 获取活跃执行数
     */
    public int getActiveCount() {
        return activeCount.get();
    }

    /**
     * 获取正在执行的任务ID列表
     */
    public Map<Long, Long> getRunningTasks() {
        return Map.copyOf(runningTasks);
    }

    /**
     * 检查任务是否正在执行
     */
    public boolean isRunning(Long taskId) {
        return runningTasks.containsKey(taskId);
    }
}
