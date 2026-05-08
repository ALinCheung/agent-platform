package com.agentplatform.core.mapper;

import com.agentplatform.core.entity.Subtask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 子任务数据访问层
 */
@Mapper
public interface SubtaskMapper extends BaseMapper<Subtask> {

    /**
     * 按执行ID查询子任务列表（按序号排序）
     */
    @Select("SELECT * FROM task_subtask WHERE execution_id = #{executionId} ORDER BY seq")
    List<Subtask> findByExecutionId(@Param("executionId") Long executionId);

    /**
     * 统计执行的子任务完成情况
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
            "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending, " +
            "SUM(CASE WHEN status = 'RUNNING' THEN 1 ELSE 0 END) as running " +
            "FROM task_subtask WHERE execution_id = #{executionId}")
    java.util.Map<String, Object> countByExecutionId(@Param("executionId") Long executionId);
}
