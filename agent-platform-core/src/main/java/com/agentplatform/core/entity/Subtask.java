package com.agentplatform.core.entity;

import com.agentplatform.core.enums.SubtaskStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 子任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_subtask")
public class Subtask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private Integer seq;

    private String title;

    private String description;

    private SubtaskStatus status;

    private String output;

    private String error;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
