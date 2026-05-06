package com.agentplatform.starter.e2e;

/**
 * Playwright测试配置
 * 定义测试环境参数和超时设置
 */
public class PlaywrightConfig {

    /** 应用基础URL */
    public static final String BASE_URL = "http://localhost:8080";

    /** 页面加载超时（毫秒） */
    public static final int PAGE_TIMEOUT = 30000;

    /** 元素等待超时（毫秒） */
    public static final int ELEMENT_TIMEOUT = 10000;

    /** 导航超时（毫秒） */
    public static final int NAVIGATION_TIMEOUT = 15000;

    /** 测试数据前缀 */
    public static final String TEST_DATA_PREFIX = "pw-test-";

    /** 截图目录 */
    public static final String SCREENSHOT_DIR = "target/screenshots";

    /** 是否在失败时截图 */
    public static final boolean SCREENSHOT_ON_FAILURE = true;
}
