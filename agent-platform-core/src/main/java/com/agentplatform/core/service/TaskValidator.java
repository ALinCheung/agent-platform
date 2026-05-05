package com.agentplatform.core.service;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.ValidationResult;

/**
 * 任务验证器接口
 */
public interface TaskValidator {

    /**
     * 验证任务配置
     * @param task 任务定义
     * @return 验证结果
     */
    ValidationResult validate(Task task);
}
