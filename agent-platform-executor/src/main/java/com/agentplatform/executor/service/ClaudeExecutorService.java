package com.agentplatform.executor.service;

import com.agentplatform.core.entity.ExecutionResult;
import com.agentplatform.core.entity.Subtask;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.enums.LogType;
import com.agentplatform.core.service.ExecutionLogService;
import com.agentplatform.core.service.TaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    private final ExecutionLogService executionLogService;

    /** 正在运行的进程：executionId -> Process */
    private final java.util.concurrent.ConcurrentHashMap<Long, Process> runningProcesses = new java.util.concurrent.ConcurrentHashMap<>();

    public ClaudeExecutorService(ExecutionLogService executionLogService) {
        this.executionLogService = executionLogService;
    }

    @Override
    public ExecutionResult execute(Task task) {
        return executeWithLogCapture(task.getCommand(), task.getTimeoutSeconds(), null, null);
    }

    /**
     * 执行指定 prompt 并捕获输出到日志表
     */
    public ExecutionResult executeWithLogCapture(String prompt, Integer timeoutSeconds,
                                                  Long executionId, Long subtaskId) {
        long startTime = System.currentTimeMillis();
        int timeout = timeoutSeconds != null ? timeoutSeconds : defaultTimeoutSeconds;

        try {
            Path executionDir = Files.createTempDirectory("agent-platform-execution-");
            List<String> command = buildPromptCommand(prompt);

            log.info("执行命令: prompt={}, executionId={}", prompt.substring(0, Math.min(50, prompt.length())), executionId);

            // 记录 prompt 到日志
            if (executionId != null) {
                executionLogService.appendLog(executionId, subtaskId, LogType.PROMPT, prompt);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(executionDir.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // 记录进程用于终止
            if (executionId != null) {
                runningProcesses.put(executionId, process);
            }

            // 使用 StreamGobbler 实时捕获输出
            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();

            Thread stdoutThread = new Thread(new StreamGobbler(
                    process.getInputStream(), LogType.OUTPUT, executionId, subtaskId, outputBuilder));
            Thread stderrThread = new Thread(new StreamGobbler(
                    process.getErrorStream(), LogType.ERROR, executionId, subtaskId, errorBuilder));

            stdoutThread.start();
            stderrThread.start();

            // 等待进程完成（带超时）
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            // 等待输出线程结束
            stdoutThread.join(5000);
            stderrThread.join(5000);

            if (executionId != null) {
                runningProcesses.remove(executionId);
            }

            if (!finished) {
                process.destroyForcibly();
                long duration = System.currentTimeMillis() - startTime;
                log.warn("执行超时: timeout={}s", timeout);
                return ExecutionResult.builder()
                        .status(ExecutionStatus.TIMEOUT)
                        .error("执行超时，已强制终止")
                        .durationMs(duration)
                        .build();
            }

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
                log.info("执行成功: duration={}ms", duration);
                return ExecutionResult.builder()
                        .status(ExecutionStatus.SUCCESS)
                        .exitCode(exitCode)
                        .output(outputBuilder.toString())
                        .durationMs(duration)
                        .build();
            } else {
                log.warn("执行失败: exitCode={}", exitCode);
                return ExecutionResult.builder()
                        .status(ExecutionStatus.FAILED)
                        .exitCode(exitCode)
                        .output(outputBuilder.toString())
                        .error(errorBuilder.toString())
                        .durationMs(duration)
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            log.error("执行被中断", e);
            if (executionId != null) runningProcesses.remove(executionId);
            return ExecutionResult.builder()
                    .status(ExecutionStatus.FAILED)
                    .error("执行被中断: " + e.getMessage())
                    .durationMs(duration)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("执行异常", e);
            if (executionId != null) runningProcesses.remove(executionId);
            return ExecutionResult.builder()
                    .status(ExecutionStatus.FAILED)
                    .error("执行异常: " + e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }

    /**
     * 分解任务为子任务列表
     */
    public List<Subtask> decomposeTask(Task task, Long executionId) {
        String decomposePrompt = "请将以下任务分解为具体的子任务步骤，以JSON数组格式返回，每个元素包含seq（从1开始的整数）、title（简短标题）、description（详细描述）字段。只返回JSON数组，不要有其他文字：\n" + task.getCommand();

        log.info("分解任务: taskId={}, executionId={}", task.getId(), executionId);
        if (executionId != null) {
            executionLogService.appendLog(executionId, null, LogType.STEP, "开始分解任务为子任务列表");
        }

        ExecutionResult result = executeWithLogCapture(decomposePrompt, task.getTimeoutSeconds(), executionId, null);

        if (result.getStatus() != ExecutionStatus.SUCCESS || result.getOutput() == null) {
            log.warn("任务分解失败，回退为单任务模式");
            if (executionId != null) {
                executionLogService.appendLog(executionId, null, LogType.STEP, "任务分解失败，回退为单任务模式");
            }
            return null;
        }

        try {
            return parseSubtasks(result.getOutput(), executionId);
        } catch (Exception e) {
            log.warn("解析子任务JSON失败: {}", e.getMessage());
            if (executionId != null) {
                executionLogService.appendLog(executionId, null, LogType.STEP, "子任务JSON解析失败: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 执行单个子任务
     */
    public ExecutionResult executeSubtask(Subtask subtask, String taskName, int totalSubtasks,
                                           Integer timeoutSeconds, Long executionId) {
        String prompt = String.format("你正在执行任务\"%s\"的子任务步骤 %d/%d：%s\n%s\n请执行此步骤并返回结果。",
                taskName, subtask.getSeq(), totalSubtasks, subtask.getTitle(),
                subtask.getDescription() != null ? subtask.getDescription() : "");

        if (executionId != null) {
            executionLogService.appendLog(executionId, subtask.getId(), LogType.STEP,
                    String.format("开始执行子任务 %d/%d: %s", subtask.getSeq(), totalSubtasks, subtask.getTitle()));
        }

        return executeWithLogCapture(prompt, timeoutSeconds, executionId, subtask.getId());
    }

    /**
     * 解析 Claude 返回的子任务 JSON
     */
    private List<Subtask> parseSubtasks(String jsonOutput, Long executionId) {
        // 提取 JSON 数组部分（Claude 可能返回包含其他文字的内容）
        String json = jsonOutput.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("无法找到 JSON 数组");
        }
        json = json.substring(start, end + 1);

        // 简单 JSON 解析（避免引入额外依赖）
        List<Subtask> subtasks = new ArrayList<>();
        // 使用简单的字符串解析
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("JSON 格式错误");
        }

        // 移除外层括号
        String inner = json.substring(1, json.length() - 1).trim();
        if (inner.isEmpty()) {
            return subtasks;
        }

        // 按 },{ 分割各个对象
        List<String> objects = splitJsonObjects(inner);

        for (String obj : objects) {
            Subtask subtask = parseSingleSubtask(obj);
            if (subtask != null) {
                subtasks.add(subtask);
            }
        }

        if (executionId != null && !subtasks.isEmpty()) {
            executionLogService.appendLog(executionId, null, LogType.STEP,
                    String.format("成功分解为 %d 个子任务", subtasks.size()));
        }

        return subtasks;
    }

    private List<String> splitJsonObjects(String inner) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == ',' && depth == 0) {
                objects.add(inner.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (start < inner.length()) {
            objects.add(inner.substring(start).trim());
        }
        return objects;
    }

    private Subtask parseSingleSubtask(String obj) {
        try {
            obj = obj.trim();
            if (!obj.startsWith("{") || !obj.endsWith("}")) return null;
            obj = obj.substring(1, obj.length() - 1).trim();

            Integer seq = null;
            String title = null;
            String description = null;

            // 简单的 key:value 解析
            String[] parts = obj.split(",");
            for (String part : parts) {
                int colonIdx = part.indexOf(':');
                if (colonIdx < 0) continue;
                String key = part.substring(0, colonIdx).trim().replace("\"", "");
                String value = part.substring(colonIdx + 1).trim();

                // 移除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                switch (key) {
                    case "seq" -> seq = Integer.parseInt(value);
                    case "title" -> title = value;
                    case "description" -> description = value;
                }
            }

            if (seq == null || title == null) return null;

            return Subtask.builder()
                    .seq(seq)
                    .title(title)
                    .description(description)
                    .build();
        } catch (Exception e) {
            log.warn("解析单个子任务失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 终止指定执行的进程
     */
    public boolean terminateProcess(Long executionId) {
        Process process = runningProcesses.get(executionId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningProcesses.remove(executionId);
            log.info("已终止进程: executionId={}", executionId);
            return true;
        }
        return false;
    }

    /**
     * 构建执行命令（非交互模式）
     */
    private List<String> buildPromptCommand(String prompt) {
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.addAll(Arrays.asList("cmd", "/c", cliPath));
        } else {
            command.add(cliPath);
        }
        command.add("-p");
        command.add("--dangerously-skip-permissions");
        command.add(prompt);
        return command;
    }

    @Override
    public boolean isAvailable() {
        try {
            List<String> command = new ArrayList<>();
            if (isWindows()) {
                command.addAll(Arrays.asList("cmd", "/c", cliPath, "--version"));
            } else {
                command.addAll(Arrays.asList(cliPath, "--version"));
            }
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

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String readStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
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

    /**
     * 输出流实时捕获器
     * 使用独立线程逐行读取进程输出流并写入日志表
     */
    private class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final LogType logType;
        private final Long executionId;
        private final Long subtaskId;
        private final StringBuilder collector;

        StreamGobbler(InputStream inputStream, LogType logType, Long executionId, Long subtaskId, StringBuilder collector) {
            this.inputStream = inputStream;
            this.logType = logType;
            this.executionId = executionId;
            this.subtaskId = subtaskId;
            this.collector = collector;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    collector.append(line).append("\n");
                    // 实时写入日志表
                    if (executionId != null) {
                        try {
                            executionLogService.appendLog(executionId, subtaskId, logType, line);
                        } catch (Exception e) {
                            log.debug("写入日志失败: {}", e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.debug("StreamGobbler读取结束: {}", e.getMessage());
            }
        }
    }
}
