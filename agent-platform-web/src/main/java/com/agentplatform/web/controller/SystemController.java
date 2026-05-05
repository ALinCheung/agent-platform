package com.agentplatform.web.controller;

import com.agentplatform.core.service.impl.SqliteDataSourceConfig;
import com.agentplatform.executor.monitor.ResourceMonitor;
import com.agentplatform.executor.service.ClaudeExecutorService;
import com.agentplatform.scheduler.service.CliHealthChecker;
import com.agentplatform.scheduler.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 系统管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final ResourceMonitor resourceMonitor;
    private final SchedulerService schedulerService;
    private final CliHealthChecker cliHealthChecker;

    @Value("${app.data-dir:./data}")
    private String dataDir;

    @Value("${app.db-name:agent-platform.db}")
    private String dbName;

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "cliAvailable", cliHealthChecker.isCliAvailable(),
                "scheduledTasks", schedulerService.getRegisteredCount(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 系统资源使用情况
     */
    @GetMapping("/resources")
    public ResponseEntity<ResourceMonitor.ResourceInfo> resources() {
        return ResponseEntity.ok(resourceMonitor.getResourceInfo());
    }

    /**
     * 备份数据库
     */
    @PostMapping("/backup")
    public ResponseEntity<?> backup() {
        try {
            Path dbPath = Path.of(dataDir, dbName);
            if (!Files.exists(dbPath)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "数据库文件不存在"));
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = dbName.replace(".db", "_backup_" + timestamp + ".db");
            Path backupPath = Path.of(dataDir, backupName);

            Files.copy(dbPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("数据库备份完成: {}", backupPath);
            return ResponseEntity.ok(Map.of(
                    "message", "备份成功",
                    "path", backupPath.toString()
            ));
        } catch (IOException e) {
            log.error("数据库备份失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "备份失败: " + e.getMessage()));
        }
    }
}
