package com.agentplatform.core.mapper;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskVersionMapper 数据访问层测试
 * 测试任务版本相关的自定义SQL查询方法
 */
@DisplayName("TaskVersionMapper 测试")
class TaskVersionMapperTest extends BaseTest {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskVersionMapper taskVersionMapper;

    /**
     * 测试获取无版本记录任务的最大版本号
     * 验证当任务没有任何版本记录时，返回0
     */
    @Test
    @DisplayName("getMaxVersion - 无版本记录时返回0")
    void testGetMaxVersionNoVersions() {
        // 创建一个任务，但不创建任何版本
        Task task = TestUtils.createTestTask("no-version-task", "echo test");
        task.setId(null);
        taskMapper.insert(task);

        // 查询最大版本号，应返回0
        int maxVersion = taskVersionMapper.getMaxVersion(task.getId());
        assertEquals(0, maxVersion);
    }

    /**
     * 测试获取有单个版本记录任务的最大版本号
     * 验证正确返回唯一的版本号
     */
    @Test
    @DisplayName("getMaxVersion - 单个版本时返回正确版本号")
    void testGetMaxVersionSingleVersion() {
        // 创建任务
        Task task = TestUtils.createTestTask("single-version-task", "echo test");
        task.setId(null);
        taskMapper.insert(task);

        // 创建版本1
        TaskVersion version1 = TestUtils.createTestTaskVersion(task.getId(), 1, "echo v1");
        version1.setId(null);
        taskVersionMapper.insert(version1);

        // 查询最大版本号，应返回1
        int maxVersion = taskVersionMapper.getMaxVersion(task.getId());
        assertEquals(1, maxVersion);
    }

    /**
     * 测试获取有多个版本记录任务的最大版本号
     * 验证正确返回最大版本号
     */
    @Test
    @DisplayName("getMaxVersion - 多个版本时返回最大版本号")
    void testGetMaxVersionMultipleVersions() {
        // 创建任务
        Task task = TestUtils.createTestTask("multi-version-task", "echo test");
        task.setId(null);
        taskMapper.insert(task);

        // 创建版本1
        TaskVersion version1 = TestUtils.createTestTaskVersion(task.getId(), 1, "echo v1");
        version1.setId(null);
        taskVersionMapper.insert(version1);

        // 创建版本2
        TaskVersion version2 = TestUtils.createTestTaskVersion(task.getId(), 2, "echo v2");
        version2.setId(null);
        taskVersionMapper.insert(version2);

        // 创建版本3
        TaskVersion version3 = TestUtils.createTestTaskVersion(task.getId(), 3, "echo v3");
        version3.setId(null);
        taskVersionMapper.insert(version3);

        // 查询最大版本号，应返回3
        int maxVersion = taskVersionMapper.getMaxVersion(task.getId());
        assertEquals(3, maxVersion);
    }
}
