package com.agentplatform.core.entity;

import com.agentplatform.core.enums.ChangeType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务配置版本实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_version")
public class TaskVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer version;

    private String command;

    private String cronExpression;

    private String webhookPath;

    private String webhookSecret;

    private Integer timeoutSeconds;

    private Integer maxRetries;

    private Integer retryIntervalSeconds;

    private String workDir;

    private ChangeType changeType;

    private String changeDescription;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private String createdBy;
}
