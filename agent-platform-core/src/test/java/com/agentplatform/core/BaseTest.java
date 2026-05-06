package com.agentplatform.core;

import com.agentplatform.core.config.TestDataSourceConfig;
import com.agentplatform.core.config.TestMyBatisPlusConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基类 - 提供通用测试配置
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestDataSourceConfig.class, TestMyBatisPlusConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public abstract class BaseTest {
}
