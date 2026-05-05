package com.agentplatform.core.entity;

import com.agentplatform.core.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    private ExecutionStatus status;
    private Integer exitCode;
    private String output;
    private String error;
    private Long durationMs;
    private Integer memoryUsedMb;

    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }
}
