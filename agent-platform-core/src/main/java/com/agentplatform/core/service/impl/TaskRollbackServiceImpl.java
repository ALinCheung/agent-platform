package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务回滚服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRollbackServiceImpl {

    private final TaskService taskService;
    private final TaskVersionService taskVersionService;

    /**
     * 回滚到指定版本
     * @param taskId 任务ID
     * @param version 目标版本号
     * @return 回滚后的任务
     */
    @Transactional
    public Task rollback(Long taskId, int version) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        TaskVersion targetVersion = taskVersionService.getVersion(taskId, version);
        if (targetVersion == null) {
            throw new RuntimeException("版本不存在: taskId=" + taskId + ", version=" + version);
        }

        // 保存当前版本（回滚前）
        taskVersionService.saveVersion(task, ChangeType.UPDATE, "回滚前保存");

        // 恢复目标版本的配置
        task.setCommand(targetVersion.getCommand());
        task.setCronExpression(targetVersion.getCronExpression());
        task.setWebhookPath(targetVersion.getWebhookPath());
        task.setWebhookSecret(targetVersion.getWebhookSecret());
        task.setTimeoutSeconds(targetVersion.getTimeoutSeconds());
        task.setMaxRetries(targetVersion.getMaxRetries());
        task.setRetryIntervalSeconds(targetVersion.getRetryIntervalSeconds());
        task.setWorkDir(targetVersion.getWorkDir());

        taskService.updateById(task);

        // 保存回滚版本记录
        taskVersionService.saveVersion(task, ChangeType.ROLLBACK, "回滚到版本 " + version);

        log.info("任务回滚成功: taskId={}, targetVersion={}", taskId, version);
        return task;
    }
}
