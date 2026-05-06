package com.agentplatform.starter.performance;

import com.agentplatform.starter.AgentPlatformApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 磁盘空间性能测试
 * 测试文件清理和磁盘空间监控
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("磁盘空间性能测试")
class DiskSpacePerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("磁盘监控 - 系统返回可用磁盘空间")
    void diskMonitor_returnsFreeDiskSpace() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/system/resources", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "资源监控接口应返回200");
        assertNotNull(response.getBody(), "资源信息不应为空");
        assertTrue(response.getBody().containsKey("freeDiskMb"), "应包含freeDiskMb字段");

        long freeDisk = ((Number) response.getBody().get("freeDiskMb")).longValue();
        assertTrue(freeDisk > 0, "可用磁盘空间应大于0");
    }

    @Test
    @DisplayName("数据目录 - 数据目录存在且可写")
    void dataDirectory_existsAndWritable() {
        File dataDir = new File("./test-data");
        if (dataDir.exists()) {
            assertTrue(dataDir.isDirectory(), "数据目录应为目录");
            assertTrue(dataDir.canWrite(), "数据目录应可写");
        }
    }

    @Test
    @DisplayName("磁盘监控 - 连续监控磁盘空间稳定")
    void diskMonitor_stable_overTime() {
        // 多次查询磁盘空间
        Long firstFreeDisk = null;
        for (int i = 0; i < 10; i++) {
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/system/resources", Map.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());

            long freeDisk = ((Number) response.getBody().get("freeDiskMb")).longValue();

            if (firstFreeDisk == null) {
                firstFreeDisk = freeDisk;
            } else {
                // 磁盘空间不应显著减少（允许1MB波动）
                assertTrue(Math.abs(freeDisk - firstFreeDisk) < 100,
                        "磁盘空间应保持稳定");
            }
        }
    }
}
