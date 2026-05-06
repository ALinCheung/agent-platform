package com.agentplatform.web.controller;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.ValidationResult;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskValidator;
import com.agentplatform.executor.service.TaskExecutionQueue;
import com.agentplatform.scheduler.service.SchedulerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 任务控制器单元测试
 * 测试任务CRUD、启用/禁用等功能
 */
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private TaskValidator taskValidator;

    @MockBean
    private TaskExecutionQueue taskExecutionQueue;

    @MockBean
    private SchedulerService schedulerService;

    /** 构建测试用任务对象 */
    private Task buildTestTask() {
        return Task.builder()
                .id(1L).name("test-task").command("echo test")
                .triggerType(TriggerType.CRON).cronExpression("0 0 * * * ?")
                .timeoutSeconds(300).maxRetries(0).retryIntervalSeconds(60)
                .enabled(true).successCount(0).failureCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 测试获取任务列表返回200
     */
    @Test
    @DisplayName("获取任务列表返回200和任务列表")
    void listTasks_returns200_withTaskList() throws Exception {
        Task task = buildTestTask();
        when(taskService.listAll()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("test-task"))
                .andExpect(jsonPath("$[0].command").value("echo test"));

        verify(taskService).listAll();
    }

    /**
     * 测试获取单个任务返回200
     */
    @Test
    @DisplayName("获取已存在的任务返回200")
    void getTask_returns200_forExistingTask() throws Exception {
        Task task = buildTestTask();
        when(taskService.getById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test-task"));

        verify(taskService).getById(1L);
    }

    /**
     * 测试获取不存在的任务返回404
     */
    @Test
    @DisplayName("获取不存在的任务返回404")
    void getTask_returns404_forNonExistentTask() throws Exception {
        when(taskService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());

        verify(taskService).getById(999L);
    }

    /**
     * 测试创建有效任务返回201
     */
    @Test
    @DisplayName("创建有效任务返回201")
    void createTask_returns201_forValidTask() throws Exception {
        Task task = buildTestTask();
        task.setId(null); // 新任务没有ID

        when(taskValidator.validate(any(Task.class))).thenReturn(ValidationResult.success());
        when(taskService.createTask(any(Task.class))).thenReturn(task);

        String json = objectMapper.writeValueAsString(task);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test-task"));

        verify(taskValidator).validate(any(Task.class));
        verify(taskService).createTask(any(Task.class));
        verify(schedulerService).registerTask(any(Task.class));
    }

    /**
     * 测试创建无效任务返回400
     */
    @Test
    @DisplayName("创建无效任务返回400")
    void createTask_returns400_forInvalidTask() throws Exception {
        Task task = buildTestTask();
        task.setName(""); // 无效名称

        when(taskValidator.validate(any(Task.class)))
                .thenReturn(ValidationResult.failure("任务名称不能为空"));

        String json = objectMapper.writeValueAsString(task);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]").value("任务名称不能为空"));

        verify(taskValidator).validate(any(Task.class));
        verify(taskService, never()).createTask(any());
    }

    /**
     * 测试删除已存在的任务返回204
     */
    @Test
    @DisplayName("删除已存在的任务返回204")
    void deleteTask_returns204_forExistingTask() throws Exception {
        Task task = buildTestTask();
        when(taskService.getById(1L)).thenReturn(task);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(schedulerService).cancelTask(1L);
        verify(taskService).removeById(1L);
    }

    /**
     * 测试删除不存在的任务返回404
     */
    @Test
    @DisplayName("删除不存在的任务返回404")
    void deleteTask_returns404_forNonExistentTask() throws Exception {
        when(taskService.getById(999L)).thenReturn(null);

        mockMvc.perform(delete("/api/tasks/999"))
                .andExpect(status().isNotFound());

        verify(taskService, never()).removeById(any());
    }

    /**
     * 测试启用任务返回200
     */
    @Test
    @DisplayName("启用任务返回200")
    void enableTask_returns200() throws Exception {
        Task task = buildTestTask();
        when(taskService.getById(1L)).thenReturn(task);

        mockMvc.perform(post("/api/tasks/1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("任务已启用"));

        verify(taskService).enableTask(1L);
        verify(schedulerService).reRegisterTask(any(Task.class));
    }

    /**
     * 测试禁用任务返回200
     */
    @Test
    @DisplayName("禁用任务返回200")
    void disableTask_returns200() throws Exception {
        mockMvc.perform(post("/api/tasks/1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("任务已禁用"));

        verify(taskService).disableTask(1L);
        verify(schedulerService).cancelTask(1L);
    }
}
