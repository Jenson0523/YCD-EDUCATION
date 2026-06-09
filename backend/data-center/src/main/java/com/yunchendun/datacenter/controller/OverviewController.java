package com.yunchendun.datacenter.controller;

import com.yunchendun.datacenter.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模块: 数据中台
 * 功能: 学校管理总览指标
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequestMapping("/api/dc/overview")
public class OverviewController {

    private final JdbcTemplate jdbcTemplate;

    public OverviewController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> overview() {
        Integer studentCount = jdbcTemplate.queryForObject("select count(1) from stu_student where deleted = 0", Integer.class);
        Integer pendingHomeReports = jdbcTemplate.queryForObject("select count(1) from fs_home_report where follow_status = 'PENDING' and deleted = 0", Integer.class);

        return ApiResponse.ok(Map.of(
                "studentCount", studentCount == null ? 0 : studentCount,
                "pendingHomeReports", pendingHomeReports == null ? 0 : pendingHomeReports,
                "todoApprovals", 0,
                "riskAlerts", 0
        ));
    }
}
