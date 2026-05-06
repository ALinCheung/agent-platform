package com.agentplatform.core.service.impl;

import com.agentplatform.core.BaseTest;
import com.agentplatform.core.TestUtils;
import com.agentplatform.core.entity.Task;
import com.agentplatform.core.entity.TaskVersion;
import com.agentplatform.core.enums.ChangeType;
import com.agentplatform.core.service.TaskService;
import com.agentplatform.core.service.TaskVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskVersionServiceImpl 单元测试
 * 测试任务版本服务的保存、查询和版本号管理功能
 */
@DisplayName("任务版本服务实现测试")
class TaskVersionServiceImplTest extends BaseTest {

    @Autowired
    private TaskVersionService taskVersionService;

    @Autowired
    private TaskService taskService;

    /** 测试用任务 */
    private Task testTask;

    @BeforeEach
    void setUp() {
        // 创建测试任务（createTask内部会自动创建初始版本）
        Task task = TestUtils.createTestTask("版本测试任务", "echo version test");
        testTask = taskService.createTask(task);
    }

    @Test
    @DisplayName("saveVersion - 创建版本并分配正确的版本号")
    void saveVersion_createsVersionWithCorrectNumber() {
        // getVersions应至少包含createTask创建的初始版本
        List<TaskVersion> versions = taskVersionService.getVersions(testTask.getId());

        assertFalse(versions.isEmpty(), "应至少有初始版本");
        // 初始版本号应为1
        assertEquals(1, versions.get(versions.size() - 1).getVersion(),
                "初始版本号应为1");
        assertEquals(ChangeType.CREATE, versions.get(versions.size() - 1).getChangeType());
    }

    @Test
    @DisplayName("saveVersion - 版本号自动递增")
    void saveVersion_autoIncrementsVersionNumber() {
        // 初始版本（由createTask创建）版本号为1
        // 再手动保存一个版本
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "手动保存第二个版本");

        // 再保存一个
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "手动保存第三个版本");

        // 查询所有版本
        List<TaskVersion> versions = taskVersionService.getVersions(testTask.getId());

        // 应有3个版本（1个初始 + 2个手动）
        assertEquals(3, versions.size(), "应有3个版本");

        // 验证版本号递增（getVersions按version desc排序）
        assertEquals(3, versions.get(0).getVersion(), "最新版本号应为3");
        assertEquals(2, versions.get(1).getVersion(), "第二版本号应为2");
        assertEquals(1, versions.get(2).getVersion(), "初始版本号应为1");
    }

    @Test
    @DisplayName("getVersions - 返回按版本号降序排列的版本列表")
    void getVersions_returnsVersionsOrderedDesc() {
        // 再创建几个版本
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "版本2");
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "版本3");

        List<TaskVersion> versions = taskVersionService.getVersions(testTask.getId());

        // 验证降序排列
        assertTrue(versions.size() >= 3, "应至少有3个版本");
        for (int i = 0; i < versions.size() - 1; i++) {
            assertTrue(versions.get(i).getVersion() >= versions.get(i + 1).getVersion(),
                    "版本号应按降序排列: v" + versions.get(i).getVersion()
                            + " >= v" + versions.get(i + 1).getVersion());
        }
    }

    @Test
    @DisplayName("getVersion - 返回指定版本号的版本")
    void getVersion_returnsCorrectSpecificVersion() {
        // 再创建一个版本（带不同的command）
        testTask.setCommand("echo updated command");
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "更新命令");

        // 查询版本1（初始版本）
        TaskVersion version1 = taskVersionService.getVersion(testTask.getId(), 1);
        assertNotNull(version1, "应能找到版本1");
        assertEquals(1, version1.getVersion());
        assertEquals(ChangeType.CREATE, version1.getChangeType());

        // 查询版本2（更新版本）
        TaskVersion version2 = taskVersionService.getVersion(testTask.getId(), 2);
        assertNotNull(version2, "应能找到版本2");
        assertEquals(2, version2.getVersion());
        assertEquals(ChangeType.UPDATE, version2.getChangeType());
        assertEquals("echo updated command", version2.getCommand());
    }

    @Test
    @DisplayName("getVersion - 不存在的版本号返回null")
    void getVersion_returnsNullForNonExistentVersion() {
        TaskVersion result = taskVersionService.getVersion(testTask.getId(), 999);

        assertNull(result, "不存在的版本号应返回null");
    }

    @Test
    @DisplayName("getMaxVersion - 无版本的任务返回0")
    void getMaxVersion_returnsZeroForTaskWithNoVersions() {
        // 创建一个新任务（不通过createTask，避免自动创建版本）
        // 直接查询一个没有版本的任务ID
        // 使用一个不存在但有效的任务ID
        Long noVersionTaskId = 88888L;

        int maxVersion = taskVersionService.getMaxVersion(noVersionTaskId);

        assertEquals(0, maxVersion, "无版本的任务最大版本号应为0");
    }

    @Test
    @DisplayName("getMaxVersion - 返回正确的最大版本号")
    void getMaxVersion_returnsCorrectMax() {
        // 初始版本由createTask创建，版本号为1
        assertEquals(1, taskVersionService.getMaxVersion(testTask.getId()),
                "创建后最大版本号应为1");

        // 再保存两个版本
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "版本2");
        taskVersionService.saveVersion(testTask, ChangeType.UPDATE, "版本3");

        assertEquals(3, taskVersionService.getMaxVersion(testTask.getId()),
                "保存3个版本后最大版本号应为3");
    }
}
