package com.agentplatform.core.mapper;

import com.agentplatform.core.entity.ExecutionLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 执行过程日志数据访问层
 */
@Mapper
public interface ExecutionLogMapper extends BaseMapper<ExecutionLog> {

    /**
     * 按执行ID查询日志（按序号排序）
     */
    @Select("SELECT * FROM task_execution_log WHERE execution_id = #{executionId} ORDER BY seq")
    List<ExecutionLog> findByExecutionId(@Param("executionId") Long executionId);

    /**
     * 按子任务ID查询日志（按序号排序）
     */
    @Select("SELECT * FROM task_execution_log WHERE subtask_id = #{subtaskId} ORDER BY seq")
    List<ExecutionLog> findBySubtaskId(@Param("subtaskId") Long subtaskId);

    /**
     * 获取当前最大序号
     */
    @Select("SELECT COALESCE(MAX(seq), 0) FROM task_execution_log WHERE execution_id = #{executionId}")
    int getMaxSeq(@Param("executionId") Long executionId);
}
