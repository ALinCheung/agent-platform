package com.agentplatform.web.controller;

import com.agentplatform.core.service.StatisticsCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计控制器
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatisticsCollector statisticsCollector;

    /**
     * 总览统计
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(statisticsCollector.getOverview());
    }

    /**
     * 任务统计
     */
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getTaskStats() {
        return ResponseEntity.ok(statisticsCollector.getTaskStats());
    }

    /**
     * 执行统计
     */
    @GetMapping("/executions")
    public ResponseEntity<Map<String, Object>> getExecutionStats() {
        return ResponseEntity.ok(statisticsCollector.getExecutionStats());
    }

    /**
     * 历史趋势
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statisticsCollector.getHistoryTrend(days));
    }

    /**
     * 性能统计
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformance() {
        return ResponseEntity.ok(statisticsCollector.getPerformanceStats());
    }
}
