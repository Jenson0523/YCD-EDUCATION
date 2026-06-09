package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduStudentHonor;
import com.yunchendun.modules.academic.mapper.EduStudentHonorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块: 教务 edu
 * 功能: 学生评优接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/student-honors")
public class EduStudentHonorController {

    private final EduStudentHonorMapper honorMapper;

    @GetMapping
    public ApiResponse<IPage<EduStudentHonor>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<EduStudentHonor> wrapper = new LambdaQueryWrapper<>();
        if (semester != null) wrapper.eq(EduStudentHonor::getSemester, semester);
        if (status != null) wrapper.eq(EduStudentHonor::getStatus, status);
        wrapper.orderByDesc(EduStudentHonor::getScore);
        return ApiResponse.ok(honorMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduStudentHonor honor) {
        honorMapper.insert(honor);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        EduStudentHonor honor = new EduStudentHonor();
        honor.setId(id);
        honor.setStatus("APPROVED");
        honorMapper.updateById(honor);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        EduStudentHonor honor = new EduStudentHonor();
        honor.setId(id);
        honor.setStatus("REJECTED");
        honorMapper.updateById(honor);
        return ApiResponse.ok(null);
    }
}
