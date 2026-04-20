package com.gaokao.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI测试配置
 * 提供AI模型服务用于测试用例生成和分析
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "test.ai")
public class TestAiConfig {

    /**
     * 语义断言默认阈值
     */
    private Double defaultSemanticThreshold = 0.85;

    /**
     * 测试用例生成并发数
     */
    private Integer generatorConcurrency = 3;
}