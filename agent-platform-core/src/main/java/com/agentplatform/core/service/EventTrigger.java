package com.agentplatform.core.service;

/**
 * 事件触发器接口
 */
public interface EventTrigger {

    /**
     * 触发事件
     * @param eventType 事件类型
     * @param payload 事件数据
     */
    void trigger(String eventType, String payload);
}
