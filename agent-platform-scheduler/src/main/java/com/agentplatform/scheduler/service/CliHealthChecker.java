package com.agentplatform.scheduler.service;

import com.agentplatform.core.service.TaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Claude CLI健康检查器
 * 定期检测CLI可用性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CliHealthChecker {

    private final TaskExecutor taskExecutor;
    private final SchedulerService schedulerService;

    /** CLI是否可用 */
    private volatile boolean cliAvailable = false;

    /**
     * 启动时检测CLI
     */
    @Scheduled(fixedDelayString = "${app.claude.check-interval-minutes:5}60000")
    public void checkCliAvailability() {
        boolean available = taskExecutor.isAvailable();

        if (available && !cliAvailable) {
            log.info("Claude CLI变为可用");
            cliAvailable = true;
        } else if (!available && cliAvailable) {
            log.warn("Claude CLI变为不可用，暂停所有调度任务");
            cliAvailable = false;
        }
    }

    /**
     * CLI是否可用
     */
    public boolean isCliAvailable() {
        return cliAvailable;
    }
}
