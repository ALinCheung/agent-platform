package com.agentplatform.web.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE控制器
 * 提供实时状态更新
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 客户端订阅SSE
     */
    @GetMapping(value = "/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.info("SSE客户端连接: 当前{}个连接", emitters.size());
        return emitter;
    }

    /**
     * 向所有客户端推送状态更新
     */
    public void broadcast(String eventType, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(data));
            } catch (IOException e) {
                log.warn("SSE推送失败: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
