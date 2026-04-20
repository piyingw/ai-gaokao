package com.gaokao.web.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元对象字段自动填充处理器
 *
 * 功能：
 * - INSERT 时自动填充 createTime、updateTime、deleted
 * - UPDATE 时自动填充 updateTime
 *
 * 解决的问题：
 * 实体类中 @TableField(fill = FieldFill.INSERT/INSERT_UPDATE) 标注的字段
 * 如果没有此处理器，insert/update 时这些字段会为 null，依赖数据库默认值兜底，
 * 但实体层面无法感知实际值，影响后续逻辑（如缓存序列化、日志记录等）。
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始 INSERT 自动填充字段");

        // 自动填充 createTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

        // 自动填充 updateTime
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 自动填充 deleted（逻辑删除标记，0=未删除）
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始 UPDATE 自动填充字段");

        // 自动填充 updateTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
