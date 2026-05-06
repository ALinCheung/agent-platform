package com.agentplatform.core.mapper;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.TriggerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskMapper 数据访问层测试
 * 测试任务相关的自定义SQL查询方法
 */
@DisplayName("TaskMapper 测试")
class TaskMapperTest extends BaseTest {

    @Autowired
    private TaskMapper taskMapper;

    /**
     * 测试按触发类型统计任务数量
     * 验证不同触发类型（CRON、WEBHOOK等）的任务能被正确分组计数
     */
    @Test
    @DisplayName("countByTriggerType - 按触发类型统计任务数量")
    void testCountByTriggerType() {
        // 创建2个CRON类型任务
        Task cronTask1 = TestUtils.createTestTask("cron-task-1", "echo cron1");
        cronTask1.setId(null);
        taskMapper.insert(cronTask1);

        Task cronTask2 = TestUtils.createTestTask("cron-task-2", "echo cron2");
        cronTask2.setId(null);
        taskMapper.insert(cronTask2);

        // 创建1个WEBHOOK类型任务
        Task webhookTask = TestUtils.createTestTask("webhook-task-1", "echo webhook");
        webhookTask.setId(null);
        webhookTask.setTriggerType(TriggerType.WEBHOOK);
        webhookTask.setWebhookPath("/hook/test");
        taskMapper.insert(webhookTask);

        // 执行查询
        List<Map<String, Object>> result = taskMapper.countByTriggerType();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 将结果按triggerType索引，便于断言
        Map<String, Long> countMap = new java.util.HashMap<>();
        for (Map<String, Object> row : result) {
            String triggerType = (String) row.get("triggerType");
            long count = ((Number) row.get("count")).longValue();
            countMap.put(triggerType, count);
        }

        assertEquals(2L, countMap.get("CRON"));
        assertEquals(1L, countMap.get("WEBHOOK"));
    }

    /**
     * 测试按启用状态统计任务数量
     * 验证enabled=1的任务归为enabled，enabled=0的任务归为disabled
     */
    @Test
    @DisplayName("countByStatus - 按启用状态统计任务数量")
    void testCountByStatus() {
        // 创建2个启用的任务
        Task enabledTask1 = TestUtils.createTestTask("enabled-task-1", "echo enabled1");
        enabledTask1.setId(null);
        taskMapper.insert(enabledTask1);

        Task enabledTask2 = TestUtils.createTestTask("enabled-task-2", "echo enabled2");
        enabledTask2.setId(null);
        taskMapper.insert(enabledTask2);

        // 创建1个禁用的任务
        Task disabledTask = TestUtils.createTestTask("disabled-task-1", "echo disabled");
        disabledTask.setId(null);
        disabledTask.setEnabled(false);
        taskMapper.insert(disabledTask);

        // 执行查询
        Map<String, Object> result = taskMapper.countByStatus();

        // 验证结果：总数3，启用2，禁用1
        assertNotNull(result);
        assertEquals(3L, ((Number) result.get("total")).longValue());
        assertEquals(2L, ((Number) result.get("enabled")).longValue());
        assertEquals(1L, ((Number) result.get("disabled")).longValue());
    }

    /**
     * 测试查找启用的CRON任务
     * 验证只返回同时满足 enabled=1、trigger_type='CRON'、cron_expression IS NOT NULL 的任务
     */
    @Test
    @DisplayName("findEnabledCronTasks - 查找启用的CRON任务")
    void testFindEnabledCronTasks() {
        // 启用的CRON任务（有cron表达式）- 应该返回
        Task enabledCron = TestUtils.createTestTask("enabled-cron", "echo cron");
        enabledCron.setId(null);
        enabledCron.setCronExpression("0 0 * * * ?");
        taskMapper.insert(enabledCron);

        // 禁用的CRON任务（有cron表达式）- 不应返回
        Task disabledCron = TestUtils.createTestTask("disabled-cron", "echo disabled");
        disabledCron.setId(null);
        disabledCron.setEnabled(false);
        disabledCron.setCronExpression("0 0 * * * ?");
        taskMapper.insert(disabledCron);

        // 启用的WEBHOOK任务 - 不应返回（非CRON类型）
        Task webhookTask = TestUtils.createTestTask("webhook-task", "echo webhook");
        webhookTask.setId(null);
        webhookTask.setTriggerType(TriggerType.WEBHOOK);
        webhookTask.setWebhookPath("/hook/test");
        taskMapper.insert(webhookTask);

        // 启用的CRON任务但无cron表达式 - 不应返回
        Task noExprCron = TestUtils.createTestTask("no-expr-cron", "echo noexpr");
        noExprCron.setId(null);
        noExprCron.setCronExpression(null);
        taskMapper.insert(noExprCron);

        // 执行查询
        List<Task> result = taskMapper.findEnabledCronTasks();

        // 验证只返回1个任务
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("enabled-cron", result.get(0).getName());
        assertTrue(result.get(0).getEnabled());
        assertEquals(TriggerType.CRON, result.get(0).getTriggerType());
        assertNotNull(result.get(0).getCronExpression());
    }

    /**
     * 测试根据webhook路径查找任务
     * 验证能通过路径找到正确的启用任务，不存在的路径返回null
     */
    @Test
    @DisplayName("findByWebhookPath - 根据webhook路径查找任务")
    void testFindByWebhookPath() {
        // 创建一个WEBHOOK任务
        Task webhookTask = TestUtils.createTestTask("webhook-task", "echo webhook");
        webhookTask.setId(null);
        webhookTask.setTriggerType(TriggerType.WEBHOOK);
        webhookTask.setWebhookPath("/api/webhook/my-task");
        webhookTask.setWebhookSecret("secret123");
        taskMapper.insert(webhookTask);

        // 创建一个CRON任务（用于验证不会被错误返回）
        Task cronTask = TestUtils.createTestTask("cron-task", "echo cron");
        cronTask.setId(null);
        taskMapper.insert(cronTask);

        // 执行查询 - 应该找到webhook任务
        Task found = taskMapper.findByWebhookPath("/api/webhook/my-task");
        assertNotNull(found);
        assertEquals("webhook-task", found.getName());
        assertEquals("/api/webhook/my-task", found.getWebhookPath());
        assertTrue(found.getEnabled());

        // 执行查询 - 不存在的路径应返回null
        Task notFound = taskMapper.findByWebhookPath("/api/webhook/non-existent");
        assertNull(notFound);
    }
}
