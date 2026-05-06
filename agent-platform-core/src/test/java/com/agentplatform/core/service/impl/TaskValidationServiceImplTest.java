package com.agentplatform.core.service.impl;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.ValidationResult;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskValidationServiceImpl 单元测试
 * 测试任务验证器的各种校验规则
 */
@DisplayName("任务验证服务实现测试")
class TaskValidationServiceImplTest extends BaseTest {

    @Autowired
    private TaskValidator taskValidator;

    @Autowired
    private TaskService taskService;

    /**
     * 构建一个有效的CRON任务，用于需要基本有效性的测试场景
     */
    private Task buildValidCronTask() {
        return Task.builder()
                .name(TestUtils.generateRandomTaskName())
                .command("echo valid")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0 * * * ?")
                .timeoutSeconds(300)
                .maxRetries(3)
                .retryIntervalSeconds(60)
                .build();
    }

    @Test
    @DisplayName("有效任务 - 验证通过，无错误")
    void validate_validTask_returnsValidWithNoErrors() {
        Task task = buildValidCronTask();

        ValidationResult result = taskValidator.validate(task);

        assertTrue(result.isValid(), "有效任务应通过验证");
        assertTrue(result.getErrors().isEmpty(), "不应有错误信息");
    }

    @Test
    @DisplayName("空任务名称 - 返回错误'任务名称不能为空'")
    void validate_emptyName_returnsError() {
        Task task = buildValidCronTask();
        task.setName("");

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("任务名称不能为空")),
                "应包含'任务名称不能为空'错误");
    }

    @Test
    @DisplayName("null任务名称 - 返回错误'任务名称不能为空'")
    void validate_nullName_returnsError() {
        Task task = buildValidCronTask();
        task.setName(null);

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("任务名称不能为空")),
                "应包含'任务名称不能为空'错误");
    }

    @Test
    @DisplayName("空命令 - 返回错误'命令不能为空'")
    void validate_emptyCommand_returnsError() {
        Task task = buildValidCronTask();
        task.setCommand("");

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("命令不能为空")),
                "应包含'命令不能为空'错误");
    }

    @Test
    @DisplayName("null触发类型 - 返回错误'触发类型不能为空'")
    void validate_nullTriggerType_returnsError() {
        Task task = buildValidCronTask();
        task.setTriggerType(null);

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("触发类型不能为空")),
                "应包含'触发类型不能为空'错误");
    }

    @Test
    @DisplayName("CRON触发但无cronExpression - 返回错误")
    void validate_cronTriggerWithoutExpression_returnsError() {
        Task task = buildValidCronTask();
        task.setCronExpression(null);

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Cron任务必须配置Cron表达式")),
                "应包含Cron表达式缺失的错误");
    }

    @Test
    @DisplayName("CRON触发但cronExpression格式无效 - 返回错误'Cron表达式格式错误'")
    void validate_cronTriggerWithInvalidExpression_returnsError() {
        Task task = buildValidCronTask();
        task.setCronExpression("invalid cron expression");

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Cron表达式格式错误")),
                "应包含'Cron表达式格式错误'");
    }

    @Test
    @DisplayName("WEBHOOK触发但无webhookPath - 返回错误")
    void validate_webhookTriggerWithoutPath_returnsError() {
        Task task = Task.builder()
                .name(TestUtils.generateRandomTaskName())
                .command("echo webhook")
                .triggerType(TriggerType.WEBHOOK)
                .webhookPath(null)
                .timeoutSeconds(300)
                .maxRetries(0)
                .retryIntervalSeconds(60)
                .build();

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Webhook任务必须配置Webhook路径")),
                "应包含Webhook路径缺失的错误");
    }

    @Test
    @DisplayName("重复任务名称 - 返回错误'任务名称已存在'")
    void validate_duplicateName_returnsError() {
        // 先在数据库中创建一个任务
        Task existing = TestUtils.createTestTask("重复名称任务", "echo existing");
        taskService.createTask(existing);

        // 尝试验证同名任务（ID不同）
        Task duplicate = buildValidCronTask();
        duplicate.setName("重复名称任务");

        ValidationResult result = taskValidator.validate(duplicate);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("任务名称已存在")),
                "应包含'任务名称已存在'错误");
    }

    @Test
    @DisplayName("超时时间超出范围(60-3600) - 返回错误")
    void validate_timeoutOutOfRange_returnsError() {
        // 测试超时时间过小
        Task taskTooLow = buildValidCronTask();
        taskTooLow.setTimeoutSeconds(30);

        ValidationResult resultLow = taskValidator.validate(taskTooLow);
        assertFalse(resultLow.isValid());
        assertTrue(resultLow.getErrors().stream()
                .anyMatch(e -> e.contains("超时时间必须在60-3600秒之间")),
                "超时时间30秒应返回范围错误");

        // 测试超时时间过大
        Task taskTooHigh = buildValidCronTask();
        taskTooHigh.setTimeoutSeconds(7200);

        ValidationResult resultHigh = taskValidator.validate(taskTooHigh);
        assertFalse(resultHigh.isValid());
        assertTrue(resultHigh.getErrors().stream()
                .anyMatch(e -> e.contains("超时时间必须在60-3600秒之间")),
                "超时时间7200秒应返回范围错误");
    }

    @Test
    @DisplayName("最大重试次数超出范围(0-10) - 返回错误")
    void validate_maxRetriesOutOfRange_returnsError() {
        // 测试重试次数过大
        Task task = buildValidCronTask();
        task.setMaxRetries(15);

        ValidationResult result = taskValidator.validate(task);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("最大重试次数必须在0-10之间")),
                "最大重试次数15应返回范围错误");
    }

    @Test
    @DisplayName("重试间隔超出范围(10-3600) - 返回错误")
    void validate_retryIntervalOutOfRange_returnsError() {
        // 测试间隔过小
        Task taskTooLow = buildValidCronTask();
        taskTooLow.setRetryIntervalSeconds(5);

        ValidationResult resultLow = taskValidator.validate(taskTooLow);
        assertFalse(resultLow.isValid());
        assertTrue(resultLow.getErrors().stream()
                .anyMatch(e -> e.contains("重试间隔必须在10-3600秒之间")),
                "重试间隔5秒应返回范围错误");

        // 测试间隔过大
        Task taskTooHigh = buildValidCronTask();
        taskTooHigh.setRetryIntervalSeconds(7200);

        ValidationResult resultHigh = taskValidator.validate(taskTooHigh);
        assertFalse(resultHigh.isValid());
        assertTrue(resultHigh.getErrors().stream()
                .anyMatch(e -> e.contains("重试间隔必须在10-3600秒之间")),
                "重试间隔7200秒应返回范围错误");
    }
}
