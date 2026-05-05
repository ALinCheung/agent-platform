package com.agentplatform.core.mapper;

import com.agentplatform.core.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 任务数据访问层
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 按触发类型统计任务数量
     */
    @Select("SELECT trigger_type as triggerType, COUNT(*) as count FROM task GROUP BY trigger_type")
    List<Map<String, Object>> countByTriggerType();

    /**
     * 按状态统计任务数量
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN enabled = 1 THEN 1 ELSE 0 END) as enabled, " +
            "SUM(CASE WHEN enabled = 0 THEN 1 ELSE 0 END) as disabled " +
            "FROM task")
    Map<String, Object> countByStatus();

    /**
     * 查找所有启用的CRON任务
     */
    @Select("SELECT * FROM task WHERE enabled = 1 AND trigger_type = 'CRON' AND cron_expression IS NOT NULL")
    List<Task> findEnabledCronTasks();

    /**
     * 根据webhook路径查找任务
     */
    @Select("SELECT * FROM task WHERE webhook_path = #{path} AND enabled = 1")
    Task findByWebhookPath(@Param("path") String path);
}
