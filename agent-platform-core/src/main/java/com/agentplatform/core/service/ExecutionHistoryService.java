package com.agentplatform.core.service;

import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 执行历史服务接口
 */
public interface ExecutionHistoryService extends IService<TaskExecution> {

    /**
     * 创建执行记录
     */
    TaskExecution createExecution(Long taskId, Long taskVersionId);

    /**
     * 更新执行结果
     */
    void updateExecutionResult(Long executionId, ExecutionStatus status, String output, String error,
                               Integer exitCode, Long durationMs, Integer memoryUsedMb);

    /**
     * 按任务查询执行历史
     */
    List<TaskExecution> getByTaskId(Long taskId);

    /**
     * 获取今日执行统计
     */
    Map<String, Object> getTodayStats();

    /**
     * 获取最近N天统计
     */
    List<Map<String, Object>> getStatsByDays(int days);

    /**
     * 获取耗时统计
     */
    Map<String, Object> getDurationStats();

    /**
     * 失败任务TOP榜
     */
    List<Map<String, Object>> topFailedTasks(int limit);

    /**
     * 慢任务TOP榜
     */
    List<Map<String, Object>> topSlowTasks(int limit);
}
