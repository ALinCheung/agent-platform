package com.agentplatform.core.service;

import java.util.List;
import java.util.Map;

/**
 * 统计收集器接口
 */
public interface StatisticsCollector {

    /**
     * 获取总览统计
     */
    Map<String, Object> getOverview();

    /**
     * 获取任务分类统计
     */
    Map<String, Object> getTaskStats();

    /**
     * 获取执行统计
     */
    Map<String, Object> getExecutionStats();

    /**
     * 获取历史趋势数据
     * @param days 天数
     */
    List<Map<String, Object>> getHistoryTrend(int days);

    /**
     * 获取性能统计
     */
    Map<String, Object> getPerformanceStats();
}
