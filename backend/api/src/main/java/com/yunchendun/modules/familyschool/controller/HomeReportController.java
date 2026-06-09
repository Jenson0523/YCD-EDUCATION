package com.yunchendun.modules.familyschool.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.enums.BusinessModuleEnum;
import com.yunchendun.modules.familyschool.domain.HomeReport;
import com.yunchendun.modules.familyschool.mapper.HomeReportMapper;
import com.yunchendun.system.audit.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 模块: 家校共同体
 * 功能: 居家状态报备接口，体现“居家情况传学校”
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fs/home-reports")
public class HomeReportController {

    private final HomeReportMapper homeReportMapper;

    @GetMapping
    public ApiResponse<IPage<HomeReport>> page(@RequestParam(defaultValue = "1") long pageNo,
                                               @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(homeReportMapper.selectPage(Page.of(pageNo, pageSize), null));
    }

    @PostMapping
    @AuditLog(module = BusinessModuleEnum.FS, action = "CREATE_HOME_REPORT", resourceType = "fs_home_report")
    public ApiResponse<Long> create(@Valid @RequestBody CreateHomeReportRequest request) {
        HomeReport report = new HomeReport();
        report.setTenantId(1L);
        report.setStudentId(request.studentId());
        report.setParentUserId(request.parentUserId());
        report.setReportDate(request.reportDate());
        report.setSleepStatus(request.sleepStatus());
        report.setEmotionStatus(request.emotionStatus());
        report.setStudyStatus(request.studyStatus());
        report.setFamilySpecialSituation(request.familySpecialSituation());
        report.setOutgoingReport(request.outgoingReport());
        report.setFollowStatus("PENDING");
        homeReportMapper.insert(report);
        return ApiResponse.ok(report.getId());
    }

    public record CreateHomeReportRequest(
            @NotNull Long studentId,
            @NotNull Long parentUserId,
            @NotNull LocalDate reportDate,
            String sleepStatus,
            String emotionStatus,
            String studyStatus,
            String familySpecialSituation,
            String outgoingReport
    ) {
    }
}
