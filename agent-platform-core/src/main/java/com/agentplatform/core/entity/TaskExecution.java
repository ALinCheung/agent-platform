package com.agentplatform.core.entity;

import com.agentplatform.core.enums.ExecutionStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_execution")
public class TaskExecution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long taskVersionId;

    private ExecutionStatus status;

    private Integer retryCount;

    private Long parentExecutionId;

    private String output;

    private String error;

    private Integer exitCode;

    private String executionDir;

    private Long durationMs;

    private Integer memoryUsedMb;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    // ===== 非数据库字段，用于服务层传递信息 =====

    /** 关联的任务对象（查询时填充） */
    @TableField(exist = false)
    private Task task;

    /** 关联的任务版本对象（查询时填充） */
    @TableField(exist = false)
    private TaskVersion taskVersion;
}
