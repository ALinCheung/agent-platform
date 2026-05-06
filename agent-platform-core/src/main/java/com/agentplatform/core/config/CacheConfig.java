package com.agentplatform.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置
 * 使用简单的ConcurrentMapCacheManager，适合单机场景
 * 缓存统计数据，避免频繁查询数据库
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     * 统计数据缓存5分钟过期（通过定时清理实现）
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "statsOverview",
                "taskStats",
                "executionStats",
                "performanceStats",
                "historyTrend"
        );
    }
}
