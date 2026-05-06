package com.agentplatform.executor.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceMonitor 单元测试
 * 测试资源监控器的并发检查和资源信息获取
 */
@DisplayName("资源监控器测试")
class ResourceMonitorTest {

    private ResourceMonitor resourceMonitor;

    @BeforeEach
    void setUp() throws Exception {
        resourceMonitor = new ResourceMonitor();
        // 通过反射设置 @Value 注入的 maxConcurrent 字段
        Field maxConcurrentField = ResourceMonitor.class.getDeclaredField("maxConcurrent");
        maxConcurrentField.setAccessible(true);
        maxConcurrentField.setInt(resourceMonitor, 10);
    }

    @Test
    @DisplayName("checkConcurrency - 未达上限时返回true")
    void checkConcurrency_returnsTrue_whenUnderLimit() {
        // 当前活跃数5，上限10
        boolean result = resourceMonitor.checkConcurrency(5);

        assertTrue(result, "活跃数未达上限时应返回true");
    }

    @Test
    @DisplayName("checkConcurrency - 达到上限时返回false")
    void checkConcurrency_returnsFalse_whenAtOrOverLimit() throws Exception {
        // 设置上限为10
        Field maxConcurrentField = ResourceMonitor.class.getDeclaredField("maxConcurrent");
        maxConcurrentField.setAccessible(true);
        maxConcurrentField.setInt(resourceMonitor, 10);

        // 当前活跃数等于上限
        boolean resultAtLimit = resourceMonitor.checkConcurrency(10);
        assertFalse(resultAtLimit, "活跃数等于上限时应返回false");

        // 当前活跃数超过上限
        boolean resultOverLimit = resourceMonitor.checkConcurrency(15);
        assertFalse(resultOverLimit, "活跃数超过上限时应返回false");
    }

    @Test
    @DisplayName("getResourceInfo - 返回非null且包含有效值")
    void getResourceInfo_returnsNonNullWithValidValues() {
        ResourceMonitor.ResourceInfo info = resourceMonitor.getResourceInfo();

        assertNotNull(info, "资源信息不应为null");
        assertTrue(info.maxMemoryMb() > 0, "最大内存应大于0");
        assertTrue(info.usedMemoryMb() >= 0, "已用内存应大于等于0");
        assertTrue(info.freeMemoryMb() >= 0, "空闲内存应大于等于0");
        assertTrue(info.freeDiskMb() >= 0, "空闲磁盘应大于等于0");
        assertTrue(info.memoryUsagePercent() >= 0 && info.memoryUsagePercent() <= 100,
                "内存使用率应在0-100之间");
    }

    @Test
    @DisplayName("checkMemory - 返回布尔值（取决于JVM状态）")
    void checkMemory_returnsBoolean() {
        // 此测试仅验证方法可正常调用，结果取决于JVM实际内存状态
        boolean result = resourceMonitor.checkMemory();

        // 在正常测试环境下，内存使用率不应超过80%
        assertTrue(result, "测试环境下内存应充足");
    }

    @Test
    @DisplayName("checkDiskSpace - 返回布尔值（取决于系统状态）")
    void checkDiskSpace_returnsBoolean() {
        // 此测试仅验证方法可正常调用，结果取决于系统实际磁盘状态
        boolean result = resourceMonitor.checkDiskSpace();

        // 在正常测试环境下，磁盘空间应大于100MB
        assertTrue(result, "测试环境下磁盘空间应充足");
    }
}
