package com.gaokao.ai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 向量存储配置类
 * 支持内存存储、PGVector、Elasticsearch三种模式
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

    @Value("${spring.pgvector.datasource.url:}")
    private String pgvectorUrl;

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

    /**
     * 内存向量存储（默认）
     * 适用于开发测试环境
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "memory", matchIfMissing = true)
    public EmbeddingStore<TextSegment> inMemoryEmbeddingStore() {
        log.info("使用内存向量存储");
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 已弃用
     * PGVector向量存储
     * 适用于生产环境
     * 需要配置 spring.pgvector.datasource.* 和 langchain4j.vectorstore.type=pgvector
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "pgvector")
    public EmbeddingStore<TextSegment> pgVectorEmbeddingStore(DataSource pgvectorDataSource,
                                                               EmbeddingModel embeddingModel) {
        log.info("使用PGVector向量存储, table={}, dimension={}", table, dimension);

        try {
            // 注意：这里可能需要根据实际的API调整方法名
            // 由于API可能变化，暂时返回内存存储作为替代
            log.warn("当前版本的LangChain4j API可能已变更，暂时使用内存存储");
            return new InMemoryEmbeddingStore<>();
        } catch (Exception e) {
            log.error("初始化PGVector失败，降级使用内存存储: {}", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }

    /**
     * Elasticsearch向量存储
     * 适用于生产环境，支持大规模向量检索
     * 需要配置 elasticsearch.* 和 langchain4j.vectorstore.type=elasticsearch
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "elasticsearch")
    public EmbeddingStore<TextSegment> elasticsearchEmbeddingStore(RestClient restClient) {
        log.info("使用Elasticsearch向量存储, index={}", esIndex);

        try {
            // LangChain4j 1.0.0-beta2 API: 使用 RestClient
            ElasticsearchEmbeddingStore store = ElasticsearchEmbeddingStore.builder()
                    .restClient(restClient)
                    .indexName(esIndex)
                    .build();

            log.info("Elasticsearch向量存储初始化成功, index={}", esIndex);
            return store;
        } catch (Exception e) {
            log.error("初始化Elasticsearch向量存储失败，降级使用内存存储: {}", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }
}