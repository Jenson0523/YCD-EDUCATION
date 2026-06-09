package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduClass;
import com.yunchendun.modules.academic.mapper.EduClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块: 教务 edu
 * 功能: 班级管理接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/classes")
public class EduClassController {

    private final EduClassMapper classMapper;

    @GetMapping
    public ApiResponse<IPage<EduClass>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long gradeId) {
        LambdaQueryWrapper<EduClass> wrapper = new LambdaQueryWrapper<>();
        if (gradeId != null) wrapper.eq(EduClass::getGradeId, gradeId);
        return ApiResponse.ok(classMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduClass eduClass) {
        classMapper.insert(eduClass);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduClass eduClass) {
        eduClass.setId(id);
        classMapper.updateById(eduClass);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
