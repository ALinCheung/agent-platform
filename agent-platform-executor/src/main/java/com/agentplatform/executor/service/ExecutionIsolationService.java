package com.agentplatform.executor.service;

import com.agentplatform.core.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 执行隔离服务
 * 为每次执行提供独立的工作目录和环境变量
 */
@Slf4j
@Service
public class ExecutionIsolationService {

    /**
     * 创建隔离的执行环境
     * @param task 任务定义
     * @param executionId 执行记录ID
     * @return 执行环境信息
     */
    public ExecutionEnvironment createEnvironment(Task task, Long executionId) throws IOException {
        // 创建独立临时目录
        Path executionDir = Files.createTempDirectory("agent-platform-exec-" + executionId + "-");
        log.debug("创建执行目录: taskId={}, dir={}", task.getId(), executionDir);

        // 构建隔离的环境变量
        Map<String, String> env = new HashMap<>();
        env.put("AGENT_PLATFORM_TASK_ID", String.valueOf(task.getId()));
        env.put("AGENT_PLATFORM_EXECUTION_ID", String.valueOf(executionId));
        env.put("AGENT_PLATFORM_EXECUTION_DIR", executionDir.toString());
        env.put("AGENT_PLATFORM_TASK_NAME", task.getName());

        return new ExecutionEnvironment(executionDir, env);
    }

    /**
     * 清理执行环境
     */
    public void cleanupEnvironment(Path executionDir) {
        try {
            if (Files.exists(executionDir)) {
                // 递归删除目录
                Files.walk(executionDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("删除文件失败: {}", path, e);
                            }
                        });
                log.debug("清理执行目录: {}", executionDir);
            }
        } catch (IOException e) {
            log.warn("清理执行目录失败: {}", executionDir, e);
        }
    }

    /**
     * 执行环境信息
     */
    public record ExecutionEnvironment(Path executionDir, Map<String, String> environmentVariables) {}
}
