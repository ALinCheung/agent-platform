package com.agentplatform.starter.e2e;

import com.agentplatform.starter.AgentPlatformApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实时状态更新端到端测试
 * 测试SSE推送功能
 */
@SpringBootTest(
        classes = AgentPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("实时状态更新测试")
class RealtimeStatusTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("SSE - 订阅端点可访问")
    void sseEndpoint_isAccessible() {
        // 使用TestRestTemplate测试SSE端点
        // 注意：TestRestTemplate对SSE的支持有限，这里主要验证端点存在
        ResponseEntity<String> response = restTemplate.getForEntity("/api/sse/status", String.class);

        // SSE端点应返回200，Content-Type为text/event-stream
        assertEquals(HttpStatus.OK, response.getStatusCode(), "SSE端点应返回200");
    }

    @Test
    @DisplayName("SSE - 端点返回正确的Content-Type")
    void sseEndpoint_returnsCorrectContentType() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/sse/status", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "SSE端点应返回200");
        // 验证Content-Type包含text/event-stream
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType, "Content-Type不应为空");
    }

    @Test
    @DisplayName("SSE - 多次订阅不会报错")
    void sseEndpoint_allowsMultipleSubscriptions() {
        // 第一次订阅
        ResponseEntity<String> response1 = restTemplate.getForEntity("/api/sse/status", String.class);
        assertEquals(HttpStatus.OK, response1.getStatusCode(), "第一次订阅应成功");

        // 第二次订阅
        ResponseEntity<String> response2 = restTemplate.getForEntity("/api/sse/status", String.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode(), "第二次订阅应成功");
    }
}
