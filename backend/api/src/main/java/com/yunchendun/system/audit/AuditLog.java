package com.yunchendun.system.audit;

import com.yunchendun.common.enums.BusinessModuleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块: 系统权限
 * 功能: 写操作审计注解
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    BusinessModuleEnum module();

    String action();

    String resourceType();
}
