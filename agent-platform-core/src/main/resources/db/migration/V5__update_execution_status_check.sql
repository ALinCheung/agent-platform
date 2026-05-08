-- 更新 task_execution 表的 CHECK 约束以包含 TERMINATED 状态
-- SQLite 不支持 ALTER CHECK，需要重建表

-- 1. 创建临时表（包含新的 CHECK 约束）
CREATE TABLE IF NOT EXISTS task_execution_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    task_version_id INTEGER,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT', 'RETRYING', 'TERMINATED')),
    retry_count INTEGER NOT NULL DEFAULT 0,
    parent_execution_id INTEGER,
    output TEXT,
    error TEXT,
    exit_code INTEGER,
    execution_dir VARCHAR(500),
    duration_ms INTEGER,
    memory_used_mb INTEGER,
    started_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    finished_at TEXT,
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    FOREIGN KEY (task_version_id) REFERENCES task_version(id),
    FOREIGN KEY (parent_execution_id) REFERENCES task_execution(id)
);

-- 2. 复制数据
INSERT INTO task_execution_new SELECT * FROM task_execution;

-- 3. 删除旧表
DROP TABLE task_execution;

-- 4. 重命名新表
ALTER TABLE task_execution_new RENAME TO task_execution;

-- 5. 重建索引
CREATE INDEX IF NOT EXISTS idx_task_execution_task_id ON task_execution(task_id);
CREATE INDEX IF NOT EXISTS idx_task_execution_status ON task_execution(status);
CREATE INDEX IF NOT EXISTS idx_task_execution_started_at ON task_execution(started_at);
CREATE INDEX IF NOT EXISTS idx_task_execution_parent ON task_execution(parent_execution_id);

-- 6. 重建触发器
CREATE TRIGGER IF NOT EXISTS trg_update_task_stats_success
AFTER UPDATE OF status ON task_execution
WHEN NEW.status = 'SUCCESS' AND OLD.status != 'SUCCESS'
BEGIN
    UPDATE task SET
        success_count = success_count + 1,
        last_execution_status = 'SUCCESS',
        last_execution_at = NEW.finished_at
    WHERE id = NEW.task_id;
END;

CREATE TRIGGER IF NOT EXISTS trg_update_task_stats_failure
AFTER UPDATE OF status ON task_execution
WHEN NEW.status IN ('FAILED', 'TIMEOUT') AND OLD.status NOT IN ('FAILED', 'TIMEOUT')
BEGIN
    UPDATE task SET
        failure_count = failure_count + 1,
        last_execution_status = NEW.status,
        last_execution_at = NEW.finished_at
    WHERE id = NEW.task_id;
END;

-- 7. 更新 task 表的 last_execution_status CHECK 约束以包含 TERMINATED
CREATE TABLE IF NOT EXISTS task_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    command TEXT NOT NULL,
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('CRON', 'WEBHOOK', 'EVENT')),
    cron_expression VARCHAR(100),
    webhook_path VARCHAR(200),
    webhook_secret VARCHAR(200),
    timeout_seconds INTEGER NOT NULL DEFAULT 300,
    max_retries INTEGER NOT NULL DEFAULT 0,
    retry_interval_seconds INTEGER NOT NULL DEFAULT 60,
    work_dir VARCHAR(500),
    enabled INTEGER NOT NULL DEFAULT 1,
    last_execution_status VARCHAR(20) CHECK (last_execution_status IN ('RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT', 'RETRYING', 'TERMINATED')),
    last_execution_at TEXT,
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

INSERT INTO task_new SELECT * FROM task;
DROP TABLE task;
ALTER TABLE task_new RENAME TO task;

-- 重建 task 表索引
CREATE INDEX IF NOT EXISTS idx_task_enabled ON task(enabled);
CREATE INDEX IF NOT EXISTS idx_task_trigger_type ON task(trigger_type);
CREATE INDEX IF NOT EXISTS idx_task_webhook_path ON task(webhook_path);
CREATE INDEX IF NOT EXISTS idx_task_last_status ON task(last_execution_status);

-- 重建 task 触发器
CREATE TRIGGER IF NOT EXISTS trg_task_updated_at
AFTER UPDATE ON task
BEGIN
    UPDATE task SET updated_at = datetime('now', 'localtime') WHERE id = NEW.id;
END;
