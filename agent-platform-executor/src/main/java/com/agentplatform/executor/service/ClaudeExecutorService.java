package com.agentplatform.executor.service;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.TaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Claude CLI执行器服务
 * 通过ProcessBuilder调用Claude CLI执行命令
 */
@Slf4j
@Service
public class ClaudeExecutorService implements TaskExecutor {

    @Value("${app.execution.default-timeout-seconds:300}")
    private int defaultTimeoutSeconds;

    @Value("${app.claude.cli-path:claude}")
    private String cliPath;

    @Override
    public ExecutionResult execute(Task task) {
        long startTime = System.currentTimeMillis();
        int timeout = task.getTimeoutSeconds() != null ? task.getTimeoutSeconds() : defaultTimeoutSeconds;

        try {
            // 创建独立工作目录
            Path executionDir = createExecutionDir(task);

            // 构建命令
            List<String> command = buildCommand(task);
            log.info("执行任务: id={}, command={}", task.getId(), String.join(" ", command));

            // 创建进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(executionDir.toFile());
            pb.redirectErrorStream(false);

            // 设置环境变量隔离
            pb.environment().put("AGENT_PLATFORM_TASK_ID", String.valueOf(task.getId()));
            pb.environment().put("AGENT_PLATFORM_EXECUTION_DIR", executionDir.toString());

            Process process = pb.start();

            // 捕获输出
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());

            // 等待进程完成（带超时）
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                long duration = System.currentTimeMillis() - startTime;
                log.warn("任务执行超时: id={}, timeout={}s", task.getId(), timeout);
                return ExecutionResult.builder()
                        .status(ExecutionStatus.TIMEOUT)
                        .error("执行超时，已强制终止")
                        .durationMs(duration)
                        .build();
            }

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
                log.info("任务执行成功: id={}, duration={}ms", task.getId(), duration);
                return ExecutionResult.builder()
                        .status(ExecutionStatus.SUCCESS)
                        .exitCode(exitCode)
                        .output(output)
                        .durationMs(duration)
                        .build();
            } else {
                log.warn("任务执行失败: id={}, exitCode={}", task.getId(), exitCode);
                return ExecutionResult.builder()
                        .status(ExecutionStatus.FAILED)
                        .exitCode(exitCode)
                        .output(output)
                        .error(error)
                        .durationMs(duration)
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务执行被中断: id={}", task.getId(), e);
            return ExecutionResult.builder()
                    .status(ExecutionStatus.FAILED)
                    .error("执行被中断: " + e.getMessage())
                    .durationMs(duration)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务执行异常: id={}", task.getId(), e);
            return ExecutionResult.builder()
                    .status(ExecutionStatus.FAILED)
                    .error("执行异常: " + e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            List<String> command = buildVersionCommand();
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                String output = readStream(process.getInputStream());
                log.info("Claude CLI可用: {}", output.trim());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Claude CLI不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建执行命令（非交互模式）
     * claude -p --dangerously-skip-permissions "prompt"
     */
    private List<String> buildCommand(Task task) {
        List<String> command = new ArrayList<>();

        if (isWindows()) {
            command.addAll(Arrays.asList("cmd", "/c", cliPath));
        } else {
            command.add(cliPath);
        }

        // 非交互模式参数
        command.add("-p");
        command.add("--dangerously-skip-permissions");
        command.add(task.getCommand());

        return command;
    }

    /**
     * 构建版本检查命令
     */
    private List<String> buildVersionCommand() {
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.addAll(Arrays.asList("cmd", "/c", cliPath, "--version"));
        } else {
            command.addAll(Arrays.asList(cliPath, "--version"));
        }
        return command;
    }

    /**
     * 创建独立执行目录
     */
    private Path createExecutionDir(Task task) throws IOException {
        Path tempDir = Files.createTempDirectory("agent-platform-execution-");
        log.debug("创建执行目录: {}", tempDir);
        return tempDir;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 读取进程输出流
     */
    private String readStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            log.warn("读取输出流失败: {}", e.getMessage());
            return "";
        }
    }
}
