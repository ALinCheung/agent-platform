-- 子任务表
CREATE TABLE IF NOT EXISTS task_subtask (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id INTEGER NOT NULL,
    seq INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED')),
    output TEXT,
    error TEXT,
    started_at TEXT,
    finished_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (execution_id) REFERENCES task_execution(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_subtask_execution_id ON task_subtask(execution_id);
CREATE INDEX IF NOT EXISTS idx_subtask_status ON task_subtask(status);
CREATE INDEX IF NOT EXISTS idx_subtask_seq ON task_subtask(execution_id, seq);
