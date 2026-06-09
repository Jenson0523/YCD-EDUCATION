package com.yunchendun.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模块: 系统权限
 * 功能: 全量操作日志
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("sys_operation_log")
public class SysOperationLog {
    private Long id;
    private Long tenantId;
    private Long operatorId;
    private String operatorName;
    private String moduleCode;
    private String resourceType;
    private String resourceId;
    private String action;
    private String beforeData;
    private String afterData;
    private String clientIp;
    private LocalDateTime createdAt;
}
