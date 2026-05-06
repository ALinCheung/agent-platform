-- AI自动化平台 - 统计视图（H2兼容版本）
-- 注意：此文件为测试环境专用，使用H2兼容SQL语法
-- 生产环境使用SQLite语法的 V2__add_statistics_views.sql

-- 任务统计视图
CREATE OR REPLACE VIEW v_task_stats AS
SELECT
    t.id,
    t.name,
    t.trigger_type,
    t.enabled,
    t.success_count,
    t.failure_count,
    t.last_execution_status,
    t.last_execution_at,
    (t.success_count + t.failure_count) as total_executions,
    CASE WHEN (t.success_count + t.failure_count) > 0
         THEN ROUND(t.success_count * 100.0 / (t.success_count + t.failure_count), 1)
         ELSE 0 END as success_rate
FROM task t;

-- 每日执行统计视图
CREATE OR REPLACE VIEW v_execution_stats AS
SELECT
    CAST(started_at AS DATE) as exec_date,
    COUNT(*) as total,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
    SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) as timeout,
    SUM(CASE WHEN status = 'RETRYING' THEN 1 ELSE 0 END) as retrying,
    AVG(duration_ms) as avg_duration
FROM task_execution
GROUP BY CAST(started_at AS DATE);

-- 系统总览视图
CREATE OR REPLACE VIEW v_system_overview AS
SELECT
    (SELECT COUNT(*) FROM task) as total_tasks,
    (SELECT COUNT(*) FROM task WHERE enabled = 1) as enabled_tasks,
    (SELECT COUNT(*) FROM task WHERE enabled = 0) as disabled_tasks,
    (SELECT COUNT(*) FROM task WHERE trigger_type = 'CRON') as cron_tasks,
    (SELECT COUNT(*) FROM task WHERE trigger_type = 'WEBHOOK') as webhook_tasks,
    (SELECT COUNT(*) FROM task_execution) as total_executions,
    (SELECT COUNT(*) FROM task_execution WHERE status = 'SUCCESS') as success_executions,
    (SELECT COUNT(*) FROM task_execution WHERE status = 'FAILED') as failed_executions;
