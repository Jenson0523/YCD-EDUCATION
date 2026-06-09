package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduHomework;
import com.yunchendun.modules.academic.mapper.EduHomeworkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块: 教务 edu
 * 功能: 作业管理接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/homeworks")
public class EduHomeworkController {

    private final EduHomeworkMapper homeworkMapper;

    @GetMapping
    public ApiResponse<IPage<EduHomework>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long subjectId) {
        LambdaQueryWrapper<EduHomework> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) wrapper.eq(EduHomework::getClassId, classId);
        if (subjectId != null) wrapper.eq(EduHomework::getSubjectId, subjectId);
        wrapper.orderByDesc(EduHomework::getAssignedDate);
        return ApiResponse.ok(homeworkMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduHomework homework) {
        homeworkMapper.insert(homework);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduHomework homework) {
        homework.setId(id);
        homeworkMapper.updateById(homework);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        homeworkMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
