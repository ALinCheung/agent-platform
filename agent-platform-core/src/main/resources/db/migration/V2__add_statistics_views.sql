-- AI自动化平台 - 添加统计视图
-- SQLite DDL Migration

-- 任务统计视图
CREATE VIEW IF NOT EXISTS v_task_stats AS
SELECT
    t.id AS task_id,
    t.name AS task_name,
    t.trigger_type,
    t.enabled,
    t.last_execution_status,
    t.last_execution_at,
    t.success_count,
    t.failure_count,
    CASE WHEN (t.success_count + t.failure_count) > 0
        THEN ROUND(t.success_count * 100.0 / (t.success_count + t.failure_count), 2)
        ELSE 0 END AS success_rate
FROM task t;

-- 执行统计视图
CREATE VIEW IF NOT EXISTS v_execution_stats AS
SELECT
    DATE(started_at) AS execution_date,
    COUNT(*) AS total_executions,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
    SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) AS timeout_count,
    SUM(CASE WHEN status = 'RETRYING' THEN 1 ELSE 0 END) AS retrying_count,
    ROUND(AVG(duration_ms), 2) AS avg_duration_ms,
    ROUND(AVG(memory_used_mb), 2) AS avg_memory_mb
FROM task_execution
GROUP BY DATE(started_at)
ORDER BY execution_date DESC;

-- 每小时执行分布视图
CREATE VIEW IF NOT EXISTS v_hourly_stats AS
SELECT
    strftime('%H', started_at) AS execution_hour,
    COUNT(*) AS total_executions,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
FROM task_execution
WHERE started_at >= datetime('now', '-1 day', 'localtime')
GROUP BY strftime('%H', started_at)
ORDER BY execution_hour;

-- 失败任务TOP榜视图
CREATE VIEW IF NOT EXISTS v_failure_top AS
SELECT
    t.id AS task_id,
    t.name AS task_name,
    t.failure_count,
    t.last_execution_at
FROM task t
WHERE t.failure_count > 0
ORDER BY t.failure_count DESC
LIMIT 10;

-- 耗时任务TOP榜视图
CREATE VIEW IF NOT EXISTS v_duration_top AS
SELECT
    t.id AS task_id,
    t.name AS task_name,
    ROUND(AVG(e.duration_ms), 2) AS avg_duration_ms,
    MAX(e.duration_ms) AS max_duration_ms,
    COUNT(e.id) AS execution_count
FROM task t
JOIN task_execution e ON t.id = e.task_id
WHERE e.duration_ms IS NOT NULL
GROUP BY t.id, t.name
ORDER BY avg_duration_ms DESC
LIMIT 10;

-- 重试统计视图
CREATE VIEW IF NOT EXISTS v_retry_stats AS
SELECT
    t.id AS task_id,
    t.name AS task_name,
    t.max_retries,
    COUNT(CASE WHEN e.retry_count > 0 THEN 1 END) AS retried_executions,
    COUNT(CASE WHEN e.retry_count >= t.max_retries AND e.status = 'FAILED' THEN 1 END) AS exhausted_retries,
    ROUND(AVG(e.retry_count), 2) AS avg_retry_count
FROM task t
LEFT JOIN task_execution e ON t.id = e.task_id
GROUP BY t.id, t.name, t.max_retries;

-- 系统总览视图
CREATE VIEW IF NOT EXISTS v_system_overview AS
SELECT
    (SELECT COUNT(*) FROM task) AS total_tasks,
    (SELECT COUNT(*) FROM task WHERE enabled = 1) AS enabled_tasks,
    (SELECT COUNT(*) FROM task WHERE enabled = 0) AS disabled_tasks,
    (SELECT COUNT(*) FROM task WHERE trigger_type = 'CRON') AS cron_tasks,
    (SELECT COUNT(*) FROM task WHERE trigger_type = 'WEBHOOK') AS webhook_tasks,
    (SELECT COUNT(*) FROM task WHERE trigger_type = 'EVENT') AS event_tasks,
    (SELECT COUNT(*) FROM task_execution WHERE DATE(started_at) = DATE('now', 'localtime')) AS today_executions,
    (SELECT COUNT(*) FROM task_execution WHERE DATE(started_at) = DATE('now', 'localtime') AND status = 'SUCCESS') AS today_success,
    (SELECT COUNT(*) FROM task_execution WHERE DATE(started_at) = DATE('now', 'localtime') AND status = 'FAILED') AS today_failed,
    (SELECT COUNT(*) FROM task_execution WHERE status = 'RUNNING') AS running_executions;
