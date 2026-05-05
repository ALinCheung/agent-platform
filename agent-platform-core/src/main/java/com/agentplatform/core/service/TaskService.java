package com.agentplatform.core.service;

import com.agentplatform.core.entity.Task;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService extends IService<Task> {

    Task createTask(Task task);

    Task updateTask(Long id, Task task);

    void enableTask(Long id);

    void disableTask(Long id);

    List<Task> findEnabledCronTasks();

    Task findByWebhookPath(String path);

    List<Task> listAll();
}
