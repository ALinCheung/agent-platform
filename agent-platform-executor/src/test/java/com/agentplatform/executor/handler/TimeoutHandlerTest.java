package com.agentplatform.executor.handler;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.enums.ExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeoutHandler 单元测试
 * 测试超时处理器的超时判定和结果创建逻辑
 */
@DisplayName("超时处理器测试")
class TimeoutHandlerTest {

    private TimeoutHandler timeoutHandler;

    @BeforeEach
    void setUp() {
        timeoutHandler = new TimeoutHandler();
    }

    @Test
    @DisplayName("createTimeoutResult - 返回TIMEOUT状态和正确的错误消息")
    void createTimeoutResult_returnsTimeoutStatusWithCorrectMessage() {
        long durationMs = 60000L;
        int timeoutSeconds = 30;

        ExecutionResult result = timeoutHandler.createTimeoutResult(durationMs, timeoutSeconds);

        assertNotNull(result, "返回结果不应为null");
        assertEquals(ExecutionStatus.TIMEOUT, result.getStatus(), "状态应为TIMEOUT");
        assertEquals(durationMs, result.getDurationMs(), "耗时应与输入一致");
        assertTrue(result.getError().contains("执行超时"), "错误消息应包含'执行超时'");
        assertTrue(result.getError().contains("30秒"), "错误消息应包含超时秒数");
    }

    @Test
    @DisplayName("isTimeout - 超过超时时间返回true")
    void isTimeout_returnsTrue_whenElapsedExceedsTimeout() {
        // 设置开始时间为2分钟前，超时时间为60秒
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(120);
        int timeoutSeconds = 60;

        boolean result = timeoutHandler.isTimeout(startTime, timeoutSeconds);

        assertTrue(result, "已超过超时时间应返回true");
    }

    @Test
    @DisplayName("isTimeout - 未超过超时时间返回false")
    void isTimeout_returnsFalse_whenWithinTimeout() {
        // 设置开始时间为10秒前，超时时间为60秒
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(10);
        int timeoutSeconds = 60;

        boolean result = timeoutHandler.isTimeout(startTime, timeoutSeconds);

        assertFalse(result, "未超过超时时间应返回false");
    }

    @Test
    @DisplayName("getElapsedTime - 返回正确的已执行秒数")
    void getElapsedTime_returnsCorrectSeconds() {
        // 设置开始时间为30秒前
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(30);

        long elapsed = timeoutHandler.getElapsedTime(startTime);

        // 由于执行时间有微小偏差，允许1秒的误差
        assertTrue(elapsed >= 29 && elapsed <= 31,
                "已执行时间应在29-31秒之间，实际值: " + elapsed);
    }
}
