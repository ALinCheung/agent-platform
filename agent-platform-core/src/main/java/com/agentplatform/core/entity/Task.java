package com.agentplatform.core.entity;

import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.TriggerType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务定义实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String command;

    private TriggerType triggerType;

    private String cronExpression;

    private String webhookPath;

    private String webhookSecret;

    private Integer timeoutSeconds;

    private Integer maxRetries;

    private Integer retryIntervalSeconds;

    private String workDir;

    private Boolean enabled;

    private ExecutionStatus lastExecutionStatus;

    private LocalDateTime lastExecutionAt;

    private Integer successCount;

    private Integer failureCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
