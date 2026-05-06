package com.agentplatform.web.controller;

import com.agentplatform.core.entity.TaskExecution;
import com.agentplatform.core.enums.ExecutionStatus;
import com.agentplatform.core.service.ExecutionHistoryService;
import com.agentplatform.executor.service.RetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 执行记录控制器单元测试
 * 测试执行历史查询、重试等功能
 */
@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecutionHistoryService executionHistoryService;

    @MockBean
    private RetryService retryService;

    /** 构建测试用执行记录对象 */
    private TaskExecution buildTestExecution() {
        return TaskExecution.builder()
                .id(1L).taskId(1L).status(ExecutionStatus.SUCCESS)
                .retryCount(0).output("test output").exitCode(0)
                .durationMs(1000L).memoryUsedMb(128)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .finishedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 测试获取全部执行记录返回200
     */
    @Test
    @DisplayName("获取全部执行记录返回200")
    void listExecutions_returns200_withAllExecutions() throws Exception {
        TaskExecution execution = buildTestExecution();
        when(executionHistoryService.list()).thenReturn(List.of(execution));

        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].taskId").value(1))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));

        verify(executionHistoryService).list();
    }

    /**
     * 测试按taskId过滤执行记录
     */
    @Test
    @DisplayName("按taskId过滤返回对应执行记录")
    void listExecutions_withTaskIdFilter_returnsFilteredList() throws Exception {
        TaskExecution execution = buildTestExecution();
        when(executionHistoryService.getByTaskId(1L)).thenReturn(List.of(execution));

        mockMvc.perform(get("/api/executions").param("taskId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].taskId").value(1));

        verify(executionHistoryService).getByTaskId(1L);
        verify(executionHistoryService, never()).list();
    }

    /**
     * 测试获取已存在的执行记录返回200
     */
    @Test
    @DisplayName("获取已存在的执行记录返回200")
    void getExecution_returns200_forExistingExecution() throws Exception {
        TaskExecution execution = buildTestExecution();
        when(executionHistoryService.getById(1L)).thenReturn(execution);

        mockMvc.perform(get("/api/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(executionHistoryService).getById(1L);
    }

    /**
     * 测试获取不存在的执行记录返回404
     */
    @Test
    @DisplayName("获取不存在的执行记录返回404")
    void getExecution_returns404_forNonExistent() throws Exception {
        when(executionHistoryService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/executions/999"))
                .andExpect(status().isNotFound());

        verify(executionHistoryService).getById(999L);
    }

    /**
     * 测试重试执行成功返回202
     */
    @Test
    @DisplayName("重试执行成功返回202")
    void retryExecution_returns202_onSuccess() throws Exception {
        when(retryService.manualRetry(1L)).thenReturn(200L);

        mockMvc.perform(post("/api/executions/1/retry"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("重试已提交"))
                .andExpect(jsonPath("$.executionId").value(200));

        verify(retryService).manualRetry(1L);
    }

    /**
     * 测试重试执行失败返回400
     */
    @Test
    @DisplayName("重试执行失败返回400")
    void retryExecution_returns400_onFailure() throws Exception {
        when(retryService.manualRetry(999L))
                .thenThrow(new RuntimeException("执行记录不存在: 999"));

        mockMvc.perform(post("/api/executions/999/retry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("执行记录不存在: 999"));

        verify(retryService).manualRetry(999L);
    }
}
