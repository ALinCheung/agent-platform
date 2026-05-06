package com.agentplatform.executor.service;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.TaskExecutor;
import com.agentplatform.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TaskExecutionQueue 单元测试
 * 测试任务提交、队列监控、并发控制
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务执行队列测试")
class TaskExecutionQueueTest {

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private TaskService taskService;

    @Mock
    private ExecutionHistoryService executionHistoryService;

    @Mock
    private Executor executionPool;

    @InjectMocks
    private TaskExecutionQueue taskExecutionQueue;

    private Task task;
    private TaskExecution execution;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .name("测试任务")
                .command("echo hello")
                .triggerType(TriggerType.CRON)
                .timeoutSeconds(300)
                .build();

        execution = new TaskExecution();
        execution.setId(100L);
        execution.setTaskId(1L);
        execution.setStatus(ExecutionStatus.RUNNING);
    }

    @Test
    @DisplayName("submit - 成功提交任务返回执行ID")
    void submit_returnsExecutionId_whenSuccess() {
        when(executionHistoryService.createExecution(1L, null)).thenReturn(execution);
        // 模拟线程池直接执行
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executionPool).execute(any(Runnable.class));

        when(taskExecutor.execute(task)).thenReturn(ExecutionResult.builder()
                .status(ExecutionStatus.SUCCESS)
                .output("done")
                .build());

        Long executionId = taskExecutionQueue.submit(task);

        assertNotNull(executionId, "应返回执行ID");
        assertEquals(100L, executionId);
        verify(executionHistoryService).createExecution(1L, null);
        verify(taskExecutor).execute(task);
    }

    @Test
    @DisplayName("submit - 任务正在执行时返回null")
    void submit_returnsNull_whenTaskAlreadyRunning() {
        when(executionHistoryService.createExecution(1L, null)).thenReturn(execution);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            // 不执行runnable，模拟异步
            return null;
        }).when(executionPool).execute(any(Runnable.class));

        // 第一次提交
        taskExecutionQueue.submit(task);

        // 第二次提交同一任务
        Long result = taskExecutionQueue.submit(task);

        assertNull(result, "重复提交应返回null");
    }

    @Test
    @DisplayName("getActiveCount - 初始状态为0")
    void getActiveCount_returnsZero_initially() {
        assertEquals(0, taskExecutionQueue.getActiveCount(), "初始活跃数应为0");
    }

    @Test
    @DisplayName("getActiveCount - 提交任务后计数增加")
    void getActiveCount_increases_afterSubmit() {
        when(executionHistoryService.createExecution(1L, null)).thenReturn(execution);
        doAnswer(invocation -> {
            // 不执行runnable，保持活跃状态
            return null;
        }).when(executionPool).execute(any(Runnable.class));

        taskExecutionQueue.submit(task);

        assertEquals(1, taskExecutionQueue.getActiveCount(), "提交后活跃数应为1");
    }

    @Test
    @DisplayName("isRunning - 未提交的任务返回false")
    void isRunning_returnsFalse_forUnsubmittedTask() {
        assertFalse(taskExecutionQueue.isRunning(999L), "未提交的任务不应在运行中");
    }

    @Test
    @DisplayName("isRunning - 已提交的任务返回true")
    void isRunning_returnsTrue_forSubmittedTask() {
        when(executionHistoryService.createExecution(1L, null)).thenReturn(execution);
        doAnswer(invocation -> {
            // 不执行runnable，保持运行状态
            return null;
        }).when(executionPool).execute(any(Runnable.class));

        taskExecutionQueue.submit(task);

        assertTrue(taskExecutionQueue.isRunning(1L), "已提交的任务应在运行中");
    }

    @Test
    @DisplayName("getRunningTasks - 返回正在运行的任务映射")
    void getRunningTasks_returnsRunningTaskMap() {
        when(executionHistoryService.createExecution(1L, null)).thenReturn(execution);
        doAnswer(invocation -> {
            return null;
        }).when(executionPool).execute(any(Runnable.class));

        taskExecutionQueue.submit(task);

        Map<Long, Long> runningTasks = taskExecutionQueue.getRunningTasks();

        assertEquals(1, runningTasks.size(), "应有1个运行中的任务");
        assertEquals(100L, runningTasks.get(1L), "任务1的执行ID应为100");
    }

    @Test
    @DisplayName("getRunningTasks - 返回不可修改的副本")
    void getRunningTasks_returnsUnmodifiableCopy() {
        Map<Long, Long> runningTasks = taskExecutionQueue.getRunningTasks();

        assertThrows(UnsupportedOperationException.class, () -> runningTasks.put(99L, 999L),
                "返回的映射应不可修改");
    }

    @Test
    @DisplayName("submit - 执行异常时记录失败状态")
    void submit_recordsFailure_whenExecutionThrows() {
        when(executionHistoryService.createExecution(1L, null)).thenReturn(execution);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executionPool).execute(any(Runnable.class));

        when(taskExecutor.execute(task)).thenThrow(new RuntimeException("执行异常"));

        taskExecutionQueue.submit(task);

        verify(executionHistoryService).updateExecutionResult(
                eq(100L),
                eq(ExecutionStatus.FAILED),
                isNull(),
                contains("执行异常"),
                isNull(),
                isNull(),
                isNull()
        );
    }
}
