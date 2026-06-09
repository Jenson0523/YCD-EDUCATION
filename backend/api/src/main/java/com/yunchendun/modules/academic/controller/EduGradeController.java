package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduGrade;
import com.yunchendun.modules.academic.mapper.EduGradeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块: 教务 edu
 * 功能: 年级管理接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/grades")
public class EduGradeController {

    private final EduGradeMapper gradeMapper;

    @GetMapping
    public ApiResponse<List<EduGrade>> list() {
        return ApiResponse.ok(gradeMapper.selectList(
                new LambdaQueryWrapper<EduGrade>().orderByAsc(EduGrade::getSortOrder)));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduGrade grade) {
        gradeMapper.insert(grade);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduGrade grade) {
        grade.setId(id);
        gradeMapper.updateById(grade);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        gradeMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
