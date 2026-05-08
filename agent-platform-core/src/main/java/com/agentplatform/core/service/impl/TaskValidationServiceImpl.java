package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.ValidationResult;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

/**
 * 任务验证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskValidationServiceImpl implements TaskValidator {

    private final TaskService taskService;

    @Override
    public ValidationResult validate(Task task) {
        ValidationResult result = ValidationResult.success();

        // 验证必填字段
        if (task.getName() == null || task.getName().isBlank()) {
            result.addError("任务名称不能为空");
        }
        if (task.getCommand() == null || task.getCommand().isBlank()) {
            result.addError("命令不能为空");
        }
        if (task.getTriggerType() == null) {
            result.addError("触发类型不能为空");
        }

        // 验证名称唯一性
        if (task.getName() != null) {
            Task existing = taskService.getOne(
                    new LambdaQueryWrapper<Task>().eq(Task::getName, task.getName()));
            if (existing != null && !existing.getId().equals(task.getId())) {
                result.addError("任务名称已存在: " + task.getName());
            }
        }

        // 验证Cron表达式
        if (task.getTriggerType() == TriggerType.CRON) {
            if (task.getCronExpression() == null || task.getCronExpression().isBlank()) {
                result.addError("Cron任务必须配置Cron表达式");
            } else if (!isValidCron(task.getCronExpression())) {
                result.addError("Cron表达式格式错误: " + task.getCronExpression());
            }
        }

        // 验证Webhook路径唯一性
        if (task.getTriggerType() == TriggerType.WEBHOOK) {
            if (task.getWebhookPath() == null || task.getWebhookPath().isBlank()) {
                result.addError("Webhook任务必须配置Webhook路径");
            } else {
                Task existing = taskService.findByWebhookPath(task.getWebhookPath());
                if (existing != null && !existing.getId().equals(task.getId())) {
                    result.addError("Webhook路径已被使用: " + task.getWebhookPath());
                }
            }
        }

        // 验证超时配置
        if (task.getTimeoutSeconds() != null) {
            if (task.getTimeoutSeconds() < 60 || task.getTimeoutSeconds() > 3600) {
                result.addError("超时时间必须在60-3600秒之间");
            }
        }

        // 验证重试配置
        if (task.getMaxRetries() != null) {
            if (task.getMaxRetries() < 0 || task.getMaxRetries() > 10) {
                result.addError("最大重试次数必须在0-10之间");
            }
        }
        if (task.getRetryIntervalSeconds() != null) {
            if (task.getRetryIntervalSeconds() < 10 || task.getRetryIntervalSeconds() > 3600) {
                result.addError("重试间隔必须在10-3600秒之间");
            }
        }

        return result;
    }

    private boolean isValidCron(String cronExpression) {
        try {
            String[] parts = cronExpression.trim().split("\\s+");
            String exprToValidate;
            if (parts.length == 5) {
                // 5字段格式: 分 时 日 月 周 -> 转换为6字段: 秒 分 时 日 月 周
                exprToValidate = "0 " + cronExpression;
            } else if (parts.length == 6) {
                exprToValidate = cronExpression;
            } else {
                return false;
            }
            CronExpression.parse(exprToValidate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
