package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.Subtask;
import com.agentplatform.core.enums.SubtaskStatus;
import com.agentplatform.core.mapper.SubtaskMapper;
import com.agentplatform.core.service.SubtaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 子任务服务实现
 */
@Slf4j
@Service
public class SubtaskServiceImpl extends ServiceImpl<SubtaskMapper, Subtask>
        implements SubtaskService {

    @Override
    public List<Subtask> batchCreate(Long executionId, List<Subtask> subtasks) {
        for (Subtask subtask : subtasks) {
            subtask.setExecutionId(executionId);
            subtask.setStatus(SubtaskStatus.PENDING);
        }
        saveBatch(subtasks);
        log.info("批量创建子任务: executionId={}, count={}", executionId, subtasks.size());
        return subtasks;
    }

    @Override
    public List<Subtask> getByExecutionId(Long executionId) {
        return baseMapper.findByExecutionId(executionId);
    }

    @Override
    public void updateStatus(Long subtaskId, SubtaskStatus status, String output, String error) {
        Subtask subtask = getById(subtaskId);
        if (subtask == null) {
            log.warn("子任务不存在: id={}", subtaskId);
            return;
        }

        subtask.setStatus(status);
        if (output != null) subtask.setOutput(output);
        if (error != null) subtask.setError(error);

        if (status == SubtaskStatus.RUNNING) {
            subtask.setStartedAt(LocalDateTime.now());
        } else if (status == SubtaskStatus.COMPLETED || status == SubtaskStatus.FAILED || status == SubtaskStatus.SKIPPED) {
            subtask.setFinishedAt(LocalDateTime.now());
        }

        updateById(subtask);
        log.debug("更新子任务状态: id={}, status={}", subtaskId, status);
    }

    @Override
    public Map<String, Object> getStatsByExecutionId(Long executionId) {
        return baseMapper.countByExecutionId(executionId);
    }

    @Override
    public boolean hasIncompleteSubtasks(Long executionId) {
        return count(new LambdaQueryWrapper<Subtask>()
                .eq(Subtask::getExecutionId, executionId)
                .in(Subtask::getStatus, SubtaskStatus.PENDING, SubtaskStatus.RUNNING)) > 0;
    }

    @Override
    public Subtask getNextPending(Long executionId) {
        return getOne(new LambdaQueryWrapper<Subtask>()
                .eq(Subtask::getExecutionId, executionId)
                .eq(Subtask::getStatus, SubtaskStatus.PENDING)
                .orderByAsc(Subtask::getSeq)
                .last("LIMIT 1"));
    }

    @Override
    public void skipPendingAndRunning(Long executionId) {
        List<Subtask> incomplete = list(new LambdaQueryWrapper<Subtask>()
                .eq(Subtask::getExecutionId, executionId)
                .in(Subtask::getStatus, SubtaskStatus.PENDING, SubtaskStatus.RUNNING));

        for (Subtask subtask : incomplete) {
            subtask.setStatus(SubtaskStatus.SKIPPED);
            subtask.setFinishedAt(LocalDateTime.now());
        }
        updateBatchById(incomplete);
        log.info("跳过未完成子任务: executionId={}, count={}", executionId, incomplete.size());
    }
}
