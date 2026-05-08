package com.agentplatform.core.entity;

import com.agentplatform.core.enums.LogType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行过程日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_execution_log")
public class ExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private Long subtaskId;

    private LogType logType;

    private String content;

    private Integer seq;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
