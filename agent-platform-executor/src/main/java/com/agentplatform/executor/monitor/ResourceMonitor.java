package com.agentplatform.executor.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 资源监控器
 * 监控内存、磁盘空间和并发限制
 */
@Slf4j
@Service
public class ResourceMonitor {

    @Value("${app.execution.max-concurrent:10}")
    private int maxConcurrent;

    /**
     * 检查系统资源是否充足
     * @return true表示资源充足，可以执行新任务
     */
    public boolean checkResources() {
        return checkMemory() && checkDiskSpace();
    }

    /**
     * 检查内存使用
     * @return true表示内存充足
     */
    public boolean checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double usagePercent = (double) usedMemory / maxMemory * 100;

        if (usagePercent > 80) {
            log.warn("内存使用率过高: {:.1f}%", usagePercent);
            return false;
        }

        log.debug("内存使用: used={}MB, max={}MB, usage={:.1f}%",
                usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, usagePercent);
        return true;
    }

    /**
     * 检查磁盘空间
     * @return true表示磁盘空间充足
     */
    public boolean checkDiskSpace() {
        File dataDir = new File("./data");
        long freeSpace = dataDir.getFreeSpace();
        long freeSpaceMB = freeSpace / 1024 / 1024;

        if (freeSpaceMB < 100) {
            log.warn("磁盘空间不足: {}MB", freeSpaceMB);
            return false;
        }

        log.debug("磁盘可用空间: {}MB", freeSpaceMB);
        return true;
    }

    /**
     * 检查并发数是否超限
     * @param currentActive 当前活跃执行数
     * @return true表示未超限
     */
    public boolean checkConcurrency(int currentActive) {
        if (currentActive >= maxConcurrent) {
            log.warn("并发执行数已达上限: current={}, max={}", currentActive, maxConcurrent);
            return false;
        }
        return true;
    }

    /**
     * 获取系统资源信息
     */
    public ResourceInfo getResourceInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        File dataDir = new File("./data");
        long freeDiskSpace = dataDir.getFreeSpace();

        return new ResourceInfo(
                maxMemory / 1024 / 1024,
                usedMemory / 1024 / 1024,
                freeMemory / 1024 / 1024,
                freeDiskSpace / 1024 / 1024,
                (double) usedMemory / maxMemory * 100
        );
    }

    /**
     * 资源信息
     */
    public record ResourceInfo(
            long maxMemoryMb,
            long usedMemoryMb,
            long freeMemoryMb,
            long freeDiskMb,
            double memoryUsagePercent
    ) {}
}
