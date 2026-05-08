package com.agentplatform.core.service.impl;

import com.agentplatform.core.entity.ExecutionLog;
import com.agentplatform.core.enums.LogType;
import com.agentplatform.core.mapper.ExecutionLogMapper;
import com.agentplatform.core.service.ExecutionLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 执行过程日志服务实现
 */
@Slf4j
@Service
public class ExecutionLogServiceImpl extends ServiceImpl<ExecutionLogMapper, ExecutionLog>
        implements ExecutionLogService {

    @Override
    public void appendLog(Long executionId, Long subtaskId, LogType logType, String content) {
        int nextSeq = baseMapper.getMaxSeq(executionId) + 1;
        ExecutionLog logEntry = ExecutionLog.builder()
                .executionId(executionId)
                .subtaskId(subtaskId)
                .logType(logType)
                .content(content)
                .seq(nextSeq)
                .build();
        save(logEntry);
    }

    @Override
    public List<ExecutionLog> getByExecutionId(Long executionId) {
        return baseMapper.findByExecutionId(executionId);
    }

    @Override
    public List<ExecutionLog> getByExecutionIdAndType(Long executionId, LogType logType) {
        return list(new LambdaQueryWrapper<ExecutionLog>()
                .eq(ExecutionLog::getExecutionId, executionId)
                .eq(ExecutionLog::getLogType, logType)
                .orderByAsc(ExecutionLog::getSeq));
    }

    @Override
    public List<ExecutionLog> getBySubtaskId(Long subtaskId) {
        return baseMapper.findBySubtaskId(subtaskId);
    }
}
