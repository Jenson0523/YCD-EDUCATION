package com.yunchendun.system.audit;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunchendun.system.domain.SysOperationLog;
import com.yunchendun.system.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 模块: 系统权限
 * 功能: 所有写操作统一记录操作日志
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SysOperationLogMapper logMapper;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object writeLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        Object result = joinPoint.proceed();

        SysOperationLog log = new SysOperationLog();
        log.setTenantId(1L);
        log.setOperatorId(StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 0L);
        log.setOperatorName(StpUtil.isLogin() ? String.valueOf(StpUtil.getLoginId()) : "anonymous");
        log.setModuleCode(auditLog.module().name());
        log.setResourceType(auditLog.resourceType());
        log.setAction(auditLog.action());
        log.setAfterData(maskSensitive(objectMapper.writeValueAsString(result)));
        log.setClientIp(request.getRemoteAddr());
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);

        return result;
    }

    private String maskSensitive(String raw) {
        return raw
                .replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")
                .replaceAll("(\\d{6})\\d{8}(\\w{4})", "$1********$2");
    }
}
