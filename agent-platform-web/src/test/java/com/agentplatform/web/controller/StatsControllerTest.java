package com.agentplatform.web.controller;

import com.agentplatform.core.service.StatisticsCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 统计控制器单元测试
 * 测试总览、任务统计、执行统计、历史趋势、性能统计等功能
 */
@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatisticsCollector statisticsCollector;

    /**
     * 测试获取总览统计返回200
     */
    @Test
    @DisplayName("获取总览统计返回200")
    void getOverview_returns200() throws Exception {
        Map<String, Object> overview = Map.of(
                "totalTasks", 10,
                "activeTasks", 5,
                "totalExecutions", 100,
                "successRate", 95.5
        );
        when(statisticsCollector.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(10))
                .andExpect(jsonPath("$.activeTasks").value(5))
                .andExpect(jsonPath("$.successRate").value(95.5));

        verify(statisticsCollector).getOverview();
    }

    /**
     * 测试获取任务统计返回200
     */
    @Test
    @DisplayName("获取任务统计返回200")
    void getTaskStats_returns200() throws Exception {
        Map<String, Object> taskStats = Map.of(
                "cronTasks", 3,
                "webhookTasks", 2,
                "enabledTasks", 4,
                "disabledTasks", 1
        );
        when(statisticsCollector.getTaskStats()).thenReturn(taskStats);

        mockMvc.perform(get("/api/stats/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronTasks").value(3))
                .andExpect(jsonPath("$.webhookTasks").value(2));

        verify(statisticsCollector).getTaskStats();
    }

    /**
     * 测试获取执行统计返回200
     */
    @Test
    @DisplayName("获取执行统计返回200")
    void getExecutionStats_returns200() throws Exception {
        Map<String, Object> execStats = Map.of(
                "todayTotal", 20,
                "todaySuccess", 18,
                "todayFailed", 2,
                "avgDurationMs", 5000
        );
        when(statisticsCollector.getExecutionStats()).thenReturn(execStats);

        mockMvc.perform(get("/api/stats/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayTotal").value(20))
                .andExpect(jsonPath("$.todaySuccess").value(18));

        verify(statisticsCollector).getExecutionStats();
    }

    /**
     * 测试获取历史趋势默认7天
     */
    @Test
    @DisplayName("获取历史趋势默认天数为7")
    void getHistory_returns200_withDefaultDays7() throws Exception {
        List<Map<String, Object>> history = List.of(
                Map.of("date", "2026-04-30", "total", 10, "success", 9),
                Map.of("date", "2026-05-01", "total", 12, "success", 11)
        );
        when(statisticsCollector.getHistoryTrend(7)).thenReturn(history);

        mockMvc.perform(get("/api/stats/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].date").value("2026-04-30"))
                .andExpect(jsonPath("$[1].total").value(12));

        verify(statisticsCollector).getHistoryTrend(7);
    }

    /**
     * 测试获取性能统计返回200
     */
    @Test
    @DisplayName("获取性能统计返回200")
    void getPerformance_returns200() throws Exception {
        Map<String, Object> perf = Map.of(
                "avgDurationMs", 3000,
                "p95DurationMs", 8000,
                "p99DurationMs", 15000,
                "maxConcurrency", 10
        );
        when(statisticsCollector.getPerformanceStats()).thenReturn(perf);

        mockMvc.perform(get("/api/stats/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgDurationMs").value(3000))
                .andExpect(jsonPath("$.p95DurationMs").value(8000));

        verify(statisticsCollector).getPerformanceStats();
    }
}
