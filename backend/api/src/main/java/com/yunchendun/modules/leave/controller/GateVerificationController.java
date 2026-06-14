package com.yunchendun.modules.leave.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.leave.domain.FaceRecord;
import com.yunchendun.modules.leave.domain.GateVerification;
import com.yunchendun.modules.leave.domain.LeaveApplication;
import com.yunchendun.modules.leave.mapper.FaceRecordMapper;
import com.yunchendun.modules.leave.mapper.GateVerificationMapper;
import com.yunchendun.modules.leave.mapper.LeaveApplicationMapper;
import com.yunchendun.modules.leave.service.FaceService;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
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
    private final FaceRecordMapper faceRecordMapper;
    private final FaceService faceService;
    private final StudentMapper studentMapper;
    private final SysMessageMapper messageMapper;
    private final DataPermissionHelper dataPermissionHelper;

    /**
     * 执行人脸核验离校（服务端人脸比对 + 假条校验）
     * 前端传入：studentNo, capturePhotoUrl, verifyType(DEPART/RETURN)
     * 后端执行：
     *   1. FaceService 1:1 对比（真实API或Mock）
     *   2. 查找当日有效假条
     *   3. 综合判断：人脸分 + 假条存在 = 双重校验
     *   4. 记录核验日志
     *   5. 通过则更新假条状态为 DEPARTED
     */
    @Operation(summary = "执行人脸核验")
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestBody Map<String, Object> body) {
        // 角色校验：仅门卫/管理员可执行核验放行
        String opRole = dataPermissionHelper.current().getRoleCode();
        if (!"GATE".equals(opRole) && !"ADMIN".equals(opRole)) {
            return ApiResponse.fail(403, "仅门卫可执行人脸核验放行");
        }
        String studentNo = (String) body.get("studentNo");
        String capturePhotoUrl = (String) body.get("capturePhotoUrl");
        String verifyType = body.getOrDefault("verifyType", "DEPART").toString();
        Long verifierId = StpUtil.getLoginIdAsLong();

        // 服务端执行人脸比对（不再信任客户端分数）
        Map<String, Object> faceResult = faceService.compare(studentNo, capturePhotoUrl);
        double faceScore = faceResult.get("score") instanceof Number
                ? ((Number) faceResult.get("score")).doubleValue() : 0.0;
        boolean faceOk = (boolean) faceResult.getOrDefault("passed", false);
        return ApiResponse.ok(processVerify(studentNo, capturePhotoUrl, faceScore, faceOk, verifyType, verifierId));
    }

    /**
     * 刷脸自动核验放行（简化门卫操作）：
     *   抓拍 → 1:N识别最匹配学生 → 有已批准假条则【自动放行】并记录；
     *   无有效假条 → 返回 NO_LEAVE + 学生信息，门卫可选择"发起临时审批"走流程。
     */
    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scan(@RequestBody Map<String, Object> body) {
        String opRole = dataPermissionHelper.current().getRoleCode();
        if (!"GATE".equals(opRole) && !"ADMIN".equals(opRole)) {
            return ApiResponse.fail(403, "仅门卫可刷脸核验");
        }
        String capturePhotoUrl = (String) body.get("capturePhotoUrl");
        Long verifierId = StpUtil.getLoginIdAsLong();

        // 今日可放行人脸库：ACTIVE + 有照片 + 今日未通过核验离校
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        java.util.Set<String> departedNos = verifyMapper.selectList(new LambdaQueryWrapper<GateVerification>()
                        .eq(GateVerification::getResult, "PASS").ge(GateVerification::getVerifiedAt, startOfDay))
                .stream().map(GateVerification::getStudentNo).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<FaceRecord> lib = faceRecordMapper.selectList(new LambdaQueryWrapper<FaceRecord>()
                .eq(FaceRecord::getStatus, "ACTIVE")
                .isNotNull(FaceRecord::getFacePhotoUrl).ne(FaceRecord::getFacePhotoUrl, "")
                .notIn(!departedNos.isEmpty(), FaceRecord::getStudentNo, departedNos)
                .last("LIMIT 50"));

        List<Map<String, Object>> cands = faceService.recognize(lib, capturePhotoUrl);
        if (cands.isEmpty()) {
            Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("result", "NO_MATCH");
            r.put("message", "未识别到学生，请正对镜头重试，或确认该生已录入人脸");
            return ApiResponse.ok(r);
        }
        Map<String, Object> top = cands.get(0);
        String studentNo = String.valueOf(top.get("studentNo"));
        double faceScore = top.get("score") instanceof Number ? ((Number) top.get("score")).doubleValue() : 0.0;

        Map<String, Object> resp = processVerify(studentNo, capturePhotoUrl, faceScore, true, "DEPART", verifierId);
        // 附带学生展示信息
        resp.put("realName", top.get("realName"));
        resp.put("className", top.get("className"));
        resp.put("gradeName", top.get("gradeName"));
        resp.put("studentId", top.get("studentId"));
        resp.put("facePhotoUrl", top.get("facePhotoUrl"));
        return ApiResponse.ok(resp);
    }

    /** 共享核验逻辑：查当日有效假条→记日志→有批准假条则放行(标记离校+通知家长)，否则拦截/无假条 */
    private Map<String, Object> processVerify(String studentNo, String capturePhotoUrl,
                                              double faceScore, boolean faceOk, String verifyType, Long verifierId) {
        // 查找当日有效假条（已审批且未离校）
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

        // 解析 studentId / studentName（假条优先，否则查人脸档案/学籍）
        Long studentId = leave != null ? leave.getStudentId() : null;
        String studentName = leave != null ? leave.getStudentName() : null;
        if (studentId == null) {
            FaceRecord fr = faceRecordMapper.selectOne(new LambdaQueryWrapper<FaceRecord>()
                    .eq(FaceRecord::getStudentNo, studentNo).last("LIMIT 1"));
            if (fr != null) {
                studentId = fr.getStudentId();
                if (studentName == null) studentName = fr.getRealName();
            }
        }
        if (studentId == null) {
            Student stu = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getStudentNo, studentNo).last("LIMIT 1"));
            if (stu != null) {
                studentId = stu.getId();
                if (studentName == null) studentName = stu.getName();
            }
        }
        if (studentName == null) studentName = "未知";

        // 记录核验日志
        GateVerification log = new GateVerification();
        log.setVerifyType(verifyType);
        log.setStudentId(studentId != null ? studentId : 0L);
        log.setStudentNo(studentNo);
        log.setStudentName(studentName);
        log.setLeaveAppId(leave != null ? leave.getId() : null);
        log.setVerifierId(verifierId);
        log.setFaceMatchScore(BigDecimal.valueOf(faceScore));
        log.setCapturePhotoUrl(capturePhotoUrl);
        log.setResult(result);
        log.setHardwareMode(0); // 手机端
        log.setVerifiedAt(LocalDateTime.now());
        log.setTenantId(1L);
        verifyMapper.insert(log);

        // 构建返回结果
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("result", result);
        resp.put("message", resultMsg);
        resp.put("faceScore", faceScore);
        resp.put("studentName", studentName);
        resp.put("studentNo", studentNo);

        // 通过则更新假条状态 + 返回详细假条信息
        if ("PASS".equals(result) && leave != null) {
            leave.setStatus("DEPARTED");
            leave.setDepartAt(LocalDateTime.now());
            leaveMapper.updateById(leave);

            resp.put("leaveNo", leave.getLeaveNo());
            resp.put("leaveId", leave.getId());
            resp.put("leaveType", leave.getLeaveType());
            resp.put("leaveStart", leave.getLeaveStart() != null ? leave.getLeaveStart().toString() : "");
            resp.put("leaveEnd", leave.getLeaveEnd() != null ? leave.getLeaveEnd().toString() : "");
            resp.put("reason", leave.getReason() != null ? leave.getReason() : "");
            resp.put("className", leave.getClassName() != null ? leave.getClassName() : "");

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
            msg.setPriority(1);    // 紧急
            msg.setTargetRole("PARENT"); // 仅家长可见
            msg.setTenantId(1L);
            messageMapper.insert(msg);
        } else if (!"PASS".equals(result)) {
            // 异常预警推送（仅管理员和教师可见）
            SysMessage warn = new SysMessage();
            warn.setReceiverId(0L); // 广播给管理员/班主任
            warn.setSenderId(verifierId);
            warn.setTitle("离校异常预警");
            warn.setContent("学籍号 " + studentNo + " 核验结果：" + resultMsg);
            warn.setBizType("LEAVE_ABNORMAL");
            warn.setBizId(log.getId());
            warn.setIsRead(0);
            warn.setPriority(1);          // 紧急
            warn.setTargetRole("TEACHER"); // 仅教师/管理员可见（安全预警）
            warn.setTenantId(1L);
            messageMapper.insert(warn);
        }

        return resp;
    }

    /**
     * 核验前预检：查询某学生当日是否有有效假条（已批准/临时待补且未离校）。
     * 门卫端刷脸锁定身份后调用，用于决定显示"确认放行"还是"临时放行"。
     * 返回：{ hasLeave, leaveId, leaveNo, leaveType, leaveStart, leaveEnd, reason, status }
     */
    @Operation(summary = "核验前预检学生假条")
    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(@RequestParam String studentNo) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LeaveApplication leave = leaveMapper.selectOne(
                new LambdaQueryWrapper<LeaveApplication>()
                        .eq(LeaveApplication::getStudentNo, studentNo)
                        .in(LeaveApplication::getStatus, "APPROVED", "TEMP_PENDING")
                        .ge(LeaveApplication::getLeaveStart, startOfDay)
                        .isNull(LeaveApplication::getDepartAt)
                        .orderByAsc(LeaveApplication::getLeaveStart)
                        .last("LIMIT 1"));
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("hasLeave", leave != null);
        if (leave != null) {
            resp.put("leaveId", leave.getId());
            resp.put("leaveNo", leave.getLeaveNo());
            resp.put("leaveType", "SICK".equals(leave.getLeaveType()) ? "病假"
                    : "PERSONAL".equals(leave.getLeaveType()) ? "事假" : leave.getLeaveType());
            resp.put("leaveStart", leave.getLeaveStart() != null ? leave.getLeaveStart().toString() : "");
            resp.put("leaveEnd", leave.getLeaveEnd() != null ? leave.getLeaveEnd().toString() : "");
            resp.put("reason", leave.getReason() != null ? leave.getReason() : "");
            resp.put("status", leave.getStatus());
        }
        return ApiResponse.ok(resp);
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
