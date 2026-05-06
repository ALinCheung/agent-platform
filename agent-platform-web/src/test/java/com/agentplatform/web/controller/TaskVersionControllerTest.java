package com.agentplatform.web.controller;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.enums.TriggerType;
import com.agentplatform.core.service.TaskVersionService;
import com.agentplatform.core.service.impl.TaskRollbackServiceImpl;
import com.agentplatform.scheduler.service.SchedulerService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 任务版本控制器单元测试
 * 测试版本历史查询、回滚等功能
 */
@WebMvcTest(TaskVersionController.class)
class TaskVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskVersionService taskVersionService;

    @MockBean
    private TaskRollbackServiceImpl taskRollbackService;

    @MockBean
    private SchedulerService schedulerService;

    /** 构建测试用版本对象 */
    private TaskVersion buildTestVersion() {
        return TaskVersion.builder()
                .id(1L).taskId(1L).version(1)
                .command("echo test").cronExpression("0 0 * * * ?")
                .timeoutSeconds(300).maxRetries(0).retryIntervalSeconds(60)
                .changeType(ChangeType.CREATE).changeDescription("初始创建")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 测试获取版本列表返回200
     */
    @Test
    @DisplayName("获取版本历史返回200和版本列表")
    void getVersions_returns200_withVersionList() throws Exception {
        TaskVersion v1 = buildTestVersion();
        TaskVersion v2 = TaskVersion.builder()
                .id(2L).taskId(1L).version(2)
                .command("echo updated").cronExpression("0 0 * * * ?")
                .timeoutSeconds(600).maxRetries(1).retryIntervalSeconds(120)
                .changeType(ChangeType.UPDATE).changeDescription("更新命令")
                .createdAt(LocalDateTime.now())
                .build();

        when(taskVersionService.getVersions(1L)).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/tasks/1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].changeType").value("CREATE"))
                .andExpect(jsonPath("$[1].version").value(2))
                .andExpect(jsonPath("$[1].changeType").value("UPDATE"));

        verify(taskVersionService).getVersions(1L);
    }

    /**
     * 测试回滚成功返回200
     */
    @Test
    @DisplayName("回滚到指定版本成功返回200")
    void rollback_returns200_onSuccess() throws Exception {
        Task rolledBackTask = Task.builder()
                .id(1L).name("test-task").command("echo test")
                .triggerType(TriggerType.CRON).cronExpression("0 0 * * * ?")
                .timeoutSeconds(300).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskRollbackService.rollback(1L, 1)).thenReturn(rolledBackTask);

        mockMvc.perform(post("/api/tasks/1/versions/rollback")
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("回滚成功"))
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.version").value(1));

        verify(taskRollbackService).rollback(1L, 1);
        verify(schedulerService).reRegisterTask(any(Task.class));
    }

    /**
     * 测试回滚到不存在的版本返回400
     */
    @Test
    @DisplayName("回滚到不存在的版本返回400")
    void rollback_returns400_forNonExistentVersion() throws Exception {
        when(taskRollbackService.rollback(1L, 999))
                .thenThrow(new RuntimeException("版本不存在: taskId=1, version=999"));

        mockMvc.perform(post("/api/tasks/1/versions/rollback")
                        .param("version", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("版本不存在: taskId=1, version=999"));

        verify(taskRollbackService).rollback(1L, 999);
        verify(schedulerService, never()).reRegisterTask(any());
    }
}
