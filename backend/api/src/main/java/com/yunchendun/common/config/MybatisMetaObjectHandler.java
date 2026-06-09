package com.yunchendun.common.config;

import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 模块: 平台级 / common
 * 功能: 通用创建/更新时间自动填充
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /** 安全获取当前登录用户ID，非Web上下文（如启动初始化）返回0 */
    private Long currentUserId() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 0L;
        } catch (NotWebContextException e) {
            return 0L;
        }
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, userId);
        strictInsertFill(metaObject, "updatedBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = currentUserId();
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
    }
}
