package com.agentplatform.core.service.impl;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.mapper.TaskMapper;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskVersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskServiceImpl 单元测试
 * 测试任务服务的核心功能：创建、更新、启停、查询
 */
@DisplayName("任务服务实现测试")
class TaskServiceImplTest extends BaseTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskVersionService taskVersionService;

    @Autowired
    private TaskMapper taskMapper;

    @Test
    @DisplayName("createTask - 保存任务并返回带ID的任务，同时创建初始版本")
    void createTask_savesAndCreatesInitialVersion() {
        // 准备测试数据
        Task task = TestUtils.createTestTask("创建测试任务", "echo hello");

        // 执行创建
        Task saved = taskService.createTask(task);

        // 验证任务已保存且分配了ID
        assertNotNull(saved.getId(), "保存后的任务应分配ID");
        assertEquals("创建测试任务", saved.getName());
        assertEquals("echo hello", saved.getCommand());

        // 验证初始版本已创建
        List<TaskVersion> versions = taskVersionService.getVersions(saved.getId());
        assertFalse(versions.isEmpty(), "应创建初始版本");
        assertEquals(1, versions.get(0).getVersion(), "初始版本号应为1");
        assertEquals(ChangeType.CREATE, versions.get(0).getChangeType());
        assertEquals("初始创建", versions.get(0).getChangeDescription());
    }

    @Test
    @DisplayName("createTask - 当字段为null时设置默认值")
    void createTask_setsDefaultValues() {
        // 构建一个大部分字段为null的任务
        Task task = Task.builder()
                .name("默认值测试任务")
                .command("echo defaults")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                // 以下字段故意不设置，应由createTask填充默认值
                .enabled(null)
                .timeoutSeconds(null)
                .maxRetries(null)
                .retryIntervalSeconds(null)
                .successCount(null)
                .failureCount(null)
                .build();

        // 执行创建
        Task saved = taskService.createTask(task);

        // 验证默认值已设置
        assertTrue(saved.getEnabled(), "enabled默认值应为true");
        assertEquals(300, saved.getTimeoutSeconds(), "timeoutSeconds默认值应为300");
        assertEquals(0, saved.getMaxRetries(), "maxRetries默认值应为0");
        assertEquals(60, saved.getRetryIntervalSeconds(), "retryIntervalSeconds默认值应为60");
        assertEquals(0, saved.getSuccessCount(), "successCount默认值应为0");
        assertEquals(0, saved.getFailureCount(), "failureCount默认值应为0");
    }

    @Test
    @DisplayName("updateTask - 更新任务字段并保存版本")
    void updateTask_updatesFieldsAndSavesVersion() {
        // 先创建一个任务
        Task original = TestUtils.createTestTask("更新前任务", "echo original");
        Task saved = taskService.createTask(original);
        Long taskId = saved.getId();

        // 构建更新数据
        Task updateData = Task.builder()
                .name("更新后任务")
                .command("echo updated")
                .description("更新后的描述")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 */5 * * * ?")
                .timeoutSeconds(600)
                .maxRetries(5)
                .retryIntervalSeconds(120)
                .build();

        // 执行更新
        Task updated = taskService.updateTask(taskId, updateData);

        // 验证字段已更新
        assertEquals("更新后任务", updated.getName());
        assertEquals("echo updated", updated.getCommand());
        assertEquals("更新后的描述", updated.getDescription());
        assertEquals("0 */5 * * * ?", updated.getCronExpression());
        assertEquals(600, updated.getTimeoutSeconds());
        assertEquals(5, updated.getMaxRetries());
        assertEquals(120, updated.getRetryIntervalSeconds());

        // 验证保存了旧版本（初始版本 + 更新版本 = 2个版本）
        List<TaskVersion> versions = taskVersionService.getVersions(taskId);
        assertTrue(versions.size() >= 2, "应至少有2个版本（初始+更新）");

        // 最新版本应为UPDATE类型
        TaskVersion latestVersion = versions.get(0);
        assertEquals(ChangeType.UPDATE, latestVersion.getChangeType());
        assertEquals("更新任务配置", latestVersion.getChangeDescription());
    }

    @Test
    @DisplayName("updateTask - 不存在的任务ID应抛出异常")
    void updateTask_throwsForNonExistentId() {
        Task updateData = TestUtils.createTestTask("不存在", "echo none");
        Long nonExistentId = 99999L;

        // 验证抛出RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateTask(nonExistentId, updateData));
        assertTrue(exception.getMessage().contains("任务不存在"),
                "异常信息应包含'任务不存在'");
    }

    @Test
    @DisplayName("enableTask/disableTask - 切换任务启用状态")
    void enableDisableTask_togglesEnabledFlag() {
        // 创建一个任务（默认enabled=true）
        Task task = TestUtils.createTestTask("启停测试任务", "echo toggle");
        Task saved = taskService.createTask(task);
        Long taskId = saved.getId();

        // 验证初始状态为启用
        assertTrue(taskService.getById(taskId).getEnabled(), "新建任务应为启用状态");

        // 禁用任务
        taskService.disableTask(taskId);
        assertFalse(taskService.getById(taskId).getEnabled(), "禁用后应为false");

        // 重新启用任务
        taskService.enableTask(taskId);
        assertTrue(taskService.getById(taskId).getEnabled(), "重新启用后应为true");
    }

    @Test
    @DisplayName("enableTask - 不存在的任务ID应抛出异常")
    void enableTask_throwsForNonExistentId() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.enableTask(99999L));
        assertTrue(exception.getMessage().contains("任务不存在"),
                "异常信息应包含'任务不存在'");
    }

    @Test
    @DisplayName("disableTask - 不存在的任务ID应抛出异常")
    void disableTask_throwsForNonExistentId() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.disableTask(99999L));
        assertTrue(exception.getMessage().contains("任务不存在"),
                "异常信息应包含'任务不存在'");
    }

    @Test
    @DisplayName("listAll - 返回按创建时间降序排列的任务列表")
    void listAll_returnsTasksOrderedByCreatedAtDesc() {
        // 创建多个任务，使用不同名称确保顺序可验证
        Task task1 = TestUtils.createTestTask("最早的任务", "echo first");
        taskService.createTask(task1);

        // 使用微量延迟确保时间戳差异（H2中created_at为VARCHAR存储）
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        Task task2 = TestUtils.createTestTask("较晚的任务", "echo second");
        taskService.createTask(task2);

        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        Task task3 = TestUtils.createTestTask("最晚的任务", "echo third");
        taskService.createTask(task3);

        // 查询全部任务
        List<Task> allTasks = taskService.listAll();

        // 验证至少返回3个任务
        assertTrue(allTasks.size() >= 3, "应至少返回3个任务");

        // 验证排序：最晚创建的排在最前面
        // 找到我们创建的任务在列表中的位置
        Task foundFirst = allTasks.stream()
                .filter(t -> "最早的任务".equals(t.getName()))
                .findFirst().orElse(null);
        Task foundLast = allTasks.stream()
                .filter(t -> "最晚的任务".equals(t.getName()))
                .findFirst().orElse(null);

        assertNotNull(foundFirst, "应能找到最早创建的任务");
        assertNotNull(foundLast, "应能找到最晚创建的任务");
        assertTrue(allTasks.indexOf(foundLast) < allTasks.indexOf(foundFirst),
                "最晚创建的任务应排在最早创建的任务前面");
    }
}
