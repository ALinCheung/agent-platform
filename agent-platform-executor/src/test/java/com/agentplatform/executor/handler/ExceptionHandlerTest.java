package com.agentplatform.executor.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExceptionHandler 单元测试
 * 测试异常处理器的异常分类和消息转换逻辑
 */
@DisplayName("异常处理器测试")
class ExceptionHandlerTest {

    private ExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new ExceptionHandler();
    }

    @Test
    @DisplayName("handleProcessException - Cannot run program返回CLI不可用消息")
    void handleProcessException_returnsCliMessage_forCannotRunProgram() {
        Exception e = new RuntimeException("Cannot run program \"claude\": error=2, No such file or directory");

        String message = exceptionHandler.handleProcessException(e);

        assertEquals("Claude CLI不可用，请检查是否已安装", message,
                "包含'Cannot run program'的异常应返回CLI不可用提示");
    }

    @Test
    @DisplayName("handleProcessException - OutOfMemoryError返回内存不足消息")
    void handleProcessException_returnsMemoryMessage_forOutOfMemoryError() {
        Exception e = new RuntimeException("java.lang.OutOfMemoryError: Java heap space");

        String message = exceptionHandler.handleProcessException(e);

        assertEquals("系统内存不足，请稍后重试", message,
                "包含'OutOfMemoryError'的异常应返回内存不足提示");
    }

    @Test
    @DisplayName("handleProcessException - No space left返回磁盘空间不足消息")
    void handleProcessException_returnsDiskMessage_forNoSpaceLeft() {
        Exception e = new RuntimeException("No space left on device");

        String message = exceptionHandler.handleProcessException(e);

        assertEquals("磁盘空间不足，请清理磁盘空间", message,
                "包含'No space left'的异常应返回磁盘空间不足提示");
    }

    @Test
    @DisplayName("handleProcessException - 其他异常返回通用消息")
    void handleProcessException_returnsGenericMessage_forOtherExceptions() {
        Exception e = new RuntimeException("Something went wrong");

        String message = exceptionHandler.handleProcessException(e);

        assertEquals("执行异常: Something went wrong", message,
                "其他异常应返回包含原始消息的通用提示");
    }

    @Test
    @DisplayName("isResourceException - OutOfMemoryError返回true")
    void isResourceException_returnsTrue_forOutOfMemoryError() {
        Exception e = new RuntimeException("java.lang.OutOfMemoryError: GC overhead limit exceeded");

        assertTrue(exceptionHandler.isResourceException(e),
                "OutOfMemoryError应判定为资源异常");
    }

    @Test
    @DisplayName("isResourceException - No space left返回true")
    void isResourceException_returnsTrue_forNoSpaceLeft() {
        Exception e = new RuntimeException("No space left on device");

        assertTrue(exceptionHandler.isResourceException(e),
                "No space left应判定为资源异常");
    }

    @Test
    @DisplayName("isResourceException - 通用异常返回false")
    void isResourceException_returnsFalse_forGenericException() {
        Exception e = new RuntimeException("Some generic error");

        assertFalse(exceptionHandler.isResourceException(e),
                "通用异常不应判定为资源异常");
    }

    @Test
    @DisplayName("isCliUnavailableException - Cannot run program返回true")
    void isCliUnavailableException_returnsTrue_forCannotRunProgram() {
        Exception e = new RuntimeException("Cannot run program \"claude\"");

        assertTrue(exceptionHandler.isCliUnavailableException(e),
                "Cannot run program应判定为CLI不可用");
    }

    @Test
    @DisplayName("isCliUnavailableException - No such file or directory返回true")
    void isCliUnavailableException_returnsTrue_forNoSuchFileOrDirectory() {
        Exception e = new RuntimeException("No such file or directory");

        assertTrue(exceptionHandler.isCliUnavailableException(e),
                "No such file or directory应判定为CLI不可用");
    }

    @Test
    @DisplayName("isCliUnavailableException - 通用异常返回false")
    void isCliUnavailableException_returnsFalse_forGenericException() {
        Exception e = new RuntimeException("Connection timeout");

        assertFalse(exceptionHandler.isCliUnavailableException(e),
                "通用异常不应判定为CLI不可用");
    }
}
