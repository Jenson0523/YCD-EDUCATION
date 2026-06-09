package com.yunchendun.modules.academic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.academic.domain.EduSchedule;
import com.yunchendun.modules.academic.mapper.EduScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块: 教务 edu
 * 功能: 排课接口
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu/schedules")
public class EduScheduleController {

    private final EduScheduleMapper scheduleMapper;

    @GetMapping
    public ApiResponse<List<EduSchedule>> list(
            @RequestParam Long classId,
            @RequestParam String semester) {
        return ApiResponse.ok(scheduleMapper.selectList(
                new LambdaQueryWrapper<EduSchedule>()
                        .eq(EduSchedule::getClassId, classId)
                        .eq(EduSchedule::getSemester, semester)
                        .orderByAsc(EduSchedule::getWeekDay, EduSchedule::getPeriodNo)));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EduSchedule schedule) {
        scheduleMapper.insert(schedule);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody EduSchedule schedule) {
        schedule.setId(id);
        scheduleMapper.updateById(schedule);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
