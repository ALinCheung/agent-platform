package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.mapper.TaskMapper;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final TaskVersionService taskVersionService;

    @Override
    @Transactional
    public Task createTask(Task task) {
        // 设置默认值
        if (task.getEnabled() == null) {
            task.setEnabled(true);
        }
        if (task.getTimeoutSeconds() == null) {
            task.setTimeoutSeconds(300);
        }
        if (task.getMaxRetries() == null) {
            task.setMaxRetries(0);
        }
        if (task.getRetryIntervalSeconds() == null) {
            task.setRetryIntervalSeconds(60);
        }
        if (task.getSuccessCount() == null) {
            task.setSuccessCount(0);
        }
        if (task.getFailureCount() == null) {
            task.setFailureCount(0);
        }

        save(task);
        log.info("创建任务: id={}, name={}", task.getId(), task.getName());

        // 保存初始版本
        taskVersionService.saveVersion(task, ChangeType.CREATE, "初始创建");

        return task;
    }

    @Override
    @Transactional
    public Task updateTask(Long id, Task task) {
        Task existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("任务不存在: " + id);
        }

        // 保存旧版本
        taskVersionService.saveVersion(existing, ChangeType.UPDATE, "更新任务配置");

        // 更新字段
        existing.setName(task.getName());
        existing.setDescription(task.getDescription());
        existing.setCommand(task.getCommand());
        existing.setTriggerType(task.getTriggerType());
        existing.setCronExpression(task.getCronExpression());
        existing.setWebhookPath(task.getWebhookPath());
        existing.setWebhookSecret(task.getWebhookSecret());
        existing.setTimeoutSeconds(task.getTimeoutSeconds());
        existing.setMaxRetries(task.getMaxRetries());
        existing.setRetryIntervalSeconds(task.getRetryIntervalSeconds());
        existing.setWorkDir(task.getWorkDir());

        updateById(existing);
        log.info("更新任务: id={}, name={}", id, existing.getName());

        return existing;
    }

    @Override
    @Transactional
    public void enableTask(Long id) {
        Task task = getById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        task.setEnabled(true);
        updateById(task);
        log.info("启用任务: id={}", id);
    }

    @Override
    @Transactional
    public void disableTask(Long id) {
        Task task = getById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        task.setEnabled(false);
        updateById(task);
        log.info("禁用任务: id={}", id);
    }

    @Override
    public List<Task> findEnabledCronTasks() {
        return baseMapper.findEnabledCronTasks();
    }

    @Override
    public Task findByWebhookPath(String path) {
        return baseMapper.findByWebhookPath(path);
    }

    @Override
    public List<Task> listAll() {
        return list(new LambdaQueryWrapper<Task>().orderByDesc(Task::getCreatedAt));
    }
}
