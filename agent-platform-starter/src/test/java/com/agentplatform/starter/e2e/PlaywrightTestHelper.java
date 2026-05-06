package com.agentplatform.starter.e2e;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Playwright测试辅助工具
 * 封装playwright-cli命令行调用
 */
@Slf4j
public class PlaywrightTestHelper {

    /**
     * 检查playwright-cli是否可用
     */
    public static boolean isPlaywrightAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("npx", "playwright", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                String output = readStream(process);
                log.info("Playwright版本: {}", output.trim());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Playwright不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 运行Playwright测试
     * @param testFile 测试文件路径
     * @param baseURL 测试目标URL
     * @return 测试输出
     */
    public static String runTest(String testFile, String baseURL) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "npx", "playwright", "test",
                testFile,
                "--config", "playwright.config.ts",
                "--base-url", baseURL
        );
        pb.redirectErrorStream(true);
        pb.environment().put("BASE_URL", baseURL);

        Process process = pb.start();
        String output = readStream(process);
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Playwright测试超时");
        }

        if (process.exitValue() != 0) {
            log.error("Playwright测试失败: {}", output);
        }

        return output;
    }

    /**
     * 读取进程输出流
     */
    private static String readStream(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
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
