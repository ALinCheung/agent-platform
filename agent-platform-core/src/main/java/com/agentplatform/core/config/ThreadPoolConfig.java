package com.agentplatform.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * scheduler-pool: 调度线程池，用于Cron任务调度
 * execution-pool: 执行线程池，用于任务执行
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 调度线程池
     * 核心线程数=2，最大线程数=4，用于Cron调度任务
     */
    @Bean("scheduler-pool")
    public ThreadPoolTaskScheduler schedulerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduler-pool-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setErrorHandler(throwable -> {
            log.error("调度线程池发生异常: {}", throwable.getMessage(), throwable);
        });
        scheduler.initialize();
        log.info("调度线程池已初始化: coreSize=4");
        return scheduler;
    }

    /**
     * 执行线程池
     * 核心线程数=5，最大线程数=10，队列容量=100
     * 拒绝策略：CallerRunsPolicy（队列满时由调用线程执行）
     */
    @Bean("execution-pool")
    public Executor executionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("execution-pool-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        log.info("执行线程池已初始化: coreSize=5, maxSize=10, queueCapacity=100");
        return executor;
    }
}
