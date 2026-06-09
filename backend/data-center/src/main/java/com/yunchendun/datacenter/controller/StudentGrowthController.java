package com.yunchendun.datacenter.controller;

import com.yunchendun.datacenter.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模块: 数据中台
 * 功能: 学生成长档案聚合概览
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequestMapping("/api/dc/student-growth")
public class StudentGrowthController {

    private final JdbcTemplate jdbcTemplate;

    public StudentGrowthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{studentId}")
    public ApiResponse<Map<String, Object>> overview(@PathVariable Long studentId) {
        Integer homeReportCount = jdbcTemplate.queryForObject(
                "select count(1) from fs_home_report where student_id = ? and deleted = 0",
                Integer.class,
                studentId
        );

        return ApiResponse.ok(Map.of(
                "studentId", studentId,
                "homeReportCount", homeReportCount == null ? 0 : homeReportCount,
                "riskLevel", "NORMAL",
                "growthSummary", "已接入学生主档案与家校居家报备数据"
        ));
    }
}
