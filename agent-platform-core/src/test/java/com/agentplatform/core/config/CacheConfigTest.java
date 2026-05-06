package com.agentplatform.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存配置测试
 * 验证CacheManager正确配置了所有缓存名称
 */
class CacheConfigTest {

    @Test
    @DisplayName("CacheManager - 创建ConcurrentMapCacheManager实例")
    void cacheManager_createsConcurrentMapCacheManager() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();

        assertNotNull(cacheManager, "CacheManager不应为空");
        assertInstanceOf(ConcurrentMapCacheManager.class, cacheManager,
                "应为ConcurrentMapCacheManager类型");
    }

    @Test
    @DisplayName("CacheManager - 包含所有预定义缓存名称")
    void cacheManager_containsAllCacheNames() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();

        // 获取缓存实例，验证缓存名称存在
        assertNotNull(cacheManager.getCache("statsOverview"), "应包含statsOverview缓存");
        assertNotNull(cacheManager.getCache("taskStats"), "应包含taskStats缓存");
        assertNotNull(cacheManager.getCache("executionStats"), "应包含executionStats缓存");
        assertNotNull(cacheManager.getCache("performanceStats"), "应包含performanceStats缓存");
        assertNotNull(cacheManager.getCache("historyTrend"), "应包含historyTrend缓存");
    }

    @Test
    @DisplayName("CacheManager - 缓存可以存取数据")
    void cacheManager_canStoreAndRetrieveData() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();

        // 测试缓存读写
        var cache = cacheManager.getCache("statsOverview");
        assertNotNull(cache, "缓存不应为空");

        cache.put("testKey", "testValue");
        var value = cache.get("testKey");
        assertNotNull(value, "缓存值不应为空");
        assertEquals("testValue", value.get(), "缓存值应匹配");
    }

    @Test
    @DisplayName("CacheManager - 获取不存在的缓存返回null")
    void cacheManager_returnsNull_forNonExistentCache() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();

        assertNull(cacheManager.getCache("nonExistentCache"), "不存在的缓存应返回null");
    }
}
