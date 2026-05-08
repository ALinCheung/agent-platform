package com.agentplatform.scheduler.service;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.executor.service.TaskExecutionQueue;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 调度服务
 * 管理Cron任务的注册、执行和持久化恢复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final TaskService taskService;
    private final TaskExecutionQueue taskExecutionQueue;
    private final TaskScheduler schedulerTaskScheduler;

    /** 已注册的调度任务 */
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 应用启动时恢复所有已启用的Cron任务
     */
    @PostConstruct
    public void init() {
        log.info("开始恢复Cron调度任务...");
        List<Task> cronTasks = taskService.findEnabledCronTasks();
        for (Task task : cronTasks) {
            try {
                registerTask(task);
                log.info("恢复Cron任务: id={}, name={}, cron={}", task.getId(), task.getName(), task.getCronExpression());
            } catch (Exception e) {
                log.error("恢复Cron任务失败: id={}, name={}", task.getId(), task.getName(), e);
            }
        }
        log.info("Cron调度任务恢复完成: 共{}个任务", cronTasks.size());
    }

    /**
     * 注册Cron任务
     */
    public void registerTask(Task task) {
        if (task.getCronExpression() == null || task.getCronExpression().isBlank()) {
            log.warn("任务缺少Cron表达式: taskId={}", task.getId());
            return;
        }

        // 如果已注册，先取消
        cancelTask(task.getId());

        // 转换为6字段格式（秒 分 时 日 月 周）
        String cronExpr = normalizeCronExpression(task.getCronExpression());
        CronTrigger trigger = new CronTrigger(cronExpr);
        ScheduledFuture<?> future = schedulerTaskScheduler.schedule(
                () -> executeScheduledTask(task),
                trigger
        );

        scheduledTasks.put(task.getId(), future);
        log.info("注册Cron任务: id={}, cron={}", task.getId(), cronExpr);
    }

    /**
     * 将5字段Cron表达式转换为6字段格式
     * 5字段格式: 分 时 日 月 周
     * 6字段格式: 秒 分 时 日 月 周
     */
    private String normalizeCronExpression(String cronExpression) {
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length == 5) {
            return "0 " + cronExpression;
        }
        return cronExpression;
    }

    /**
     * 取消任务调度
     */
    public void cancelTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            log.info("取消任务调度: id={}", taskId);
        }
    }

    /**
     * 重新注册任务（配置变更后）
     */
    public void reRegisterTask(Task task) {
        cancelTask(task.getId());
        if (task.getEnabled() && task.getTriggerType() == com.agentplatform.core.enums.TriggerType.CRON) {
            registerTask(task);
        }
    }

    /**
     * 获取已注册的任务数
     */
    public int getRegisteredCount() {
        return scheduledTasks.size();
    }

    /**
     * 检查任务是否已注册
     */
    public boolean isRegistered(Long taskId) {
        return scheduledTasks.containsKey(taskId);
    }

    private void executeScheduledTask(Task task) {
        try {
            log.info("调度触发执行: taskId={}, name={}", task.getId(), task.getName());
            taskExecutionQueue.submit(task);
        } catch (Exception e) {
            log.error("调度执行失败: taskId={}", task.getId(), e);
        }
    }
}
