package com.agentplatform.core.service;

import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 任务版本服务接口
 */
public interface TaskVersionService extends IService<TaskVersion> {

    /**
     * 保存任务版本
     */
    void saveVersion(Task task, ChangeType changeType, String description);

    /**
     * 获取任务的版本历史
     */
    List<TaskVersion> getVersions(Long taskId);

    /**
     * 获取指定版本
     */
    TaskVersion getVersion(Long taskId, int version);

    /**
     * 获取最新版本号
     */
    int getMaxVersion(Long taskId);
}
