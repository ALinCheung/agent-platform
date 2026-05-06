package com.agentplatform.core.service.impl;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionHistoryServiceImpl 单元测试
 * 测试执行历史服务的创建、更新和查询功能
 */
@DisplayName("执行历史服务实现测试")
class ExecutionHistoryServiceImplTest extends BaseTest {

    @Autowired
    private ExecutionHistoryService executionHistoryService;

    @Autowired
    private TaskService taskService;

    /** 测试用任务，每个测试前创建 */
    private Task testTask;

    @BeforeEach
    void setUp() {
        // 创建测试任务，用于后续创建执行记录
        Task task = TestUtils.createTestTask("执行历史测试任务", "echo execution test");
        testTask = taskService.createTask(task);
    }

    @Test
    @DisplayName("createExecution - 创建RUNNING状态的执行记录并设置startedAt")
    void createExecution_createsRecordWithRunningStatus() {
        // 创建执行记录
        TaskExecution execution = executionHistoryService.createExecution(
                testTask.getId(), null);

        // 验证基本字段
        assertNotNull(execution.getId(), "应分配执行记录ID");
        assertEquals(testTask.getId(), execution.getTaskId(), "taskId应匹配");
        assertEquals(ExecutionStatus.RUNNING, execution.getStatus(), "初始状态应为RUNNING");
        assertEquals(0, execution.getRetryCount(), "初始重试次数应为0");
        assertNotNull(execution.getStartedAt(), "startedAt不应为null");
    }

    @Test
    @DisplayName("updateExecutionResult - 更新所有字段并设置finishedAt")
    void updateExecutionResult_updatesAllFields() {
        // 先创建一个执行记录
        TaskExecution execution = executionHistoryService.createExecution(
                testTask.getId(), null);
        Long executionId = execution.getId();

        // 更新执行结果
        String output = "执行成功输出";
        String error = null;
        Integer exitCode = 0;
        Long durationMs = 5000L;
        Integer memoryUsedMb = 256;

        executionHistoryService.updateExecutionResult(
                executionId, ExecutionStatus.SUCCESS,
                output, error, exitCode, durationMs, memoryUsedMb);

        // 重新查询验证
        TaskExecution updated = executionHistoryService.getById(executionId);

        assertNotNull(updated, "应能查询到更新后的执行记录");
        assertEquals(ExecutionStatus.SUCCESS, updated.getStatus(), "状态应更新为SUCCESS");
        assertEquals(output, updated.getOutput(), "输出应匹配");
        assertNull(updated.getError(), "错误信息应为null");
        assertEquals(exitCode, updated.getExitCode(), "退出码应匹配");
        assertEquals(durationMs, updated.getDurationMs(), "耗时应匹配");
        assertEquals(memoryUsedMb, updated.getMemoryUsedMb(), "内存使用应匹配");
        assertNotNull(updated.getFinishedAt(), "finishedAt不应为null");
    }

    @Test
    @DisplayName("updateExecutionResult - 不存在的ID应优雅处理（不抛异常）")
    void updateExecutionResult_handlesNonExistentIdGracefully() {
        Long nonExistentId = 99999L;

        // 不应抛出异常，实现中只打印warn日志并返回
        assertDoesNotThrow(() ->
                executionHistoryService.updateExecutionResult(
                        nonExistentId, ExecutionStatus.FAILED,
                        null, "error", 1, 0L, null));
    }

    @Test
    @DisplayName("getByTaskId - 返回指定任务的执行记录，按startedAt降序排列")
    void getByTaskId_returnsCorrectExecutionsOrderedByDate() {
        // 创建多条执行记录
        TaskExecution exec1 = executionHistoryService.createExecution(
                testTask.getId(), null);
        // 更新第一条为SUCCESS
        executionHistoryService.updateExecutionResult(
                exec1.getId(), ExecutionStatus.SUCCESS,
                "输出1", null, 0, 1000L, 128);

        // 稍微延迟确保时间戳差异
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        TaskExecution exec2 = executionHistoryService.createExecution(
                testTask.getId(), null);
        // 更新第二条为FAILED
        executionHistoryService.updateExecutionResult(
                exec2.getId(), ExecutionStatus.FAILED,
                null, "执行失败", 1, 2000L, 256);

        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        TaskExecution exec3 = executionHistoryService.createExecution(
                testTask.getId(), null);
        // 第三条保持RUNNING状态

        // 查询该任务的所有执行记录
        List<TaskExecution> executions = executionHistoryService.getByTaskId(testTask.getId());

        // 验证返回了3条记录
        assertEquals(3, executions.size(), "应返回3条执行记录");

        // 验证排序：最新的在前（exec3在最前面）
        assertEquals(exec3.getId(), executions.get(0).getId(),
                "最新的执行记录应排在最前");
        assertEquals(exec2.getId(), executions.get(1).getId(),
                "第二条执行记录应排在第二位");
        assertEquals(exec1.getId(), executions.get(2).getId(),
                "最早的执行记录应排在最后");

        // 验证不同状态的记录都在
        assertTrue(executions.stream()
                        .anyMatch(e -> e.getStatus() == ExecutionStatus.RUNNING),
                "应包含RUNNING状态的记录");
        assertTrue(executions.stream()
                        .anyMatch(e -> e.getStatus() == ExecutionStatus.SUCCESS),
                "应包含SUCCESS状态的记录");
        assertTrue(executions.stream()
                        .anyMatch(e -> e.getStatus() == ExecutionStatus.FAILED),
                "应包含FAILED状态的记录");
    }
}
