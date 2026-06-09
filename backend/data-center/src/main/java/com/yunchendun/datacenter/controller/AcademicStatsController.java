package com.yunchendun.datacenter.controller;

import com.yunchendun.datacenter.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模块: 数据中台 dc
 * 功能: 教务统计指标 — 学科成绩均值、排课量、作业完成率
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dc/academic-stats")
public class AcademicStatsController {

    private final JdbcTemplate jdbc;

    @GetMapping("/score-summary")
    public ApiResponse<List<Map<String, Object>>> scoreSummary(@RequestParam String semester) {
        String sql = "SELECT subject_id, exam_type, AVG(score) AS avg_score, MAX(score) AS max_score, MIN(score) AS min_score, COUNT(*) AS cnt " +
                     "FROM edu_score WHERE semester = ? AND deleted = 0 GROUP BY subject_id, exam_type ORDER BY subject_id, exam_type";
        return ApiResponse.ok(jdbc.queryForList(sql, semester));
    }

    @GetMapping("/homework-completion")
    public ApiResponse<List<Map<String, Object>>> homeworkCompletion(@RequestParam Long classId) {
        String sql = "SELECT h.id AS homework_id, h.title, h.due_date, " +
                     "SUM(CASE WHEN s.status = 'SUBMITTED' THEN 1 ELSE 0 END) AS submitted, " +
                     "COUNT(s.id) AS total " +
                     "FROM edu_homework h LEFT JOIN edu_homework_submission s ON s.homework_id = h.id " +
                     "WHERE h.class_id = ? AND h.deleted = 0 GROUP BY h.id ORDER BY h.assigned_date DESC LIMIT 20";
        return ApiResponse.ok(jdbc.queryForList(sql, classId));
    }

    @GetMapping("/schedule-count")
    public ApiResponse<Map<String, Object>> scheduleCount(@RequestParam String semester) {
        String sql = "SELECT COUNT(*) AS total_periods, COUNT(DISTINCT class_id) AS class_cnt, COUNT(DISTINCT teacher_user_id) AS teacher_cnt " +
                     "FROM edu_schedule WHERE semester = ? AND deleted = 0";
        return ApiResponse.ok(jdbc.queryForMap(sql, semester));
    }
}
