package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduScore;
import com.yunchendun.modules.academic.mapper.EduScoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块: 教务 edu
 * 功能: 成绩管理接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/scores")
public class EduScoreController {

    private final EduScoreMapper scoreMapper;

    @GetMapping
    public ApiResponse<IPage<EduScore>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String examType) {
        LambdaQueryWrapper<EduScore> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null) wrapper.eq(EduScore::getStudentId, studentId);
        if (classId != null) wrapper.eq(EduScore::getClassId, classId);
        if (semester != null) wrapper.eq(EduScore::getSemester, semester);
        if (examType != null) wrapper.eq(EduScore::getExamType, examType);
        return ApiResponse.ok(scoreMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduScore score) {
        scoreMapper.insert(score);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduScore score) {
        score.setId(id);
        scoreMapper.updateById(score);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scoreMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
