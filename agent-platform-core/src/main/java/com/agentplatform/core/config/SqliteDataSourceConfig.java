package com.agentplatform.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * SQLite数据源配置
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@Profile("!test")
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

        // 初始化数据库表结构
        initializeSchema(dataSource);

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

    /**
     * 初始化数据库表结构
     */
    private void initializeSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            // 检查是否需要初始化（查询表是否存在）
            boolean needsInit = true;
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='task'");
                needsInit = !rs.next();
                rs.close();
            }

            if (needsInit) {
                log.info("检测到空数据库，开始初始化表结构...");
                executeSchemaScript(conn, "db/migration/V1__init_schema.sql");
                executeSchemaScript(conn, "db/migration/V2__add_statistics_views.sql");
                executeSchemaScript(conn, "db/migration/V3__add_subtask_table.sql");
                executeSchemaScript(conn, "db/migration/V4__add_execution_log_table.sql");
                executeSchemaScript(conn, "db/migration/V5__update_execution_status_check.sql");
                log.info("数据库表结构初始化完成");
            } else {
                log.info("数据库已存在，跳过初始化");
            }
        } catch (SQLException e) {
            log.error("检查数据库状态失败", e);
        }
    }

    /**
     * 执行SQL schema脚本
     */
    private void executeSchemaScript(Connection conn, String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("找不到schema脚本: {}", resourcePath);
                return;
            }
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            log.debug("SQL脚本内容长度: {} 字符", sql.length());

            // 禁用外键检查，避免引用表不存在的问题
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF");
            }

            int stmtCount = 0;

            // 使用事务执行所有语句
            conn.setAutoCommit(false);
            try {
                // 分割SQL语句，但正确处理触发器等复杂语句
                int i = 0;
                while (i < sql.length()) {
                    // 跳过空白和注释
                    while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) i++;
                    if (i >= sql.length()) break;
                    if (sql.charAt(i) == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                        // 单行注释
                        while (i < sql.length() && sql.charAt(i) != '\n') i++;
                        continue;
                    }

                    // 检查是否是 CREATE TRIGGER 语句
                    if (sql.substring(i).toUpperCase().startsWith("CREATE TRIGGER")) {
                        // 找到 END; 作为触发器结束
                        int endIdx = sql.indexOf("END;", i);
                        if (endIdx != -1) {
                            String stmt = sql.substring(i, endIdx + 4).trim();
                            log.debug("执行触发器: {}...", stmt.substring(0, Math.min(50, stmt.length())));
                            try (Statement st = conn.createStatement()) {
                                st.execute(stmt);
                                stmtCount++;
                            }
                            i = endIdx + 4;
                            continue;
                        }
                    }

                    // 普通语句，找分号结束
                    int semiIdx = sql.indexOf(';', i);
                    if (semiIdx == -1) {
                        // 没有分号，执行到结尾
                        String stmt = sql.substring(i).trim();
                        if (!stmt.isEmpty()) {
                            log.debug("执行SQL: {}...", stmt.substring(0, Math.min(80, stmt.length())));
                            try (Statement st = conn.createStatement()) {
                                st.execute(stmt);
                                stmtCount++;
                            }
                        }
                        break;
                    } else {
                        String stmt = sql.substring(i, semiIdx + 1).trim();
                        if (!stmt.isEmpty()) {
                            log.debug("执行SQL: {}...", stmt.substring(0, Math.min(80, stmt.length())));
                            try (Statement st = conn.createStatement()) {
                                st.execute(stmt);
                                stmtCount++;
                            }
                        }
                        i = semiIdx + 1;
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            // 重新启用外键检查
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            log.info("已执行schema脚本: {} ({}条语句)", resourcePath, stmtCount);
        } catch (IOException | SQLException e) {
            log.error("执行schema脚本失败: {}", resourcePath, e);
        }
    }
}
