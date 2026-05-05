package com.agentplatform.core.mapper;

import com.agentplatform.core.entity.TaskExecution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 任务执行记录数据访问层
 */
@Mapper
public interface TaskExecutionMapper extends BaseMapper<TaskExecution> {

    /**
     * 按状态统计执行次数
     */
    @Select("SELECT status, COUNT(*) as count FROM task_execution GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 统计今日执行次数
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
            "SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) as timeout " +
            "FROM task_execution WHERE DATE(started_at) = DATE('now', 'localtime')")
    Map<String, Object> countToday();

    /**
     * 最近N天每天的执行统计
     */
    @Select("SELECT DATE(started_at) as date, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM task_execution " +
            "WHERE started_at >= datetime('now', '-' || #{days} || ' days', 'localtime') " +
            "GROUP BY DATE(started_at) " +
            "ORDER BY date")
    List<Map<String, Object>> countByDays(@Param("days") int days);

    /**
     * 获取执行耗时统计
     */
    @Select("SELECT " +
            "AVG(duration_ms) as avgDuration, " +
            "MAX(duration_ms) as maxDuration, " +
            "MIN(duration_ms) as minDuration " +
            "FROM task_execution WHERE status = 'SUCCESS' AND duration_ms IS NOT NULL")
    Map<String, Object> getDurationStats();

    /**
     * 失败次数最多的任务TOP榜
     */
    @Select("SELECT t.name, COUNT(*) as failureCount " +
            "FROM task_execution te " +
            "JOIN task t ON te.task_id = t.id " +
            "WHERE te.status IN ('FAILED', 'TIMEOUT') " +
            "GROUP BY te.task_id " +
            "ORDER BY failureCount DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> topFailedTasks(@Param("limit") int limit);

    /**
     * 平均耗时最长的任务TOP榜
     */
    @Select("SELECT t.name, AVG(te.duration_ms) as avgDuration " +
            "FROM task_execution te " +
            "JOIN task t ON te.task_id = t.id " +
            "WHERE te.status = 'SUCCESS' AND te.duration_ms IS NOT NULL " +
            "GROUP BY te.task_id " +
            "ORDER BY avgDuration DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> topSlowTasks(@Param("limit") int limit);
}
