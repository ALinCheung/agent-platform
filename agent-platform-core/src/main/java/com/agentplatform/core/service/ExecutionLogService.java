package com.agentplatform.core.service;

import com.agentplatform.core.entity.ExecutionLog;
import com.agentplatform.core.enums.LogType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 执行过程日志服务接口
 */
public interface ExecutionLogService extends IService<ExecutionLog> {

    /**
     * 追加日志
     */
    void appendLog(Long executionId, Long subtaskId, LogType logType, String content);

    /**
     * 按执行ID查询日志
     */
    List<ExecutionLog> getByExecutionId(Long executionId);

    /**
     * 按执行ID和日志类型查询
     */
    List<ExecutionLog> getByExecutionIdAndType(Long executionId, LogType logType);

    /**
     * 按子任务ID查询日志
     */
    List<ExecutionLog> getBySubtaskId(Long subtaskId);
}
