package com.gaokao.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置类
 * 支持内存存储、Elasticsearch 两种模式
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.vectorstore")
public class VectorStoreConfig {

    private String table = "gaokao_embeddings";
    private int dimension = 1536;
    private boolean useIndex = true;
    private String indexListSize = "100";
    private boolean createTable = true;

    @Value("${langchain4j.vectorstore.elasticsearch.index:gaokao_embeddings}")
    private String esIndex;

    @Value("${elasticsearch.host:localhost}")
    private String esHost;

    @Value("${elasticsearch.port:9200}")
    private int esPort;

    @Value("${elasticsearch.username:}")
    private String esUsername;

    @Value("${elasticsearch.password:}")
    private String esPassword;

    @Value("${elasticsearch.use-ssl:false}")
    private boolean esUseSsl;

    /**
     * 内存向量存储（备用）
     * 适用于开发测试环境
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "memory")
    public EmbeddingStore<TextSegment> inMemoryEmbeddingStore() {
        log.info("使用内存向量存储");
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Elasticsearch 向量存储
     * 适用于生产环境，支持大规模向量检索
     * 需要配置 elasticsearch.* 和 langchain4j.vectorstore.type=elasticsearch
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "elasticsearch")
    public EmbeddingStore<TextSegment> elasticsearchEmbeddingStore() {
        log.info("使用 Elasticsearch 向量存储, index={}, dimension={}", esIndex, dimension);

        try {
            String protocol = esUseSsl ? "https" : "http";
            String serverUrl = protocol + "://" + esHost + ":" + esPort;

            var builder = ElasticsearchEmbeddingStore.builder()
                    .serverUrl(serverUrl)
                    .indexName(esIndex)
                    .dimension(dimension);

            // 如果提供了用户名密码，则设置认证信息
            if (esUsername != null && !esUsername.isEmpty()) {
                builder.apiKey(esUsername + ":" + esPassword);
            }

            EmbeddingStore<TextSegment> store = builder.build();

            log.info("Elasticsearch 向量存储初始化成功, index={}", esIndex);
            return store;
        } catch (Exception e) {
            log.error("初始化 Elasticsearch 向量存储失败，降级使用内存存储: {}", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }
}
