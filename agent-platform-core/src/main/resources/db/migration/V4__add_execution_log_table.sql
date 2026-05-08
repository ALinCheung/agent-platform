-- 执行过程日志表
CREATE TABLE IF NOT EXISTS task_execution_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id INTEGER NOT NULL,
    subtask_id INTEGER,
    log_type VARCHAR(20) NOT NULL CHECK (log_type IN ('PROMPT', 'OUTPUT', 'ERROR', 'STEP', 'STATUS')),
    content TEXT NOT NULL,
    seq INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (execution_id) REFERENCES task_execution(id) ON DELETE CASCADE,
    FOREIGN KEY (subtask_id) REFERENCES task_subtask(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_exec_log_execution_id ON task_execution_log(execution_id);
CREATE INDEX IF NOT EXISTS idx_exec_log_subtask_id ON task_execution_log(subtask_id);
CREATE INDEX IF NOT EXISTS idx_exec_log_type ON task_execution_log(log_type);
CREATE INDEX IF NOT EXISTS idx_exec_log_seq ON task_execution_log(execution_id, seq);
