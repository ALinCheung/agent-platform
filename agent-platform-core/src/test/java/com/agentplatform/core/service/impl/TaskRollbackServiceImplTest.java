package com.agentplatform.core.service.impl;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskRollbackServiceImpl 集成测试
 * 测试任务回滚服务的版本恢复和保存逻辑
 * 需要Spring上下文（继承BaseTest）
 */
@DisplayName("任务回滚服务测试")
class TaskRollbackServiceImplTest extends BaseTest {

    @Autowired
    private TaskRollbackServiceImpl taskRollbackService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskVersionService taskVersionService;

    private Task testTask;

    @BeforeEach
    void setUp() {
        // 创建测试任务
        testTask = Task.builder()
                .name("回滚测试任务")
                .description("用于测试回滚功能")
                .command("echo original")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                .timeoutSeconds(300)
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .workDir("/tmp/work")
                .enabled(true)
                .successCount(0)
                .failureCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testTask = taskService.createTask(testTask);
    }

    @Test
    @DisplayName("rollback - 恢复目标版本的配置")
    void rollback_restoresTargetVersionConfiguration() {
        // 保存初始版本（版本1）
        taskVersionService.saveVersion(testTask, ChangeType.CREATE, "初始版本");

        // 修改任务配置
        testTask.setCommand("echo modified");
        testTask.setTimeoutSeconds(600);
        testTask.setMaxRetries(5);
        testTask.setWorkDir("/tmp/modified");
        taskService.updateById(testTask);

        // 保存修改后的版本（版本2）
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "修改配置");

        // 再次修改
        testTask.setCommand("echo latest");
        taskService.updateById(testTask);

        // 回滚到版本1
        Task rolledBack = taskRollbackService.rollback(testTask.getId(), 1);

        // 验证配置已恢复到版本1
        assertNotNull(rolledBack, "回滚后的任务不应为null");
        assertEquals("echo original", rolledBack.getCommand(),
                "命令应恢复到版本1的值");
        assertEquals(Integer.valueOf(300), rolledBack.getTimeoutSeconds(),
                "超时时间应恢复到版本1的值");
        assertEquals(Integer.valueOf(3), rolledBack.getMaxRetries(),
                "最大重试次数应恢复到版本1的值");
        assertEquals("/tmp/work", rolledBack.getWorkDir(),
                "工作目录应恢复到版本1的值");
    }

    @Test
    @DisplayName("rollback - 回滚前保存当前版本")
    void rollback_savesCurrentVersionBeforeRestoring() {
        // 保存初始版本
        taskVersionService.saveVersion(testTask, ChangeType.CREATE, "初始版本");

        // 修改并保存
        testTask.setCommand("echo modified");
        taskService.updateById(testTask);
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "修改配置");

        // 回滚到版本1
        taskRollbackService.rollback(testTask.getId(), 1);

        // 验证存在回滚前保存的版本记录（应该是版本3，类型为UPDATE）
        int maxVersion = taskVersionService.getMaxVersion(testTask.getId());
        assertTrue(maxVersion >= 3, "应至少有3个版本记录（初始、修改、回滚前保存）");
    }

    @Test
    @DisplayName("rollback - 创建ROLLBACK类型的版本记录")
    void rollback_createsRollbackVersionRecord() {
        // 保存初始版本
        taskVersionService.saveVersion(testTask, ChangeType.CREATE, "初始版本");

        // 回滚到版本1
        taskRollbackService.rollback(testTask.getId(), 1);

        // 获取所有版本，验证存在ROLLBACK类型的记录
        var versions = taskVersionService.getVersions(testTask.getId());
        boolean hasRollback = versions.stream()
                .anyMatch(v -> v.getChangeType() == ChangeType.ROLLBACK);

        assertTrue(hasRollback, "应存在ROLLBACK类型的版本记录");
    }

    @Test
    @DisplayName("rollback - 任务不存在时抛出异常")
    void rollback_throwsForNonExistentTask() {
        Long nonExistentTaskId = 99999L;

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskRollbackService.rollback(nonExistentTaskId, 1),
                "回滚不存在的任务应抛出异常");

        assertTrue(exception.getMessage().contains("任务不存在"),
                "异常消息应包含'任务不存在'");
    }

    @Test
    @DisplayName("rollback - 版本不存在时抛出异常")
    void rollback_throwsForNonExistentVersion() {
        // 保存初始版本（版本1）
        taskVersionService.saveVersion(testTask, ChangeType.CREATE, "初始版本");

        // 尝试回滚到不存在的版本999
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskRollbackService.rollback(testTask.getId(), 999),
                "回滚不存在的版本应抛出异常");

        assertTrue(exception.getMessage().contains("版本不存在"),
                "异常消息应包含'版本不存在'");
    }
}
