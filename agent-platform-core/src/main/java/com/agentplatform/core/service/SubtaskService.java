package com.agentplatform.core.service;

import com.agentplatform.core.entity.Subtask;
import com.agentplatform.core.enums.SubtaskStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 子任务服务接口
 */
public interface SubtaskService extends IService<Subtask> {

    /**
     * 批量创建子任务
     */
    List<Subtask> batchCreate(Long executionId, List<Subtask> subtasks);

    /**
     * 按执行ID查询子任务列表
     */
    List<Subtask> getByExecutionId(Long executionId);

    /**
     * 更新子任务状态
     */
    void updateStatus(Long subtaskId, SubtaskStatus status, String output, String error);

    /**
     * 统计执行的子任务完成情况
     */
    Map<String, Object> getStatsByExecutionId(Long executionId);

    /**
     * 检查是否有未完成的子任务
     */
    boolean hasIncompleteSubtasks(Long executionId);

    /**
     * 获取第一个待执行的子任务
     */
    Subtask getNextPending(Long executionId);

    /**
     * 跳过执行中和待执行的子任务（用于终止场景）
     */
    void skipPendingAndRunning(Long executionId);
}
