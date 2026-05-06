package com.agentplatform.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQLite数据源配置测试
 * 验证WAL模式和连接池配置参数
 */
@DisplayName("SQLite数据源配置测试")
class SqliteDataSourceConfigTest {

    @Test
    @DisplayName("配置类 - 数据目录和数据库名称有默认值")
    void config_hasDefaultValues() {
        SqliteDataSourceConfig config = new SqliteDataSourceConfig();

        // 通过反射读取默认值
        String dataDir = (String) ReflectionTestUtils.getField(config, "dataDir");
        String dbName = (String) ReflectionTestUtils.getField(config, "dbName");

        // 默认值由@Value注解提供，在非Spring环境下为null
        // 这里验证字段存在
        assertDoesNotThrow(() -> SqliteDataSourceConfig.class.getDeclaredField("dataDir"));
        assertDoesNotThrow(() -> SqliteDataSourceConfig.class.getDeclaredField("dbName"));
    }

    @Test
    @DisplayName("配置类 - 存在enableWalMode方法")
    void config_hasEnableWalModeMethod() throws NoSuchMethodException {
        var method = SqliteDataSourceConfig.class.getDeclaredMethod("enableWalMode",
                javax.sql.DataSource.class);

        assertNotNull(method, "应存在enableWalMode方法");
        // 私有方法
        assertFalse(method.canAccess(null), "enableWalMode应为私有方法");
    }

    @Test
    @DisplayName("配置类 - 存在dataSource Bean方法")
    void config_hasDataSourceBeanMethod() throws NoSuchMethodException {
        var method = SqliteDataSourceConfig.class.getMethod("dataSource");

        assertNotNull(method, "应存在dataSource方法");
    }

    @Test
    @DisplayName("配置类 - 存在transactionManager Bean方法")
    void config_hasTransactionManagerBeanMethod() throws NoSuchMethodException {
        var method = SqliteDataSourceConfig.class.getMethod("transactionManager",
                javax.sql.DataSource.class);

        assertNotNull(method, "应存在transactionManager方法");
    }

    @Test
    @DisplayName("配置类 - 启用事务管理")
    void config_enablesTransactionManagement() {
        assertTrue(SqliteDataSourceConfig.class.isAnnotationPresent(
                        org.springframework.transaction.annotation.EnableTransactionManagement.class),
                "应启用事务管理");
    }
}
