package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.mapper.TaskExecutionMapper;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 执行历史服务实现
 */
@Slf4j
@Service
public class ExecutionHistoryServiceImpl extends ServiceImpl<TaskExecutionMapper, TaskExecution>
        implements ExecutionHistoryService {

    @Override
    public TaskExecution createExecution(Long taskId, Long taskVersionId) {
        TaskExecution execution = TaskExecution.builder()
                .taskId(taskId)
                .taskVersionId(taskVersionId)
                .status(ExecutionStatus.RUNNING)
                .retryCount(0)
                .startedAt(LocalDateTime.now())
                .build();
        save(execution);
        log.info("创建执行记录: id={}, taskId={}", execution.getId(), taskId);
        return execution;
    }

    @Override
    public void updateExecutionResult(Long executionId, ExecutionStatus status, String output, String error,
                                      Integer exitCode, Long durationMs, Integer memoryUsedMb) {
        TaskExecution execution = getById(executionId);
        if (execution == null) {
            log.warn("执行记录不存在: id={}", executionId);
            return;
        }

        execution.setStatus(status);
        execution.setOutput(output);
        execution.setError(error);
        execution.setExitCode(exitCode);
        execution.setDurationMs(durationMs);
        execution.setMemoryUsedMb(memoryUsedMb);
        execution.setFinishedAt(LocalDateTime.now());

        updateById(execution);
        log.info("更新执行结果: id={}, status={}", executionId, status);
    }

    @Override
    public List<TaskExecution> getByTaskId(Long taskId) {
        return list(new LambdaQueryWrapper<TaskExecution>()
                .eq(TaskExecution::getTaskId, taskId)
                .orderByDesc(TaskExecution::getStartedAt));
    }

    @Override
    public Map<String, Object> getTodayStats() {
        return baseMapper.countToday();
    }

    @Override
    public List<Map<String, Object>> getStatsByDays(int days) {
        return baseMapper.countByDays(days);
    }

    @Override
    public Map<String, Object> getDurationStats() {
        return baseMapper.getDurationStats();
    }

    @Override
    public List<Map<String, Object>> topFailedTasks(int limit) {
        return baseMapper.topFailedTasks(limit);
    }

    @Override
    public List<Map<String, Object>> topSlowTasks(int limit) {
        return baseMapper.topSlowTasks(limit);
    }
}
