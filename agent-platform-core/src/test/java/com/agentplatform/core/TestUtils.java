package com.agentplatform.core;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.TriggerType;

import java.time.LocalDateTime;

/**
 * 测试工具类 - 提供通用测试数据创建方法
 */
public class TestUtils {

    /**
     * 创建测试任务
     */
    public static Task createTestTask(String name, String command) {
        return Task.builder()
                .name(name)
                .description("测试任务描述")
                .command(command)
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                .timeoutSeconds(300)
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .enabled(true)
                .successCount(0)
                .failureCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建测试任务版本
     */
    public static TaskVersion createTestTaskVersion(Long taskId, Integer version, String command) {
        return TaskVersion.builder()
                .taskId(taskId)
                .version(version)
                .command(command)
                .timeoutSeconds(300)
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .changeType(ChangeType.CREATE)
                .changeDescription("初始版本")
                .createdAt(LocalDateTime.now())
                .createdBy("test")
                .build();
    }

    /**
     * 创建测试执行记录
     */
    public static TaskExecution createTestExecution(Long taskId, ExecutionStatus status) {
        return TaskExecution.builder()
                .taskId(taskId)
                .status(status)
                .retryCount(0)
                .output("测试输出")
                .error(null)
                .exitCode(0)
                .durationMs(1000L)
                .memoryUsedMb(128)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now().plusSeconds(1))
                .build();
    }

    /**
     * 生成随机任务名称
     */
    public static String generateRandomTaskName() {
        return "test-task-" + System.currentTimeMillis();
    }

    /**
     * 生成随机命令
     */
    public static String generateRandomCommand() {
        return "echo 'test command " + System.currentTimeMillis() + "'";
    }

    /**
     * 获取当前时间
     */
    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
}
