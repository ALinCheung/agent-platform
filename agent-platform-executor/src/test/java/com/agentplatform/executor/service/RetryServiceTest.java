package com.agentplatform.executor.service;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.executor.handler.FailureHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RetryService 单元测试
 * 测试重试服务的自动重试、手动重试和取消重试逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("重试服务测试")
class RetryServiceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private ExecutionHistoryService executionHistoryService;

    @Mock
    private TaskExecutionQueue taskExecutionQueue;

    @Mock
    private FailureHandler failureHandler;

    @Mock
    private ThreadPoolTaskScheduler schedulerTaskScheduler;

    @InjectMocks
    private RetryService retryService;

    private Task task;
    private TaskExecution execution;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .name("测试任务")
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .build();

        execution = TaskExecution.builder()
                .id(100L)
                .taskId(1L)
                .taskVersionId(10L)
                .status(ExecutionStatus.FAILED)
                .retryCount(1)
                .startedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("autoRetry - shouldRetry返回false时不执行重试")
    void autoRetry_doesNothing_whenShouldRetryReturnsFalse() {
        // 模拟FailureHandler返回false（不满足重试条件）
        when(failureHandler.shouldRetry(eq(task), isNull(), eq(1))).thenReturn(false);

        retryService.autoRetry(task, execution);

        // 验证没有创建执行记录
        verify(executionHistoryService, never()).createExecution(anyLong(), any());
        // 验证没有调度重试
        verify(schedulerTaskScheduler, never()).schedule(any(Runnable.class), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("autoRetry - shouldRetry返回true时创建执行记录并调度重试")
    void autoRetry_createsExecutionAndSchedulesRetry_whenShouldRetryReturnsTrue() {
        // 模拟FailureHandler返回true（满足重试条件）
        when(failureHandler.shouldRetry(eq(task), isNull(), eq(1))).thenReturn(true);

        // 模拟创建新的执行记录
        TaskExecution retryExecution = TaskExecution.builder()
                .id(200L)
                .taskId(1L)
                .taskVersionId(10L)
                .retryCount(2)
                .build();
        when(executionHistoryService.createExecution(eq(1L), eq(10L))).thenReturn(retryExecution);

        // 模拟调度器返回ScheduledFuture
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(schedulerTaskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(mockFuture);

        retryService.autoRetry(task, execution);

        // 验证创建了执行记录
        verify(executionHistoryService).createExecution(1L, 10L);
        // 验证调度了重试
        verify(schedulerTaskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("manualRetry - 执行记录不存在时抛出异常")
    void manualRetry_throwsForNonExistentExecution() {
        Long nonExistentExecutionId = 999L;
        when(executionHistoryService.getById(eq(nonExistentExecutionId))).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> retryService.manualRetry(nonExistentExecutionId),
                "手动重试不存在的执行记录应抛出异常");

        assertTrue(exception.getMessage().contains("执行记录不存在"),
                "异常消息应包含'执行记录不存在'");
    }

    @Test
    @DisplayName("manualRetry - 任务不存在时抛出异常")
    void manualRetry_throwsForNonExistentTask() {
        Long executionId = 100L;
        when(executionHistoryService.getById(eq(executionId))).thenReturn(execution);
        when(taskService.getById(eq(1L))).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> retryService.manualRetry(executionId),
                "手动重试不存在的任务应抛出异常");

        assertTrue(exception.getMessage().contains("任务不存在"),
                "异常消息应包含'任务不存在'");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("cancelRetry - 取消已调度的重试")
    void cancelRetry_cancelsScheduledFuture() {
        // 先通过autoRetry设置一个调度
        when(failureHandler.shouldRetry(eq(task), isNull(), eq(1))).thenReturn(true);

        TaskExecution retryExecution = TaskExecution.builder()
                .id(200L)
                .taskId(1L)
                .taskVersionId(10L)
                .retryCount(2)
                .build();
        when(executionHistoryService.createExecution(eq(1L), eq(10L))).thenReturn(retryExecution);

        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(schedulerTaskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(mockFuture);

        retryService.autoRetry(task, execution);

        // 取消重试
        retryService.cancelRetry(1L);

        // 验证调用了future.cancel
        verify(mockFuture).cancel(false);
    }
}
