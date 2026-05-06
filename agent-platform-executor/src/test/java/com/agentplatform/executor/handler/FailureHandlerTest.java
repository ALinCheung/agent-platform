package com.agentplatform.executor.handler;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FailureHandler 单元测试
 * 测试失败处理器的重试判断和错误分类逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("失败处理器测试")
class FailureHandlerTest {

    @Mock
    private ExecutionHistoryService executionHistoryService;

    @InjectMocks
    private FailureHandler failureHandler;

    private Task task;
    private ExecutionResult failedResult;
    private ExecutionResult timeoutResult;
    private ExecutionResult successResult;

    @BeforeEach
    void setUp() {
        // 创建基础测试任务，设置最大重试次数为3
        task = Task.builder()
                .id(1L)
                .name("测试任务")
                .maxRetries(3)
                .build();

        // 创建失败的执行结果
        failedResult = ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .error("执行失败")
                .build();

        // 创建超时的执行结果
        timeoutResult = ExecutionResult.builder()
                .status(ExecutionStatus.TIMEOUT)
                .error("执行超时")
                .build();

        // 创建成功的执行结果
        successResult = ExecutionResult.builder()
                .status(ExecutionStatus.SUCCESS)
                .output("执行成功")
                .build();
    }

    @Test
    @DisplayName("shouldRetry - maxRetries为0时返回false")
    void shouldRetry_returnsFalse_whenMaxRetriesIsZero() {
        task.setMaxRetries(0);

        boolean result = failureHandler.shouldRetry(task, failedResult, 0);

        assertFalse(result, "maxRetries为0时不应重试");
    }

    @Test
    @DisplayName("shouldRetry - maxRetries为null时返回false")
    void shouldRetry_returnsFalse_whenMaxRetriesIsNull() {
        task.setMaxRetries(null);

        boolean result = failureHandler.shouldRetry(task, failedResult, 0);

        assertFalse(result, "maxRetries为null时不应重试");
    }

    @Test
    @DisplayName("shouldRetry - 当前重试次数大于等于最大重试次数时返回false")
    void shouldRetry_returnsFalse_whenCurrentRetryCountExceedsMax() {
        // maxRetries=3, currentRetryCount=3, 已达上限
        boolean result = failureHandler.shouldRetry(task, failedResult, 3);

        assertFalse(result, "当前重试次数已达上限时不应重试");
    }

    @Test
    @DisplayName("shouldRetry - FAILED状态且有剩余重试次数时返回true")
    void shouldRetry_returnsTrue_forFailedStatusWithRetriesRemaining() {
        // maxRetries=3, currentRetryCount=1, 还有剩余重试次数
        boolean result = failureHandler.shouldRetry(task, failedResult, 1);

        assertTrue(result, "FAILED状态且有剩余重试次数时应重试");
    }

    @Test
    @DisplayName("shouldRetry - TIMEOUT状态且有剩余重试次数时返回true")
    void shouldRetry_returnsTrue_forTimeoutStatusWithRetriesRemaining() {
        // maxRetries=3, currentRetryCount=2, 还有剩余重试次数
        boolean result = failureHandler.shouldRetry(task, timeoutResult, 2);

        assertTrue(result, "TIMEOUT状态且有剩余重试次数时应重试");
    }

    @Test
    @DisplayName("shouldRetry - SUCCESS状态时返回false")
    void shouldRetry_returnsFalse_forSuccessStatus() {
        boolean result = failureHandler.shouldRetry(task, successResult, 0);

        assertFalse(result, "SUCCESS状态不应重试");
    }

    @Test
    @DisplayName("isRetryableError - TIMEOUT状态返回true")
    void isRetryableError_returnsTrue_forTimeoutStatus() {
        boolean result = failureHandler.isRetryableError(timeoutResult);

        assertTrue(result, "TIMEOUT状态应判定为可重试错误");
    }

    @Test
    @DisplayName("isRetryableError - 错误信息包含timeout返回true")
    void isRetryableError_returnsTrue_forErrorContainingTimeout() {
        ExecutionResult result = ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .error("Connection timeout occurred")
                .build();

        assertTrue(failureHandler.isRetryableError(result), "包含timeout的错误应判定为可重试");
    }

    @Test
    @DisplayName("isRetryableError - 错误信息包含connection refused返回true")
    void isRetryableError_returnsTrue_forErrorContainingConnectionRefused() {
        ExecutionResult result = ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .error("Connection refused by remote host")
                .build();

        assertTrue(failureHandler.isRetryableError(result), "包含connection refused的错误应判定为可重试");
    }

    @Test
    @DisplayName("isRetryableError - 错误信息为null时返回false")
    void isRetryableError_returnsTrue_forNullError() {
        ExecutionResult result = ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .error(null)
                .build();

        assertFalse(failureHandler.isRetryableError(result), "null错误信息应判定为不可重试");
    }

    @Test
    @DisplayName("isRetryableError - 不可重试的错误返回false")
    void isRetryableError_returnsFalse_forNonRetryableError() {
        ExecutionResult result = ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .error("Invalid command format")
                .build();

        assertFalse(failureHandler.isRetryableError(result), "非临时性错误应判定为不可重试");
    }

    @Test
    @DisplayName("recordFailure - 正确委托给ExecutionHistoryService")
    void recordFailure_delegatesToExecutionHistoryService() {
        Long executionId = 100L;
        ExecutionResult result = ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .output("部分输出")
                .error("执行失败")
                .exitCode(1)
                .durationMs(5000L)
                .memoryUsedMb(256)
                .build();

        failureHandler.recordFailure(executionId, result);

        // 验证调用了executionHistoryService的updateExecutionResult方法，参数正确
        verify(executionHistoryService).updateExecutionResult(
                eq(executionId),
                eq(ExecutionStatus.FAILED),
                eq("部分输出"),
                eq("执行失败"),
                eq(1),
                eq(5000L),
                eq(256)
        );
    }
}
