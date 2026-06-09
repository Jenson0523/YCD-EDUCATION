package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduTeachingProgress;
import com.yunchendun.modules.academic.mapper.EduTeachingProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块: 教务 edu
 * 功能: 教学进度接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/teaching-progress")
public class EduTeachingProgressController {

    private final EduTeachingProgressMapper progressMapper;

    @GetMapping
    public ApiResponse<IPage<EduTeachingProgress>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String semester) {
        LambdaQueryWrapper<EduTeachingProgress> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) wrapper.eq(EduTeachingProgress::getClassId, classId);
        if (semester != null) wrapper.eq(EduTeachingProgress::getSemester, semester);
        wrapper.orderByDesc(EduTeachingProgress::getTeachDate);
        return ApiResponse.ok(progressMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduTeachingProgress progress) {
        progressMapper.insert(progress);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduTeachingProgress progress) {
        progress.setId(id);
        progressMapper.updateById(progress);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        progressMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
