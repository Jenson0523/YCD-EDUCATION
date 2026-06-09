package com.yunchendun.modules.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块: 学生学籍
 * 功能: 学生主档案接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stu/students")
public class StudentController {

    private final StudentMapper studentMapper;

    @GetMapping
    public ApiResponse<IPage<Student>> page(@RequestParam(defaultValue = "1") long pageNo,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(studentMapper.selectPage(Page.of(pageNo, pageSize), null));
    }
}
