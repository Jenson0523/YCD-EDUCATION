package com.yunchendun.modules.leave.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.leave.domain.FaceRecord;
import com.yunchendun.modules.leave.domain.GateVerification;
import com.yunchendun.modules.leave.mapper.FaceRecordMapper;
import com.yunchendun.modules.leave.mapper.GateVerificationMapper;
import com.yunchendun.modules.leave.service.FaceConsentService;
import com.yunchendun.modules.leave.service.FaceService;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模块: 人脸识别请假离校
 * 功能: 学生人脸档案管理
 */
@Tag(name = "人脸档案管理")
@RestController
@RequestMapping("/api/leave/face")
@RequiredArgsConstructor
public class FaceRecordController {

    private final FaceRecordMapper faceRecordMapper;
    private final FaceService faceService;
    private final FaceConsentService faceConsentService;
    private final GateVerificationMapper gateVerificationMapper;
    private final DataPermissionHelper dataPermissionHelper;
    private final SysUserMapper sysUserMapper;

    /** 分页查询人脸档案（后台管理） */
    @Operation(summary = "人脸档案列表")
    @GetMapping
    public ApiResponse<Page<FaceRecord>> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<FaceRecord> wrapper = new LambdaQueryWrapper<FaceRecord>()
                .and(StringUtils.hasText(keyword), q -> q
                        .like(FaceRecord::getRealName, keyword)
                        .or().like(FaceRecord::getStudentNo, keyword))
                .eq(StringUtils.hasText(status), FaceRecord::getStatus, status)
                .orderByDesc(FaceRecord::getCreatedAt);

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            if (dp.hasNoClass()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            wrapper.in(FaceRecord::getClassId, dp.getClassIds());
        } else if (dp.isSelf()) {
            if (dp.hasNoStudent()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            wrapper.in(FaceRecord::getStudentId, dp.getStudentIds());
        }
        // ALL / GATE_VALID：全部可见
        return ApiResponse.ok(faceRecordMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    /** 根据学生ID查询人脸档案（按数据权限：家长限本人孩子/教师限本班/管理员门卫全部） */
    @Operation(summary = "查询某学生人脸档案（by studentId）")
    @GetMapping("/by-student/{studentId}")
    public ApiResponse<FaceRecord> getByStudent(@PathVariable Long studentId) {
        if (!canAccessStudent(studentId)) return ApiResponse.fail(403, "无权查看该学生人脸档案");
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>().eq(FaceRecord::getStudentId, studentId));
        return ApiResponse.ok(rec);
    }

    /** 根据学籍号查询人脸档案（按数据权限过滤，防止枚举他人档案） */
    @Operation(summary = "查询某学生人脸档案（by studentNo）")
    @GetMapping("/by-no/{studentNo}")
    public ApiResponse<FaceRecord> getByStudentNo(@PathVariable String studentNo) {
        FaceRecord rec = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>().eq(FaceRecord::getStudentNo, studentNo));
        if (rec != null && !canAccessStudent(rec.getStudentId())) {
            return ApiResponse.fail(403, "无权查看该学生人脸档案");
        }
        return ApiResponse.ok(rec);
    }

    /** 当前登录用户是否有权访问指定学生（家长=绑定孩子，教师=所辖班级，管理员/门卫=全部） */
    private boolean canAccessStudent(Long studentId) {
        if (studentId == null) return false;
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isSelf()) return !dp.hasNoStudent() && dp.getStudentIds().contains(studentId);
        if (dp.isClass()) return !dp.hasNoClass()
                && dataPermissionHelper.studentIdsByClasses(dp.getClassIds()).contains(studentId);
        return true; // ALL / GATE_VALID
    }

    /** 人脸采集《告知同意书》全文（采集前必须展示给用户阅读） */
    @Operation(summary = "人脸采集告知同意书")
    @GetMapping("/consent-doc")
    public ApiResponse<Map<String, Object>> consentDoc() {
        return ApiResponse.ok(faceConsentService.getConsentDoc());
    }

    /**
     * 新建或更新人脸档案（家长为孩子录入 / 教师代录入 / 管理员后台录入）。
     * 合规要求：
     *   1. 必须 consentAgreed=true（已阅读并同意《告知同意书》），否则拒绝；
     *   2. 数据权限：家长仅能为绑定孩子录入；教师仅能为所辖班级学生录入；
     *   3. 每次录入/更新写入授权审计日志（face_consent_log）。
     */
    @Operation(summary = "新建/更新人脸档案（需先同意告知书）")
    @PostMapping
    public ApiResponse<FaceRecord> save(@RequestBody FaceRecord record) {
        // ── 合规：必须勾选同意 ──
        if (record.getConsentAgreed() == null || !record.getConsentAgreed()) {
            return ApiResponse.fail(400, "请先阅读并同意《学生人脸信息采集与使用告知同意书》");
        }

        // ── 数据权限：家长→绑定孩子；教师→所辖班级；管理员→全部 ──
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isSelf()) {
            if (record.getStudentId() == null || dp.hasNoStudent()
                    || !dp.getStudentIds().contains(record.getStudentId())) {
                return ApiResponse.fail(403, "只能为已绑定的孩子录入人脸");
            }
        } else if (dp.isClass()) {
            if (record.getStudentId() == null || dp.hasNoClass()
                    || !dataPermissionHelper.studentIdsByClasses(dp.getClassIds())
                            .contains(record.getStudentId())) {
                return ApiResponse.fail(403, "只能为所辖班级的学生录入人脸");
            }
        }
        // ALL/GATE：不限制（管理员后台录入；门卫端不提供录入入口）

        // 操作人信息（写入授权摘要与审计日志）
        Long uid = StpUtil.getLoginIdAsLong();
        SysUser operator = sysUserMapper.selectById(uid);
        String operatorName = operator != null ? operator.getRealName() : "";
        String operatorRole = operator != null ? operator.getRoleCode() : "";

        // 检查是否已有档案
        FaceRecord exist = faceRecordMapper.selectOne(
                new LambdaQueryWrapper<FaceRecord>()
                        .eq(FaceRecord::getStudentNo, record.getStudentNo()));
        boolean isUpdate = exist != null;
        FaceRecord target;
        if (isUpdate) {
            exist.setFacePhotoUrl(record.getFacePhotoUrl());
            exist.setStatus("ACTIVE");
            if (StringUtils.hasText(record.getRealName())) exist.setRealName(record.getRealName());
            if (StringUtils.hasText(record.getClassName())) exist.setClassName(record.getClassName());
            if (StringUtils.hasText(record.getGradeName())) exist.setGradeName(record.getGradeName());
            if (record.getClassId() != null) exist.setClassId(record.getClassId());
            target = exist;
        } else {
            record.setStatus("ACTIVE");
            record.setTenantId(1L);
            target = record;
        }

        // 先同步到云端人脸库（百度faceset）。照片检测不到人脸则拒绝录入，
        // 避免存入"无法识别"的坏档案（解决重新录入后识别失败问题）。
        Map<String, Object> reg = faceService.registerToLibrary(
                target.getStudentNo(), target.getRealName(), record.getFacePhotoUrl());
        if (Boolean.FALSE.equals(reg.get("success"))) {
            return ApiResponse.fail(400, String.valueOf(reg.get("message")));
        }

        // 授权摘要
        target.setConsentVersion(FaceConsentService.CONSENT_VERSION);
        target.setConsentAt(LocalDateTime.now());
        target.setConsentBy(uid);
        target.setConsentByName(operatorName);
        target.setConsentRole(operatorRole);

        if (isUpdate) faceRecordMapper.updateById(target);
        else faceRecordMapper.insert(target);

        // 审计留痕
        faceConsentService.recordConsent(
                target.getStudentId(), target.getStudentNo(), target.getRealName(),
                uid, operatorName, operatorRole, isUpdate ? "UPDATE" : "ENROLL");

        return ApiResponse.ok(target);
    }

    /** 编辑人脸档案（修改基本信息，不含照片） */
    @Operation(summary = "编辑人脸档案")
    @PutMapping("/{id}")
    public ApiResponse<FaceRecord> update(@PathVariable Long id, @RequestBody FaceRecord record) {
        FaceRecord rec = faceRecordMapper.selectById(id);
        if (rec == null) return ApiResponse.fail(404, "档案不存在");
        if (StringUtils.hasText(record.getRealName())) rec.setRealName(record.getRealName());
        if (StringUtils.hasText(record.getStudentNo())) rec.setStudentNo(record.getStudentNo());
        if (StringUtils.hasText(record.getClassName())) rec.setClassName(record.getClassName());
        if (StringUtils.hasText(record.getGradeName())) rec.setGradeName(record.getGradeName());
        if (StringUtils.hasText(record.getHeadTeacherName())) rec.setHeadTeacherName(record.getHeadTeacherName());
        if (record.getClassId() != null) rec.setClassId(record.getClassId());
        if (record.getStudentId() != null) rec.setStudentId(record.getStudentId());
        faceRecordMapper.updateById(rec);
        return ApiResponse.ok(rec);
    }

    /** 删除人脸档案 */
    @Operation(summary = "删除人脸档案")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        FaceRecord rec = faceRecordMapper.selectById(id);
        if (rec == null) return ApiResponse.fail(404, "档案不存在");
        faceService.removeFromLibrary(rec.getStudentNo()); // 同步移除云端人脸库
        faceRecordMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    /** 修改档案状态（启用/禁用） */
    @Operation(summary = "修改人脸档案状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        FaceRecord rec = faceRecordMapper.selectById(id);
        if (rec == null) return ApiResponse.fail(404, "档案不存在");
        String status = body.get("status");
        rec.setStatus(status);
        faceRecordMapper.updateById(rec);
        // 禁用→移出云端人脸库；启用→重新入库
        if ("DISABLED".equals(status)) {
            faceService.removeFromLibrary(rec.getStudentNo());
        } else if ("ACTIVE".equals(status)) {
            faceService.registerToLibrary(rec.getStudentNo(), rec.getRealName(), rec.getFacePhotoUrl());
        }
        return ApiResponse.ok(null);
    }

    /** 人脸照片比对（门卫核验用，由 FaceService 统一处理真实 API 或 Mock） */
    @Operation(summary = "人脸比对（门卫核验用）")
    @PostMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(@RequestBody Map<String, String> body) {
        // 仅门卫/管理员可比对（核验操作）
        String role = dataPermissionHelper.current().getRoleCode();
        if (!"GATE".equals(role) && !"ADMIN".equals(role)) {
            return ApiResponse.fail(403, "仅门卫可执行人脸比对核验");
        }
        String studentNo = body.get("studentNo");
        String capturePhotoUrl = body.get("capturePhotoUrl");
        return ApiResponse.ok(faceService.compare(studentNo, capturePhotoUrl));
    }

    /** 当前人脸引擎信息（虹软离线 / 图像过渡引擎，便于排查精准度） */
    @Operation(summary = "当前人脸引擎信息")
    @GetMapping("/engine-info")
    public ApiResponse<Map<String, Object>> engineInfo() {
        return ApiResponse.ok(faceService.engineInfo());
    }

    /**
     * 刷脸识别：上传抓拍照片，在人脸库中检索匹配学生（门卫端免输入学籍号）。
     * 由 FaceService 统一处理（真实 API 或 Mock），返回 Top-5 候选人（降序）。
     */
    @Operation(summary = "刷脸识别（人脸库1:N检索）")
    @PostMapping("/recognize")
    public ApiResponse<List<Map<String, Object>>> recognize(@RequestBody Map<String, String> body) {
        // 隐私保护：1:N 全库检索仅门卫/管理员可用，家长/学生不得扫描他人
        String role = dataPermissionHelper.current().getRoleCode();
        if (!"GATE".equals(role) && !"ADMIN".equals(role)) {
            return ApiResponse.fail(403, "仅门卫可使用刷脸识别");
        }
        String capturePhotoUrl = body.get("capturePhotoUrl");

        // 查今日已核验离校的学籍号（刷脸识别时排除已离校学生）
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<GateVerification> todayDeparted = gateVerificationMapper.selectList(
                new LambdaQueryWrapper<GateVerification>()
                        .eq(GateVerification::getResult, "PASS")
                        .ge(GateVerification::getVerifiedAt, startOfDay));
        java.util.Set<String> departedNos = todayDeparted.stream()
                .map(GateVerification::getStudentNo)
                .collect(Collectors.toSet());

        // 从人脸库中获取所有 Active 且有照片的记录，排除已离校的
        List<FaceRecord> lib = faceRecordMapper.selectList(new LambdaQueryWrapper<FaceRecord>()
                .eq(FaceRecord::getStatus, "ACTIVE")
                .isNotNull(FaceRecord::getFacePhotoUrl)
                .ne(FaceRecord::getFacePhotoUrl, "")
                .notIn(!departedNos.isEmpty(), FaceRecord::getStudentNo, departedNos)
                .orderByDesc(FaceRecord::getUpdatedAt)
                .last("LIMIT 20"));
        return ApiResponse.ok(faceService.recognize(lib, capturePhotoUrl));
    }
}
