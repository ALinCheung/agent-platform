package com.agentplatform.executor.handler;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 失败处理器
 * 处理任务执行失败的情况，包括失败分类和重试判断
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailureHandler {

    private final ExecutionHistoryService executionHistoryService;

    /**
     * 判断是否可重试
     * @param task 任务定义
     * @param executionResult 执行结果
     * @param currentRetryCount 当前重试次数
     * @return 是否应该重试
     */
    public boolean shouldRetry(Task task, ExecutionResult executionResult, int currentRetryCount) {
        if (task.getMaxRetries() == null || task.getMaxRetries() == 0) {
            return false;
        }

        if (currentRetryCount >= task.getMaxRetries()) {
            log.info("已达到最大重试次数: taskId={}, maxRetries={}", task.getId(), task.getMaxRetries());
            return false;
        }

        // 只有超时和失败才重试，成功不重试
        ExecutionStatus status = executionResult.getStatus();
        return status == ExecutionStatus.FAILED || status == ExecutionStatus.TIMEOUT;
    }

    /**
     * 判断错误是否可重试
     * 可重试：超时、网络错误等临时性故障
     * 不可重试：命令格式错误、CLI不可用等
     */
    public boolean isRetryableError(ExecutionResult executionResult) {
        if (executionResult.getStatus() == ExecutionStatus.TIMEOUT) {
            return true;
        }

        String error = executionResult.getError();
        if (error == null) {
            return false;
        }

        // 可重试的错误模式
        String[] retryablePatterns = {
                "timeout", "connection refused", "network error",
                "temporary failure", "service unavailable"
        };

        String lowerError = error.toLowerCase();
        for (String pattern : retryablePatterns) {
            if (lowerError.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 记录失败信息
     */
    public void recordFailure(Long executionId, ExecutionResult result) {
        executionHistoryService.updateExecutionResult(
                executionId,
                result.getStatus(),
                result.getOutput(),
                result.getError(),
                result.getExitCode(),
                result.getDurationMs(),
                result.getMemoryUsedMb()
        );
        log.warn("记录执行失败: executionId={}, status={}, error={}",
                executionId, result.getStatus(), result.getError());
    }
}
