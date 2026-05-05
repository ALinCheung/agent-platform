package com.agentplatform.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite数据源配置
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class SqliteDataSourceConfig {

    @Value("${app.data-dir:./data}")
    private String dataDir;

    @Value("${app.db-name:agent-platform.db}")
    private String dbName;

    @Bean
    public DataSource dataSource() {
        // 确保数据目录存在
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("创建数据目录: {}", dir.getAbsolutePath());
        }

        String dbPath = new File(dir, dbName).getAbsolutePath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;

        log.info("SQLite数据库路径: {}", dbPath);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        // SQLite连接池配置
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");

        HikariDataSource dataSource = new HikariDataSource(config);

        // 启用WAL模式
        enableWalMode(dataSource);

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 启用SQLite WAL模式以支持并发读写
     */
    private void enableWalMode(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA synchronous=NORMAL");
            log.info("SQLite WAL模式已启用");
        } catch (SQLException e) {
            log.error("启用WAL模式失败", e);
        }
    }
}
