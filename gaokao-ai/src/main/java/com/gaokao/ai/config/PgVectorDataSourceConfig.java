package com.gaokao.ai.config;

/**
 * PGVector 数据源配置 - 已禁用
 *
 * 当前项目使用 Elasticsearch 作为向量数据库，不再需要 PostgreSQL PGVector。
 * 原 PGVectorDataSourceConfig 类已被移除。
 *
 * 如需恢复 PGVector 支持，请：
 * 1. 取消 gaokao-ai/pom.xml 中 postgresql 和 langchain4j-pgvector 依赖的注释
 * 2. 取消 application.yml 中 pgvector 配置的注释
 * 3. 将 langchain4j.vectorstore.type 改为 pgvector
 */