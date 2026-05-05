package com.agentplatform.executor.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 异常处理器
 * 处理进程异常、资源不足、CLI不可用等异常情况
 */
@Slf4j
@Service
public class ExceptionHandler {

    /**
     * 处理进程异常
     */
    public String handleProcessException(Exception e) {
        String message = e.getMessage();

        if (message != null && message.contains("Cannot run program")) {
            log.error("CLI不可用: {}", message);
            return "Claude CLI不可用，请检查是否已安装";
        }

        if (message != null && message.contains("OutOfMemoryError")) {
            log.error("内存不足: {}", message);
            return "系统内存不足，请稍后重试";
        }

        if (message != null && message.contains("No space left")) {
            log.error("磁盘空间不足: {}", message);
            return "磁盘空间不足，请清理磁盘空间";
        }

        log.error("执行异常: {}", message, e);
        return "执行异常: " + message;
    }

    /**
     * 判断是否为资源不足异常
     */
    public boolean isResourceException(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;

        return message.contains("OutOfMemoryError") ||
               message.contains("No space left") ||
               message.contains("Cannot allocate memory");
    }

    /**
     * 判断是否为CLI不可用异常
     */
    public boolean isCliUnavailableException(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("Cannot run program") ||
                message.contains("No such file or directory") ||
                message.contains("not found")
        );
    }
}
