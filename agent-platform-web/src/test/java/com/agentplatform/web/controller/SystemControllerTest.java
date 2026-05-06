package com.agentplatform.web.controller;

import com.agentplatform.executor.monitor.ResourceMonitor;
import com.agentplatform.scheduler.service.CliHealthChecker;
import com.agentplatform.scheduler.service.SchedulerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 系统管理控制器单元测试
 * 测试健康检查、资源监控等功能
 */
@WebMvcTest(SystemController.class)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceMonitor resourceMonitor;

    @MockBean
    private SchedulerService schedulerService;

    @MockBean
    private CliHealthChecker cliHealthChecker;

    /**
     * 测试健康检查返回200且状态为UP
     */
    @Test
    @DisplayName("健康检查返回200且状态为UP")
    void health_returns200_withStatusUP() throws Exception {
        when(cliHealthChecker.isCliAvailable()).thenReturn(true);
        when(schedulerService.getRegisteredCount()).thenReturn(3);

        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.cliAvailable").value(true))
                .andExpect(jsonPath("$.scheduledTasks").value(3))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(cliHealthChecker).isCliAvailable();
        verify(schedulerService).getRegisteredCount();
    }

    /**
     * 测试资源信息返回200
     */
    @Test
    @DisplayName("获取系统资源信息返回200")
    void resources_returns200_withResourceInfo() throws Exception {
        ResourceMonitor.ResourceInfo resourceInfo = new ResourceMonitor.ResourceInfo(
                1024L,   // maxMemoryMb
                512L,    // usedMemoryMb
                512L,    // freeMemoryMb
                20480L,  // freeDiskMb
                50.0     // memoryUsagePercent
        );
        when(resourceMonitor.getResourceInfo()).thenReturn(resourceInfo);

        mockMvc.perform(get("/api/system/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxMemoryMb").value(1024))
                .andExpect(jsonPath("$.usedMemoryMb").value(512))
                .andExpect(jsonPath("$.freeMemoryMb").value(512))
                .andExpect(jsonPath("$.freeDiskMb").value(20480))
                .andExpect(jsonPath("$.memoryUsagePercent").value(50.0));

        verify(resourceMonitor).getResourceInfo();
    }
}
