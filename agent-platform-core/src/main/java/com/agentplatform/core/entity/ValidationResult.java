package com.agentplatform.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public static ValidationResult success() {
        return ValidationResult.builder().valid(true).build();
    }

    public static ValidationResult failure(String error) {
        return ValidationResult.builder()
                .valid(false)
                .errors(List.of(error))
                .build();
    }

    public ValidationResult addError(String error) {
        this.errors.add(error);
        this.valid = false;
        return this;
    }

    public ValidationResult addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }
}
