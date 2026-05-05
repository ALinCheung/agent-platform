package com.agentplatform.scheduler.service;

import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 自动清理服务
 * 定期清理过期的执行记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {

    private final ExecutionHistoryService executionHistoryService;

    @Value("${app.scheduler.cleanup-days:30}")
    private int cleanupDays;

    /**
     * 每天凌晨2点执行清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredExecutions() {
        log.info("开始清理过期执行记录: 保留{}天", cleanupDays);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupDays);

        int deleted = executionHistoryService.remove(
                new LambdaQueryWrapper<TaskExecution>()
                        .lt(TaskExecution::getStartedAt, cutoff)
        );

        log.info("清理完成: 删除{}条过期记录", deleted);
    }
}
