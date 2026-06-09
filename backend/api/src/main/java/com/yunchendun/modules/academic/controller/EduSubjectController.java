package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduSubject;
import com.yunchendun.modules.academic.mapper.EduSubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块: 教务 edu
 * 功能: 学科管理接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/subjects")
public class EduSubjectController {

    private final EduSubjectMapper subjectMapper;

    @GetMapping
    public ApiResponse<List<EduSubject>> list() {
        return ApiResponse.ok(subjectMapper.selectList(
                new LambdaQueryWrapper<EduSubject>().eq(EduSubject::getEnabled, true)
                        .orderByAsc(EduSubject::getSortOrder)));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduSubject subject) {
        subjectMapper.insert(subject);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduSubject subject) {
        subject.setId(id);
        subjectMapper.updateById(subject);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
