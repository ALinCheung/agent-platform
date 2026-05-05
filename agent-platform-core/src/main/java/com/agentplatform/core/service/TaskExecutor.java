package com.agentplatform.core.service;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Task;

/**
 * 任务执行器接口
 */
public interface TaskExecutor {

    /**
     * 执行任务
     * @param task 任务定义
     * @return 执行结果
     */
    ExecutionResult execute(Task task);

    /**
     * 检查执行器是否可用
     * @return 是否可用
     */
    boolean isAvailable();
}
