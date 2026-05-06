package com.agentplatform.executor.service;

import com.agentplatform.core.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionIsolationService 单元测试
 * 测试执行隔离服务的环境创建和清理逻辑
 */
@DisplayName("执行隔离服务测试")
class ExecutionIsolationServiceTest {

    private ExecutionIsolationService isolationService;
    private Task task;

    @BeforeEach
    void setUp() {
        isolationService = new ExecutionIsolationService();
        task = Task.builder()
                .id(1L)
                .name("测试任务")
                .command("echo hello")
                .build();
    }

    @Test
    @DisplayName("createEnvironment - 创建的临时目录应存在")
    void createEnvironment_createsTempDirectoryThatExists() throws IOException {
        ExecutionIsolationService.ExecutionEnvironment env =
                isolationService.createEnvironment(task, 100L);

        try {
            assertNotNull(env, "执行环境不应为null");
            assertNotNull(env.executionDir(), "执行目录不应为null");
            assertTrue(Files.exists(env.executionDir()), "执行目录应存在");
            assertTrue(Files.isDirectory(env.executionDir()), "执行目录应为目录");
        } finally {
            // 清理临时目录
            isolationService.cleanupEnvironment(env.executionDir());
        }
    }

    @Test
    @DisplayName("createEnvironment - 设置正确的环境变量")
    void createEnvironment_setsCorrectEnvVars() throws IOException {
        Long executionId = 200L;

        ExecutionIsolationService.ExecutionEnvironment env =
                isolationService.createEnvironment(task, executionId);

        try {
            Map<String, String> envVars = env.environmentVariables();

            assertNotNull(envVars, "环境变量Map不应为null");
            assertEquals("1", envVars.get("AGENT_PLATFORM_TASK_ID"),
                    "TASK_ID应为任务ID");
            assertEquals("200", envVars.get("AGENT_PLATFORM_EXECUTION_ID"),
                    "EXECUTION_ID应为执行记录ID");
            assertEquals(env.executionDir().toString(), envVars.get("AGENT_PLATFORM_EXECUTION_DIR"),
                    "EXECUTION_DIR应为临时目录路径");
            assertEquals("测试任务", envVars.get("AGENT_PLATFORM_TASK_NAME"),
                    "TASK_NAME应为任务名称");
        } finally {
            isolationService.cleanupEnvironment(env.executionDir());
        }
    }

    @Test
    @DisplayName("cleanupEnvironment - 删除目录及其内容")
    void cleanupEnvironment_deletesDirectory() throws IOException {
        // 先创建一个执行环境
        ExecutionIsolationService.ExecutionEnvironment env =
                isolationService.createEnvironment(task, 300L);
        Path execDir = env.executionDir();

        // 在目录中创建一个测试文件
        Path testFile = execDir.resolve("test-file.txt");
        Files.writeString(testFile, "test content");
        assertTrue(Files.exists(testFile), "测试文件应已创建");

        // 执行清理
        isolationService.cleanupEnvironment(execDir);

        // 验证目录已被删除
        assertFalse(Files.exists(execDir), "执行目录应已被删除");
    }

    @Test
    @DisplayName("cleanupEnvironment - 处理不存在的目录时不抛异常")
    void cleanupEnvironment_handlesNonExistentDirectoryGracefully() {
        Path nonExistentDir = Path.of("/tmp/non-existent-dir-" + System.currentTimeMillis());

        // 不应抛出异常
        assertDoesNotThrow(() -> isolationService.cleanupEnvironment(nonExistentDir),
                "清理不存在的目录不应抛出异常");
    }
}
