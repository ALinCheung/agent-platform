package com.agentplatform.core.service.impl;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.mapper.TaskExecutionMapper;
import com.agentplatform.core.mapper.TaskMapper;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.core.service.StatisticsCollector;
import com.agentplatform.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatisticsCollectorImpl 单元测试
 * 测试统计收集器的各项统计功能
 *
 * 注意：H2测试环境中部分SQLite日期函数不可用，
 * 涉及日期筛选的查询可能返回空结果，测试侧重于验证返回结构正确性
 */
@DisplayName("统计收集器实现测试")
class StatisticsCollectorImplTest extends BaseTest {

    @Autowired
    private StatisticsCollector statisticsCollector;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ExecutionHistoryService executionHistoryService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskExecutionMapper taskExecutionMapper;

    /** 测试用任务 */
    private Task cronTask;
    private Task webhookTask;

    @BeforeEach
    void setUp() {
        // 创建不同触发类型的任务用于统计
        Task task1 = TestUtils.createTestTask("CRON统计任务", "echo cron stats");
        task1.setTriggerType(TriggerType.CRON);
        cronTask = taskService.createTask(task1);

        Task task2 = Task.builder()
                .name("WEBHOOK统计任务")
                .command("echo webhook stats")
                .triggerType(TriggerType.WEBHOOK)
                .webhookPath("/webhook/stats")
                .timeoutSeconds(300)
                .maxRetries(0)
                .retryIntervalSeconds(60)
                .enabled(true)
                .build();
        webhookTask = taskService.createTask(task2);

        // 创建一些执行记录用于统计
        TaskExecution exec1 = executionHistoryService.createExecution(cronTask.getId(), null);
        executionHistoryService.updateExecutionResult(
                exec1.getId(), ExecutionStatus.SUCCESS, "成功", null, 0, 1000L, 128);

        TaskExecution exec2 = executionHistoryService.createExecution(cronTask.getId(), null);
        executionHistoryService.updateExecutionResult(
                exec2.getId(), ExecutionStatus.FAILED, null, "失败", 1, 2000L, 256);

        TaskExecution exec3 = executionHistoryService.createExecution(webhookTask.getId(), null);
        executionHistoryService.updateExecutionResult(
                exec3.getId(), ExecutionStatus.SUCCESS, "成功", null, 0, 500L, 64);
    }

    @Test
    @DisplayName("getOverview - 返回任务统计和今日执行统计")
    void getOverview_returnsTaskAndTodayStats() {
        Map<String, Object> overview = statisticsCollector.getOverview();

        assertNotNull(overview, "总览不应为null");
        assertTrue(overview.containsKey("tasks"), "应包含tasks统计");
        assertTrue(overview.containsKey("today"), "应包含today统计");

        // 验证tasks统计结构
        @SuppressWarnings("unchecked")
        Map<String, Object> taskStats = (Map<String, Object>) overview.get("tasks");
        assertNotNull(taskStats, "任务统计不应为null");
        assertTrue(taskStats.containsKey("total"), "任务统计应包含total");
        assertTrue(taskStats.containsKey("enabled"), "任务统计应包含enabled");
        assertTrue(taskStats.containsKey("disabled"), "任务统计应包含disabled");

        // 验证today统计结构
        @SuppressWarnings("unchecked")
        Map<String, Object> todayStats = (Map<String, Object>) overview.get("today");
        assertNotNull(todayStats, "今日统计不应为null");
        // countToday查询在H2中可能因SQLite日期函数返回空结果，只验证结构存在
    }

    @Test
    @DisplayName("getTaskStats - 返回按状态和触发类型分类的统计")
    void getTaskStats_returnsStatusAndTriggerTypeBreakdown() {
        Map<String, Object> stats = statisticsCollector.getTaskStats();

        assertNotNull(stats, "任务统计不应为null");
        assertTrue(stats.containsKey("status"), "应包含status统计");
        assertTrue(stats.containsKey("triggerType"), "应包含triggerType统计");

        // 验证status统计结构
        @SuppressWarnings("unchecked")
        Map<String, Object> statusStats = (Map<String, Object>) stats.get("status");
        assertNotNull(statusStats, "状态统计不应为null");
        assertTrue(statusStats.containsKey("total"), "状态统计应包含total");

        // 验证triggerType统计结构
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggerTypeStats =
                (List<Map<String, Object>>) stats.get("triggerType");
        assertNotNull(triggerTypeStats, "触发类型统计不应为null");
        // 应至少有CRON和WEBHOOK两种类型
        assertTrue(triggerTypeStats.size() >= 2,
                "应至少有2种触发类型（CRON和WEBHOOK）");

        // 验证包含CRON和WEBHOOK类型
        boolean hasCron = triggerTypeStats.stream()
                .anyMatch(m -> "CRON".equals(m.get("triggerType")));
        boolean hasWebhook = triggerTypeStats.stream()
                .anyMatch(m -> "WEBHOOK".equals(m.get("triggerType")));
        assertTrue(hasCron, "应包含CRON触发类型统计");
        assertTrue(hasWebhook, "应包含WEBHOOK触发类型统计");
    }

    @Test
    @DisplayName("getExecutionStats - 返回今日统计和按状态统计")
    void getExecutionStats_returnsTodayAndByStatus() {
        Map<String, Object> stats = statisticsCollector.getExecutionStats();

        assertNotNull(stats, "执行统计不应为null");
        assertTrue(stats.containsKey("today"), "应包含today统计");
        assertTrue(stats.containsKey("byStatus"), "应包含byStatus统计");

        // 验证byStatus统计结构
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byStatusStats =
                (List<Map<String, Object>>) stats.get("byStatus");
        assertNotNull(byStatusStats, "按状态统计不应为null");
        // H2中日期函数可能影响结果，但byStatus统计不受日期限制
        // 应包含SUCCESS和FAILED两种状态
        boolean hasSuccess = byStatusStats.stream()
                .anyMatch(m -> "SUCCESS".equals(m.get("status")));
        boolean hasFailed = byStatusStats.stream()
                .anyMatch(m -> "FAILED".equals(m.get("status")));
        assertTrue(hasSuccess, "应包含SUCCESS状态统计");
        assertTrue(hasFailed, "应包含FAILED状态统计");
    }

    @Test
    @DisplayName("getPerformanceStats - 返回耗时统计、失败TOP榜和慢任务TOP榜")
    void getPerformanceStats_returnsDurationTopFailedTopSlow() {
        Map<String, Object> stats = statisticsCollector.getPerformanceStats();

        assertNotNull(stats, "性能统计不应为null");
        assertTrue(stats.containsKey("duration"), "应包含duration统计");
        assertTrue(stats.containsKey("topFailed"), "应包含topFailed统计");
        assertTrue(stats.containsKey("topSlow"), "应包含topSlow统计");

        // 验证duration统计结构
        @SuppressWarnings("unchecked")
        Map<String, Object> durationStats = (Map<String, Object>) stats.get("duration");
        assertNotNull(durationStats, "耗时统计不应为null");
        // 耗时统计应包含avgDuration、maxDuration、minDuration
        assertTrue(durationStats.containsKey("avgDuration"), "耗时统计应包含avgDuration");
        assertTrue(durationStats.containsKey("maxDuration"), "耗时统计应包含maxDuration");
        assertTrue(durationStats.containsKey("minDuration"), "耗时统计应包含minDuration");

        // 验证topFailed统计
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topFailed = (List<Map<String, Object>>) stats.get("topFailed");
        assertNotNull(topFailed, "失败TOP榜不应为null");
        // 至少应有cronTask的失败记录
        assertFalse(topFailed.isEmpty(), "失败TOP榜不应为空");

        // 验证topSlow统计
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topSlow = (List<Map<String, Object>>) stats.get("topSlow");
        assertNotNull(topSlow, "慢任务TOP榜不应为null");
        // 至少应有成功执行的记录
        assertFalse(topSlow.isEmpty(), "慢任务TOP榜不应为空");
    }
}
