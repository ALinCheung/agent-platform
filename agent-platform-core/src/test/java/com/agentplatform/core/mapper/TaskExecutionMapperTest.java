package com.agentplatform.core.mapper;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskExecutionMapper 数据访问层测试
 * 测试任务执行记录相关的自定义SQL查询方法
 *
 * 注意：countToday() 和 countByDays() 使用了 SQLite 特有的 SQL 语法
 * （DATE('now', 'localtime')、datetime('now', '-' || #{days} || ' days', 'localtime')），
 * 与 H2 不兼容，因此不在此测试范围内。
 */
@DisplayName("TaskExecutionMapper 测试")
class TaskExecutionMapperTest extends BaseTest {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskExecutionMapper taskExecutionMapper;

    /**
     * 测试按状态统计执行次数
     * 验证不同状态的执行记录能被正确分组计数
     */
    @Test
    @DisplayName("countByStatus - 按状态统计执行次数")
    void testCountByStatus() {
        // 创建任务
        Task task = TestUtils.createTestTask("status-test-task", "echo test");
        task.setId(null);
        taskMapper.insert(task);

        // 创建不同状态的执行记录：2个SUCCESS，1个FAILED，1个TIMEOUT
        TaskExecution success1 = TestUtils.createTestExecution(task.getId(), ExecutionStatus.SUCCESS);
        success1.setId(null);
        taskExecutionMapper.insert(success1);

        TaskExecution success2 = TestUtils.createTestExecution(task.getId(), ExecutionStatus.SUCCESS);
        success2.setId(null);
        taskExecutionMapper.insert(success2);

        TaskExecution failed = TestUtils.createTestExecution(task.getId(), ExecutionStatus.FAILED);
        failed.setId(null);
        taskExecutionMapper.insert(failed);

        TaskExecution timeout = TestUtils.createTestExecution(task.getId(), ExecutionStatus.TIMEOUT);
        timeout.setId(null);
        taskExecutionMapper.insert(timeout);

        // 执行查询
        List<Map<String, Object>> result = taskExecutionMapper.countByStatus();

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 将结果按status索引，便于断言
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            String status = (String) row.get("status");
            long count = ((Number) row.get("count")).longValue();
            countMap.put(status, count);
        }

        assertEquals(2L, countMap.get("SUCCESS"));
        assertEquals(1L, countMap.get("FAILED"));
        assertEquals(1L, countMap.get("TIMEOUT"));
    }

    /**
     * 测试获取执行耗时统计
     * 验证只统计状态为SUCCESS且duration_ms不为NULL的执行记录
     */
    @Test
    @DisplayName("getDurationStats - 获取成功执行的耗时统计")
    void testGetDurationStats() {
        // 创建任务
        Task task = TestUtils.createTestTask("duration-test-task", "echo test");
        task.setId(null);
        taskMapper.insert(task);

        // 创建成功执行记录，设置不同的耗时
        TaskExecution exec1 = TestUtils.createTestExecution(task.getId(), ExecutionStatus.SUCCESS);
        exec1.setId(null);
        exec1.setDurationMs(1000L);
        taskExecutionMapper.insert(exec1);

        TaskExecution exec2 = TestUtils.createTestExecution(task.getId(), ExecutionStatus.SUCCESS);
        exec2.setId(null);
        exec2.setDurationMs(2000L);
        taskExecutionMapper.insert(exec2);

        TaskExecution exec3 = TestUtils.createTestExecution(task.getId(), ExecutionStatus.SUCCESS);
        exec3.setId(null);
        exec3.setDurationMs(3000L);
        taskExecutionMapper.insert(exec3);

        // 创建一个失败的执行记录（不应被统计）
        TaskExecution failedExec = TestUtils.createTestExecution(task.getId(), ExecutionStatus.FAILED);
        failedExec.setId(null);
        failedExec.setDurationMs(500L);
        taskExecutionMapper.insert(failedExec);

        // 创建一个成功但duration为NULL的执行记录（不应被统计）
        TaskExecution nullDurationExec = TestUtils.createTestExecution(task.getId(), ExecutionStatus.SUCCESS);
        nullDurationExec.setId(null);
        nullDurationExec.setDurationMs(null);
        taskExecutionMapper.insert(nullDurationExec);

        // 执行查询
        Map<String, Object> result = taskExecutionMapper.getDurationStats();

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.get("avgDuration"));
        assertNotNull(result.get("maxDuration"));
        assertNotNull(result.get("minDuration"));

        // 验证最大值和最小值
        assertEquals(3000L, ((Number) result.get("maxDuration")).longValue());
        assertEquals(1000L, ((Number) result.get("minDuration")).longValue());

        // 验证平均值约为2000
        double avgDuration = ((Number) result.get("avgDuration")).doubleValue();
        assertTrue(avgDuration >= 1999.0 && avgDuration <= 2001.0,
                "平均耗时应约为2000，实际值: " + avgDuration);
    }

    /**
     * 测试失败次数最多的任务TOP榜
     * 验证按失败执行次数降序排列，返回指定数量的记录
     */
    @Test
    @DisplayName("topFailedTasks - 失败次数最多的任务TOP榜")
    void testTopFailedTasks() {
        // 创建任务A（3次失败）
        Task taskA = TestUtils.createTestTask("task-a-most-failures", "echo a");
        taskA.setId(null);
        taskMapper.insert(taskA);

        for (int i = 0; i < 3; i++) {
            TaskExecution exec = TestUtils.createTestExecution(taskA.getId(), ExecutionStatus.FAILED);
            exec.setId(null);
            taskExecutionMapper.insert(exec);
        }

        // 创建任务B（1次失败）
        Task taskB = TestUtils.createTestTask("task-b-fewer-failures", "echo b");
        taskB.setId(null);
        taskMapper.insert(taskB);

        TaskExecution execB = TestUtils.createTestExecution(taskB.getId(), ExecutionStatus.FAILED);
        execB.setId(null);
        taskExecutionMapper.insert(execB);

        // 创建任务C（2次失败 + 1次超时，共3次计入失败统计）
        Task taskC = TestUtils.createTestTask("task-c-with-timeout", "echo c");
        taskC.setId(null);
        taskMapper.insert(taskC);

        for (int i = 0; i < 2; i++) {
            TaskExecution exec = TestUtils.createTestExecution(taskC.getId(), ExecutionStatus.FAILED);
            exec.setId(null);
            taskExecutionMapper.insert(exec);
        }
        TaskExecution timeoutExec = TestUtils.createTestExecution(taskC.getId(), ExecutionStatus.TIMEOUT);
        timeoutExec.setId(null);
        taskExecutionMapper.insert(timeoutExec);

        // 查询TOP 3
        List<Map<String, Object>> result = taskExecutionMapper.topFailedTasks(3);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证排序：task-a和task-c并列3次失败，task-b 1次失败
        // 前两名的失败次数应 >= 第三名
        long firstFailures = ((Number) result.get(0).get("failureCount")).longValue();
        long secondFailures = ((Number) result.get(1).get("failureCount")).longValue();
        long thirdFailures = ((Number) result.get(2).get("failureCount")).longValue();

        assertTrue(firstFailures >= thirdFailures, "第一名失败次数应 >= 第三名");
        assertTrue(secondFailures >= thirdFailures, "第二名失败次数应 >= 第三名");
        assertEquals(3L, firstFailures);
        assertEquals(1L, thirdFailures);

        // 验证最后一名是task-b
        String lastTaskName = (String) result.get(2).get("name");
        assertEquals("task-b-fewer-failures", lastTaskName);
    }

    /**
     * 测试平均耗时最长的任务TOP榜
     * 验证按平均耗时降序排列，只统计成功的执行记录
     */
    @Test
    @DisplayName("topSlowTasks - 平均耗时最长的任务TOP榜")
    void testTopSlowTasks() {
        // 创建任务A（平均耗时最长：5000ms）
        Task taskA = TestUtils.createTestTask("task-a-slowest", "echo a");
        taskA.setId(null);
        taskMapper.insert(taskA);

        TaskExecution execA1 = TestUtils.createTestExecution(taskA.getId(), ExecutionStatus.SUCCESS);
        execA1.setId(null);
        execA1.setDurationMs(4000L);
        taskExecutionMapper.insert(execA1);

        TaskExecution execA2 = TestUtils.createTestExecution(taskA.getId(), ExecutionStatus.SUCCESS);
        execA2.setId(null);
        execA2.setDurationMs(6000L);
        taskExecutionMapper.insert(execA2);

        // 创建任务B（平均耗时中等：2000ms）
        Task taskB = TestUtils.createTestTask("task-b-medium", "echo b");
        taskB.setId(null);
        taskMapper.insert(taskB);

        TaskExecution execB1 = TestUtils.createTestExecution(taskB.getId(), ExecutionStatus.SUCCESS);
        execB1.setId(null);
        execB1.setDurationMs(1500L);
        taskExecutionMapper.insert(execB1);

        TaskExecution execB2 = TestUtils.createTestExecution(taskB.getId(), ExecutionStatus.SUCCESS);
        execB2.setId(null);
        execB2.setDurationMs(2500L);
        taskExecutionMapper.insert(execB2);

        // 创建任务C（平均耗时最短：500ms）
        Task taskC = TestUtils.createTestTask("task-c-fastest", "echo c");
        taskC.setId(null);
        taskMapper.insert(taskC);

        TaskExecution execC = TestUtils.createTestExecution(taskC.getId(), ExecutionStatus.SUCCESS);
        execC.setId(null);
        execC.setDurationMs(500L);
        taskExecutionMapper.insert(execC);

        // 为任务A创建一个失败的执行记录（不应影响平均耗时统计）
        TaskExecution failedExec = TestUtils.createTestExecution(taskA.getId(), ExecutionStatus.FAILED);
        failedExec.setId(null);
        failedExec.setDurationMs(100L);
        taskExecutionMapper.insert(failedExec);

        // 查询TOP 3
        List<Map<String, Object>> result = taskExecutionMapper.topSlowTasks(3);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证排序：task-a(5000ms) > task-b(2000ms) > task-c(500ms)
        String firstTaskName = (String) result.get(0).get("name");
        String secondTaskName = (String) result.get(1).get("name");
        String thirdTaskName = (String) result.get(2).get("name");

        assertEquals("task-a-slowest", firstTaskName);
        assertEquals("task-b-medium", secondTaskName);
        assertEquals("task-c-fastest", thirdTaskName);

        // 验证平均耗时递减
        double firstAvg = ((Number) result.get(0).get("avgDuration")).doubleValue();
        double secondAvg = ((Number) result.get(1).get("avgDuration")).doubleValue();
        double thirdAvg = ((Number) result.get(2).get("avgDuration")).doubleValue();

        assertTrue(firstAvg > secondAvg, "第一名平均耗时应 > 第二名");
        assertTrue(secondAvg > thirdAvg, "第二名平均耗时应 > 第三名");

        // 验证具体平均值（允许浮点误差）
        assertTrue(firstAvg >= 4999.0 && firstAvg <= 5001.0,
                "task-a平均耗时应约为5000，实际值: " + firstAvg);
        assertTrue(secondAvg >= 1999.0 && secondAvg <= 2001.0,
                "task-b平均耗时应约为2000，实际值: " + secondAvg);
        assertTrue(thirdAvg >= 499.0 && thirdAvg <= 501.0,
                "task-c平均耗时应约为500，实际值: " + thirdAvg);
    }
}
