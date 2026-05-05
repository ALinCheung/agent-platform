package com.agentplatform.executor.service;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.TaskExecutor;
import com.agentplatform.core.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务执行队列
 * 管理任务的排队和执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionQueue {

    private final TaskExecutor taskExecutor;
    private final TaskService taskService;
    private final ExecutionHistoryService executionHistoryService;

    @Qualifier("execution-pool")
    private final Executor executionPool;

    /** 正在执行的任务ID -> 执行记录ID */
    private final Map<Long, Long> runningTasks = new ConcurrentHashMap<>();

    /** 活跃执行计数 */
    private final AtomicInteger activeCount = new AtomicInteger(0);

    /**
     * 提交任务执行
     * @param task 要执行的任务
     * @return 执行记录ID
     */
    public Long submit(Task task) {
        // 检查任务是否正在执行
        if (runningTasks.containsKey(task.getId())) {
            log.warn("任务正在执行中，跳过: taskId={}", task.getId());
            return null;
        }

        // 创建执行记录
        var execution = executionHistoryService.createExecution(task.getId(), null);
        Long executionId = execution.getId();

        // 标记为正在执行
        runningTasks.put(task.getId(), executionId);
        activeCount.incrementAndGet();

        // 提交到线程池
        executionPool.execute(() -> {
            try {
                log.info("开始执行任务: taskId={}, executionId={}", task.getId(), executionId);
                ExecutionResult result = taskExecutor.execute(task);

                // 更新执行结果
                executionHistoryService.updateExecutionResult(
                        executionId,
                        result.getStatus(),
                        result.getOutput(),
                        result.getError(),
                        result.getExitCode(),
                        result.getDurationMs(),
                        result.getMemoryUsedMb()
                );

                log.info("任务执行完成: taskId={}, status={}", task.getId(), result.getStatus());
            } catch (Exception e) {
                log.error("任务执行异常: taskId={}", task.getId(), e);
                executionHistoryService.updateExecutionResult(
                        executionId,
                        ExecutionStatus.FAILED,
                        null,
                        "执行异常: " + e.getMessage(),
                        null,
                        null,
                        null
                );
            } finally {
                runningTasks.remove(task.getId());
                activeCount.decrementAndGet();
            }
        });

        log.info("任务已提交执行: taskId={}, executionId={}", task.getId(), executionId);
        return executionId;
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
