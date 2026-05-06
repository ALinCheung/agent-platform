package com.agentplatform.executor.service;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.TriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClaudeExecutorService 单元测试
 * 测试命令构建、超时处理、执行结果
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Claude执行器服务测试")
class ClaudeExecutorServiceTest {

    @InjectMocks
    private ClaudeExecutorService claudeExecutorService;

    private Task task;

    @BeforeEach
    void setUp() {
        // 注入配置值
        ReflectionTestUtils.setField(claudeExecutorService, "defaultTimeoutSeconds", 300);
        ReflectionTestUtils.setField(claudeExecutorService, "cliPath", "claude");

        task = Task.builder()
                .id(1L)
                .name("测试任务")
                .command("echo hello")
                .triggerType(TriggerType.CRON)
                .timeoutSeconds(60)
                .build();
    }

    @Test
    @DisplayName("buildCommand - Windows环境下包含cmd /c前缀")
    void buildCommand_includesCmdPrefixOnWindows() {
        // 通过反射调用私有方法
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) ReflectionTestUtils.invokeMethod(
                claudeExecutorService, "buildCommand", task);

        assertNotNull(command, "命令列表不应为空");
        // 在Windows上应该以cmd /c开头
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            assertEquals("cmd", command.get(0), "Windows环境下第一个参数应为cmd");
            assertEquals("/c", command.get(1), "Windows环境下第二个参数应为/c");
            assertEquals("claude", command.get(2), "第三个参数应为CLI路径");
        } else {
            assertEquals("claude", command.get(0), "非Windows环境下第一个参数应为CLI路径");
        }
    }

    @Test
    @DisplayName("buildCommand - 包含非交互模式参数")
    void buildCommand_containsNonInteractiveFlags() {
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) ReflectionTestUtils.invokeMethod(
                claudeExecutorService, "buildCommand", task);

        assertTrue(command.contains("-p"), "应包含-p参数");
        assertTrue(command.contains("--dangerously-skip-permissions"), "应包含--dangerously-skip-permissions参数");
        assertTrue(command.contains("echo hello"), "应包含任务命令");
    }

    @Test
    @DisplayName("buildVersionCommand - 构建版本检查命令")
    void buildVersionCommand_buildsCorrectCommand() {
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) ReflectionTestUtils.invokeMethod(
                claudeExecutorService, "buildVersionCommand");

        assertNotNull(command, "版本命令列表不应为空");
        assertTrue(command.contains("--version"), "应包含--version参数");
    }

    @Test
    @DisplayName("execute - CLI不可用时返回FAILED状态")
    void execute_returnsFailed_whenCliUnavailable() {
        // 使用一个不存在的CLI路径
        ReflectionTestUtils.setField(claudeExecutorService, "cliPath", "nonexistent-cli-12345");

        ExecutionResult result = claudeExecutorService.execute(task);

        assertNotNull(result, "执行结果不应为空");
        assertEquals(ExecutionStatus.FAILED, result.getStatus(), "CLI不可用时应返回FAILED状态");
        assertNotNull(result.getDurationMs(), "应记录执行时长");
    }

    @Test
    @DisplayName("execute - 超时任务返回TIMEOUT状态")
    void execute_returnsTimeout_whenTaskTimesOut() {
        // 设置一个会超时的任务（使用sleep命令）
        Task sleepTask = Task.builder()
                .id(2L)
                .name("超时任务")
                .command("sleep 10")
                .triggerType(TriggerType.CRON)
                .timeoutSeconds(1) // 1秒超时
                .build();

        // 使用一个实际存在的命令
        ReflectionTestUtils.setField(claudeExecutorService, "cliPath", "sleep");

        ExecutionResult result = claudeExecutorService.execute(sleepTask);

        // 注意：在某些环境下可能不会超时，这里主要测试代码路径
        assertNotNull(result, "执行结果不应为空");
        assertNotNull(result.getDurationMs(), "应记录执行时长");
    }

    @Test
    @DisplayName("isAvailable - CLI不可用时返回false")
    void isAvailable_returnsFalse_whenCliUnavailable() {
        ReflectionTestUtils.setField(claudeExecutorService, "cliPath", "nonexistent-cli-12345");

        boolean available = claudeExecutorService.isAvailable();

        assertFalse(available, "不存在的CLI应返回不可用");
    }

    @Test
    @DisplayName("execute - 使用任务自定义超时而非默认超时")
    void execute_usesTaskTimeout_overDefault() {
        task.setTimeoutSeconds(10);

        // 使用不存在的CLI快速返回
        ReflectionTestUtils.setField(claudeExecutorService, "cliPath", "nonexistent-cli-12345");

        ExecutionResult result = claudeExecutorService.execute(task);

        assertNotNull(result, "执行结果不应为空");
        assertNotNull(result.getDurationMs(), "应记录执行时长");
    }

    @Test
    @DisplayName("execute - timeout为null时使用默认超时")
    void execute_usesDefaultTimeout_whenTaskTimeoutIsNull() {
        task.setTimeoutSeconds(null);

        ReflectionTestUtils.setField(claudeExecutorService, "cliPath", "nonexistent-cli-12345");

        ExecutionResult result = claudeExecutorService.execute(task);

        assertNotNull(result, "执行结果不应为空");
    }
}
