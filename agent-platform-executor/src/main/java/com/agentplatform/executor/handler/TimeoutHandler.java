package com.agentplatform.executor.handler;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.enums.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 超时处理器
 * 处理任务执行超时的情况
 */
@Slf4j
@Service
public class TimeoutHandler {

    /**
     * 创建超时结果
     */
    public ExecutionResult createTimeoutResult(long durationMs, int timeoutSeconds) {
        log.warn("任务执行超时: durationMs={}, timeoutSeconds={}", durationMs, timeoutSeconds);
        return ExecutionResult.builder()
                .status(ExecutionStatus.TIMEOUT)
                .error(String.format("执行超时（%d秒），已强制终止", timeoutSeconds))
                .durationMs(durationMs)
                .build();
    }

    /**
     * 检查是否超时
     */
    public boolean isTimeout(LocalDateTime startTime, int timeoutSeconds) {
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        return duration.getSeconds() > timeoutSeconds;
    }

    /**
     * 获取已执行时间（秒）
     */
    public long getElapsedTime(LocalDateTime startTime) {
        return Duration.between(startTime, LocalDateTime.now()).getSeconds();
    }
}
