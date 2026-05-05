package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.mapper.TaskVersionMapper;
import com.agentplatform.core.service.TaskVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务版本服务实现
 */
@Slf4j
@Service
public class TaskVersionServiceImpl extends ServiceImpl<TaskVersionMapper, TaskVersion> implements TaskVersionService {

    @Override
    public void saveVersion(Task task, ChangeType changeType, String description) {
        int nextVersion = baseMapper.getMaxVersion(task.getId()) + 1;

        TaskVersion version = TaskVersion.builder()
                .taskId(task.getId())
                .version(nextVersion)
                .command(task.getCommand())
                .cronExpression(task.getCronExpression())
                .webhookPath(task.getWebhookPath())
                .webhookSecret(task.getWebhookSecret())
                .timeoutSeconds(task.getTimeoutSeconds())
                .maxRetries(task.getMaxRetries())
                .retryIntervalSeconds(task.getRetryIntervalSeconds())
                .workDir(task.getWorkDir())
                .changeType(changeType)
                .changeDescription(description)
                .createdBy("system")
                .build();

        save(version);
        log.info("保存任务版本: taskId={}, version={}, changeType={}", task.getId(), nextVersion, changeType);
    }

    @Override
    public List<TaskVersion> getVersions(Long taskId) {
        return list(new LambdaQueryWrapper<TaskVersion>()
                .eq(TaskVersion::getTaskId, taskId)
                .orderByDesc(TaskVersion::getVersion));
    }

    @Override
    public TaskVersion getVersion(Long taskId, int version) {
        return getOne(new LambdaQueryWrapper<TaskVersion>()
                .eq(TaskVersion::getTaskId, taskId)
                .eq(TaskVersion::getVersion, version));
    }

    @Override
    public int getMaxVersion(Long taskId) {
        return baseMapper.getMaxVersion(taskId);
    }
}
