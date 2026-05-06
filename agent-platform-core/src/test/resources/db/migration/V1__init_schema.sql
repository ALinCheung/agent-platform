-- 任务定义表
CREATE TABLE IF NOT EXISTS task (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
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
    last_execution_status VARCHAR(20) CHECK (last_execution_status IN ('RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT', 'RETRYING')),
    last_execution_at VARCHAR(30),
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    created_at VARCHAR(30) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at VARCHAR(30) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 任务配置版本表
CREATE TABLE IF NOT EXISTS task_version (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    task_id INTEGER NOT NULL,
    version INTEGER NOT NULL,
    command TEXT NOT NULL,
    cron_expression VARCHAR(100),
    webhook_path VARCHAR(200),
    webhook_secret VARCHAR(200),
    timeout_seconds INTEGER NOT NULL,
    max_retries INTEGER NOT NULL,
    retry_interval_seconds INTEGER NOT NULL,
    work_dir VARCHAR(500),
    change_type VARCHAR(20) NOT NULL CHECK (change_type IN ('CREATE', 'UPDATE', 'ROLLBACK')),
    change_description VARCHAR(500),
    created_at VARCHAR(30) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
);

-- 任务执行记录表
CREATE TABLE IF NOT EXISTS task_execution (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    task_id INTEGER NOT NULL,
    task_version_id INTEGER,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT', 'RETRYING')),
    retry_count INTEGER NOT NULL DEFAULT 0,
    parent_execution_id INTEGER,
    output TEXT,
    error TEXT,
    exit_code INTEGER,
    execution_dir VARCHAR(500),
    duration_ms INTEGER,
    memory_used_mb INTEGER,
    started_at VARCHAR(30) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at VARCHAR(30),
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    FOREIGN KEY (task_version_id) REFERENCES task_version(id),
    FOREIGN KEY (parent_execution_id) REFERENCES task_execution(id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_task_enabled ON task(enabled);
CREATE INDEX IF NOT EXISTS idx_task_trigger_type ON task(trigger_type);
CREATE INDEX IF NOT EXISTS idx_task_webhook_path ON task(webhook_path);
CREATE INDEX IF NOT EXISTS idx_task_last_status ON task(last_execution_status);
CREATE INDEX IF NOT EXISTS idx_task_version_task_id ON task_version(task_id);
CREATE INDEX IF NOT EXISTS idx_task_version_version ON task_version(task_id, version);
CREATE INDEX IF NOT EXISTS idx_task_execution_task_id ON task_execution(task_id);
CREATE INDEX IF NOT EXISTS idx_task_execution_status ON task_execution(status);
CREATE INDEX IF NOT EXISTS idx_task_execution_started_at ON task_execution(started_at);
CREATE INDEX IF NOT EXISTS idx_task_execution_parent ON task_execution(parent_execution_id);
