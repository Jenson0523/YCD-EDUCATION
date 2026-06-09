package com.yunchendun.modules.familyschool.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.enums.BusinessModuleEnum;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.familyschool.domain.HomeReport;
import com.yunchendun.modules.familyschool.mapper.HomeReportMapper;
import com.yunchendun.system.audit.AuditLog;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.mapper.SysMessageMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模块: 家校共同体
 * 功能: 居家状态报备接口（提交、列表、教师跟进）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fs/home-reports")
public class HomeReportController {

    private final HomeReportMapper homeReportMapper;
    private final SysMessageMapper messageMapper;
    private final DataPermissionHelper dataPermissionHelper;

    @GetMapping
    public ApiResponse<IPage<HomeReport>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String followStatus,
            @RequestParam(required = false) Long studentId) {
        LambdaQueryWrapper<HomeReport> wrapper = new LambdaQueryWrapper<>();
        if (followStatus != null) wrapper.eq(HomeReport::getFollowStatus, followStatus);
        if (studentId != null) wrapper.eq(HomeReport::getStudentId, studentId);

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isSelf()) {
            if (dp.hasNoStudent()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            wrapper.in(HomeReport::getStudentId, dp.getStudentIds());
        } else if (dp.isClass()) {
            if (dp.hasNoClass()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            List<Long> sids = dataPermissionHelper.studentIdsByClasses(dp.getClassIds());
            if (sids.isEmpty()) return ApiResponse.ok(Page.of(pageNo, pageSize));
            wrapper.in(HomeReport::getStudentId, sids);
        }
        // ALL：全部可见（GATE 门卫不涉及居家报备）

        wrapper.orderByDesc(HomeReport::getReportDate);
        return ApiResponse.ok(homeReportMapper.selectPage(Page.of(pageNo, pageSize), wrapper));
    }

    @PostMapping
    @AuditLog(module = BusinessModuleEnum.FS, action = "CREATE_HOME_REPORT", resourceType = "fs_home_report")
    public ApiResponse<Long> create(@Valid @RequestBody CreateHomeReportRequest request) {
        HomeReport report = new HomeReport();
        report.setTenantId(1L);
        report.setStudentId(request.studentId());
        report.setParentUserId(StpUtil.getLoginIdAsLong());
        report.setReportDate(request.reportDate() != null ? request.reportDate() : LocalDate.now());
        report.setSleepStatus(request.sleepStatus());
        report.setEmotionStatus(request.emotionStatus());
        report.setStudyStatus(request.studyStatus());
        report.setFamilySpecialSituation(request.familySpecialSituation());
        report.setOutgoingReport(request.outgoingReport());
        report.setFollowStatus("PENDING");
        homeReportMapper.insert(report);
        return ApiResponse.ok(report.getId());
    }

    @PutMapping("/{id}/follow")
    @AuditLog(module = BusinessModuleEnum.FS, action = "FOLLOW_HOME_REPORT", resourceType = "fs_home_report")
    public ApiResponse<Void> follow(@PathVariable Long id, @RequestBody FollowRequest req) {
        HomeReport report = homeReportMapper.selectById(id);
        if (report == null) return ApiResponse.fail(404, "报备记录不存在");

        report.setFollowStatus("FOLLOWED");
        report.setFollowRemark(req.remark());
        report.setFollowBy(StpUtil.getLoginIdAsLong());
        report.setFollowAt(LocalDateTime.now());
        homeReportMapper.updateById(report);

        // 给家长推送站内消息
        SysMessage msg = new SysMessage();
        msg.setTenantId(1L);
        msg.setReceiverId(report.getParentUserId());
        msg.setSenderId(StpUtil.getLoginIdAsLong());
        msg.setTitle("老师已跟进您的居家报备");
        msg.setContent(req.remark() != null && !req.remark().isBlank()
                ? "老师回复：" + req.remark()
                : "老师已查看并跟进您" + report.getReportDate() + "的居家报备。");
        msg.setBizType("HOME_REPORT_REPLY");
        msg.setBizId(id);
        messageMapper.insert(msg);

        return ApiResponse.ok(null);
    }

    public record CreateHomeReportRequest(
            @NotNull Long studentId,
            LocalDate reportDate,
            String sleepStatus,
            String emotionStatus,
            String studyStatus,
            String familySpecialSituation,
            String outgoingReport) {}

    public record FollowRequest(String remark) {}
}
