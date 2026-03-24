package com.gaokao.ai.store;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.gaokao.ai.entity.LongTermMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 长期记忆存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchLongTermMemoryStore {

    private final ElasticsearchClient elasticsearchClient;

    private static final String INDEX_NAME = "long_term_memory";

    @PostConstruct
    public void initializeIndex() throws IOException {
        // 检查索引是否存在
        if (!elasticsearchClient.indices().exists(r -> r.index(INDEX_NAME)).value()) {
            // 创建索引映射
            Map<String, Property> properties = new HashMap<>();

            // keyword 类型字段
            properties.put("userId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            properties.put("sessionId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            properties.put("type", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            properties.put("tags", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

            // text 类型字段
            properties.put("content", Property.of(p -> p.text(TextProperty.of(t -> t))));

            // dense_vector 类型字段
            properties.put("embedding", Property.of(p -> p.denseVector(
                DenseVectorProperty.of(dv -> dv.dims(1024))
            )));

            // date 类型字段
            properties.put("createTime", Property.of(p -> p.date(DateProperty.of(d -> d))));
            properties.put("updateTime", Property.of(p -> p.date(DateProperty.of(d -> d))));
            properties.put("expireTime", Property.of(p -> p.date(DateProperty.of(d -> d))));

            // short 类型字段
            properties.put("importanceScore", Property.of(p ->
                p.short_(s -> s)));

            TypeMapping mapping = TypeMapping.of(tm -> tm.properties(properties));

            // 创建索引
            elasticsearchClient.indices().create(c -> c
                    .index(INDEX_NAME)
                    .mappings(mapping)
            );

            log.info("Elasticsearch 索引 {} 创建成功", INDEX_NAME);
        } else {
            log.info("Elasticsearch 索引 {} 已存在", INDEX_NAME);
        }
    }

    /**
     * 存储长期记忆
     */
    public void storeMemory(LongTermMemory memory) throws IOException {
        IndexRequest<LongTermMemory> request = IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .id(memory.getId())
                .document(memory)
        );

        elasticsearchClient.index(request);
        log.debug("长期记忆已存储：id={}, userId={}", memory.getId(), memory.getUserId());
    }

    /**
     * 根据用户 ID 检索相关记忆
     */
    public List<LongTermMemory> retrieveMemoriesByUserId(String userId, List<Float> queryEmbedding) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t.field("userId").value(userId)))
                        )
                )
                .size(10)
        );

        SearchResponse<LongTermMemory> response = elasticsearchClient.search(request, LongTermMemory.class);

        List<LongTermMemory> memories = new ArrayList<>();
        for (Hit<LongTermMemory> hit : response.hits().hits()) {
            memories.add(hit.source());
        }

        return memories;
    }

    /**
     * 根据记忆类型检索记忆
     */
    public List<LongTermMemory> retrieveMemoriesByType(String userId, LongTermMemory.MemoryType type) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t.field("userId").value(userId)))
                                .must(m -> m.term(t -> t.field("type").value(type.name())))
                        )
                )
                .size(10)
        );

        SearchResponse<LongTermMemory> response = elasticsearchClient.search(request, LongTermMemory.class);

        List<LongTermMemory> memories = new ArrayList<>();
        for (Hit<LongTermMemory> hit : response.hits().hits()) {
            memories.add(hit.source());
        }

        return memories;
    }

    /**
     * 根据标签检索记忆
     */
    public List<LongTermMemory> retrieveMemoriesByTag(String userId, String tag) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t.field("userId").value(userId)))
                                .must(m -> m.term(t -> t.field("tags").value(tag)))
                        )
                )
                .size(10)
        );

        SearchResponse<LongTermMemory> response = elasticsearchClient.search(request, LongTermMemory.class);

        List<LongTermMemory> memories = new ArrayList<>();
        for (Hit<LongTermMemory> hit : response.hits().hits()) {
            memories.add(hit.source());
        }

        return memories;
    }

    /**
     * 检索与给定内容语义相似的记忆
     */
    public List<LongTermMemory> findSimilarMemories(String userId, List<Float> queryEmbedding, int count) throws IOException {
        // 简单实现：先按 userId 检索，然后在应用层排序
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t.field("userId").value(userId)))
                        )
                )
                .size(count)
        );

        SearchResponse<LongTermMemory> response = elasticsearchClient.search(request, LongTermMemory.class);

        List<LongTermMemory> memories = new ArrayList<>();
        for (Hit<LongTermMemory> hit : response.hits().hits()) {
            memories.add(hit.source());
        }

        // 按重要性排序
        memories.sort((m1, m2) -> Integer.compare(m2.getImportanceScore(), m1.getImportanceScore()));

        return memories;
    }

    /**
     * 获取用户的所有长期记忆
     */
    public List<LongTermMemory> getAllMemoriesByUserId(String userId) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q.term(t -> t.field("userId").value(userId)))
                .sort(sf -> sf.field(ff -> ff.field("importanceScore").order(SortOrder.Desc)))
                .size(100)
        );

        SearchResponse<LongTermMemory> response = elasticsearchClient.search(request, LongTermMemory.class);

        List<LongTermMemory> memories = new ArrayList<>();
        for (Hit<LongTermMemory> hit : response.hits().hits()) {
            memories.add(hit.source());
        }

        return memories;
    }

    /**
     * 更新记忆
     */
    public void updateMemory(LongTermMemory memory) throws IOException {
        IndexRequest<LongTermMemory> request = IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .id(memory.getId())
                .document(memory)
        );

        elasticsearchClient.index(request);
        log.debug("长期记忆已更新：id={}, userId={}", memory.getId(), memory.getUserId());
    }

    /**
     * 删除用户的所有记忆
     */
    public void deleteMemoriesByUserId(String userId) throws IOException {
        DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                .index(INDEX_NAME)
                .query(q -> q.term(t -> t.field("userId").value(userId)))
        );

        DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(request);
        log.info("删除用户 {} 的记忆，删除数量：{}", userId, response.deleted());
    }

    /**
     * 清理过期的记忆
     */
    public void cleanupExpiredMemories() throws IOException {
        // 查找所有已过期的记忆
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(q -> q.range(r -> r
                        .field("expireTime")
                        .lt(JsonData.of("now"))
                ))
                .size(1000)
        );

        SearchResponse<LongTermMemory> response = elasticsearchClient.search(request, LongTermMemory.class);

        List<String> expiredIds = new ArrayList<>();
        for (Hit<LongTermMemory> hit : response.hits().hits()) {
            expiredIds.add(hit.id());
        }

        if (!expiredIds.isEmpty()) {
            // 批量删除过期的记忆
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (String id : expiredIds) {
                bulkBuilder.operations(op -> op.delete(d -> d.index(INDEX_NAME).id(id)));
            }

            BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());
            long deletedCount = bulkResponse.items().stream()
                    .filter(item -> item.result() != null && item.result().toString().contains("deleted"))
                    .count();
            log.info("清理了 {} 条过期的记忆", deletedCount);
        }
    }
}
