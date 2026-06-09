package com.yunchendun.modules.leave.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.leave.domain.GateVerification;
import com.yunchendun.modules.leave.domain.LeaveApplication;
import com.yunchendun.modules.leave.mapper.GateVerificationMapper;
import com.yunchendun.modules.leave.mapper.LeaveApplicationMapper;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.mapper.SysMessageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 模块: 人脸识别请假离校
 * 功能: 门卫核验（人脸比对 + 假条校验）、台账查询
 */
@Tag(name = "门卫核验")
@RestController
@RequestMapping("/api/leave/gate")
@RequiredArgsConstructor
public class GateVerificationController {

    private final GateVerificationMapper verifyMapper;
    private final LeaveApplicationMapper leaveMapper;
    private final SysMessageMapper messageMapper;
    private final DataPermissionHelper dataPermissionHelper;

    /**
     * 执行人脸核验离校
     * 前端传入：studentNo, capturePhotoUrl, faceMatchScore, verifyType(DEPART/RETURN)
     * 后端执行：
     *   1. 查找当日有效假条
     *   2. 综合判断：人脸分 + 假条存在 = 双重校验
     *   3. 记录核验日志
     *   4. 通过则更新假条状态为 DEPARTED
     */
    @Operation(summary = "执行人脸核验")
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestBody Map<String, Object> body) {
        String studentNo = (String) body.get("studentNo");
        String capturePhotoUrl = (String) body.get("capturePhotoUrl");
        double faceScore = body.get("faceMatchScore") != null
                ? Double.parseDouble(body.get("faceMatchScore").toString()) : 0.0;
        String verifyType = body.getOrDefault("verifyType", "DEPART").toString();
        Long verifierId = StpUtil.getLoginIdAsLong();
        double threshold = 80.0;
        boolean faceOk = faceScore >= threshold;

        // 查找今日有效假条（已审批且未离校）
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LeaveApplication leave = leaveMapper.selectOne(
                new LambdaQueryWrapper<LeaveApplication>()
                        .eq(LeaveApplication::getStudentNo, studentNo)
                        .in(LeaveApplication::getStatus, "APPROVED", "TEMP_PENDING")
                        .ge(LeaveApplication::getLeaveStart, startOfDay)
                        .isNull(LeaveApplication::getDepartAt)
                        .orderByAsc(LeaveApplication::getLeaveStart)
                        .last("LIMIT 1"));

        String result;
        String resultMsg;
        if (!faceOk) {
            result = "FACE_MISMATCH";
            resultMsg = "⚠️ 人脸比对不匹配（" + faceScore + "分），已拦截并记录异常";
        } else if (leave == null) {
            result = "NO_LEAVE";
            resultMsg = "⚠️ 人脸比对通过，但无当日有效假条，已拦截";
        } else {
            result = "PASS";
            resultMsg = "✅ 核验通过，放行";
        }

        // 记录核验日志
        GateVerification log = new GateVerification();
        log.setVerifyType(verifyType);
        log.setStudentName(leave != null ? leave.getStudentName() : "未知");
        log.setLeaveAppId(leave != null ? leave.getId() : null);
        log.setVerifierId(verifierId);
        log.setFaceMatchScore(BigDecimal.valueOf(faceScore));
        log.setCapturePhotoUrl(capturePhotoUrl);
        log.setResult(result);
        log.setHardwareMode(0); // 手机端
        log.setVerifiedAt(LocalDateTime.now());
        log.setTenantId(1L);
        verifyMapper.insert(log);

        // 通过则更新假条状态
        if ("PASS".equals(result) && leave != null) {
            leave.setStatus("DEPARTED");
            leave.setDepartAt(LocalDateTime.now());
            leaveMapper.updateById(leave);

            // 推送离校通知给家长
            SysMessage msg = new SysMessage();
            msg.setReceiverId(leave.getApplicantId());
            msg.setSenderId(verifierId);
            msg.setTitle("孩子已离校通知");
            msg.setContent(leave.getStudentName() + " 已于 " + LocalDateTime.now().toString().substring(0, 16)
                    + " 通过人脸核验离校，请悉知。");
            msg.setBizType("LEAVE_DEPART");
            msg.setBizId(leave.getId());
            msg.setIsRead(0);
            msg.setTenantId(1L);
            messageMapper.insert(msg);
        } else if (!"PASS".equals(result)) {
            // 异常预警推送
            SysMessage warn = new SysMessage();
            warn.setReceiverId(0L); // 广播给管理员/班主任
            warn.setSenderId(verifierId);
            warn.setTitle("离校异常预警");
            warn.setContent("学籍号 " + studentNo + " 核验结果：" + resultMsg);
            warn.setBizType("LEAVE_ABNORMAL");
            warn.setBizId(log.getId());
            warn.setIsRead(0);
            warn.setTenantId(1L);
            messageMapper.insert(warn);
        }

        return ApiResponse.ok(Map.of(
                "result", result,
                "message", resultMsg,
                "faceScore", faceScore,
                "leaveNo", leave != null ? leave.getLeaveNo() : "",
                "studentName", leave != null ? leave.getStudentName() : ""
        ));
    }

    /** 今日核验记录（门卫端） */
    @Operation(summary = "今日核验记录")
    @GetMapping("/today")
    public ApiResponse<List<GateVerification>> today(
            @RequestParam(required = false) String result) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<GateVerification> w = new LambdaQueryWrapper<GateVerification>()
                .ge(GateVerification::getVerifiedAt, startOfDay)
                .eq(StringUtils.hasText(result), GateVerification::getResult, result)
                .orderByDesc(GateVerification::getVerifiedAt);
        if (!applyGateScope(w)) return ApiResponse.ok(Collections.emptyList());
        return ApiResponse.ok(verifyMapper.selectList(w));
    }

    /** 台账查询：人脸离校通行台账 */
    @Operation(summary = "人脸通行台账（分页）")
    @GetMapping("/ledger")
    public ApiResponse<Page<GateVerification>> ledger(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<GateVerification> w = new LambdaQueryWrapper<GateVerification>()
                .eq(StringUtils.hasText(result), GateVerification::getResult, result)
                .like(StringUtils.hasText(keyword), GateVerification::getStudentName, keyword)
                .orderByDesc(GateVerification::getVerifiedAt);
        if (!applyGateScope(w)) return ApiResponse.ok(new Page<>(pageNo, pageSize));
        return ApiResponse.ok(verifyMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    /**
     * 对核验记录应用数据权限。
     * @return false 表示当前用户无任何可见数据（调用方应直接返回空）
     */
    private boolean applyGateScope(LambdaQueryWrapper<GateVerification> w) {
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            if (dp.hasNoClass()) return false;
            List<Long> sids = dataPermissionHelper.studentIdsByClasses(dp.getClassIds());
            if (sids.isEmpty()) return false;
            w.in(GateVerification::getStudentId, sids);
        } else if (dp.isSelf()) {
            if (dp.hasNoStudent()) return false;
            w.in(GateVerification::getStudentId, dp.getStudentIds());
        }
        // ALL / GATE_VALID：全校可见
        return true;
    }
}
