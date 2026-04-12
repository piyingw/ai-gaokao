package com.gaokao.ai.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * PGVector 数据源配置
 * 用于向量存储
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.pgvector.datasource")
public class PgVectorDataSourceConfig {

    private String driverClassName = "org.postgresql.Driver";
    private String url;
    private String username;
    private String password;

    /**
     * PGVector 数据源
     * 仅在配置了 langchain4j.vectorstore.type=pgvector 时生效
     */
    @Bean(name = "pgvectorDataSource")
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "pgvector")
    public DataSource pgvectorDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(2);
        dataSource.setMinIdle(2);
        dataSource.setMaxActive(10);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        return dataSource;
    }

    /**
     * PGVector 事务管理器
     */
    @Bean(name = "pgvectorTransactionManager")
    @ConditionalOnProperty(name = "langchain4j.vectorstore.type", havingValue = "pgvector")
    public PlatformTransactionManager pgvectorTransactionManager(DataSource pgvectorDataSource) {
        return new DataSourceTransactionManager(pgvectorDataSource);
    }
}