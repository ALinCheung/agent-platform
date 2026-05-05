package com.agentplatform.core.service.impl;

import com.agentplatform.core.mapper.TaskExecutionMapper;
import com.agentplatform.core.mapper.TaskMapper;
import com.agentplatform.core.service.StatisticsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计收集器实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsCollectorImpl implements StatisticsCollector {

    private final TaskMapper taskMapper;
    private final TaskExecutionMapper taskExecutionMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 任务统计
        Map<String, Object> taskStats = taskMapper.countByStatus();
        overview.put("tasks", taskStats);

        // 今日执行统计
        Map<String, Object> todayStats = taskExecutionMapper.countToday();
        overview.put("today", todayStats);

        return overview;
    }

    @Override
    public Map<String, Object> getTaskStats() {
        Map<String, Object> stats = new HashMap<>();

        // 任务状态统计
        stats.put("status", taskMapper.countByStatus());

        // 触发类型统计
        stats.put("triggerType", taskMapper.countByTriggerType());

        return stats;
    }

    @Override
    public Map<String, Object> getExecutionStats() {
        Map<String, Object> stats = new HashMap<>();

        // 今日统计
        stats.put("today", taskExecutionMapper.countToday());

        // 按状态统计
        stats.put("byStatus", taskExecutionMapper.countByStatus());

        return stats;
    }

    @Override
    public List<Map<String, Object>> getHistoryTrend(int days) {
        return taskExecutionMapper.countByDays(days);
    }

    @Override
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();

        // 耗时统计
        stats.put("duration", taskExecutionMapper.getDurationStats());

        // 失败任务TOP榜
        stats.put("topFailed", taskExecutionMapper.topFailedTasks(10));

        // 慢任务TOP榜
        stats.put("topSlow", taskExecutionMapper.topSlowTasks(10));

        return stats;
    }
}
