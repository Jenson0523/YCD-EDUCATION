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
import com.yunchendun.modules.leave.domain.TempSupplementOrder;
import com.yunchendun.modules.leave.mapper.FaceRecordMapper;
import com.yunchendun.modules.leave.mapper.GateVerificationMapper;
import com.yunchendun.modules.leave.mapper.LeaveApplicationMapper;
import com.yunchendun.modules.leave.mapper.TempSupplementOrderMapper;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import com.yunchendun.modules.student.domain.Student;
import com.yunchendun.modules.student.mapper.StudentMapper;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysMessageMapper;
import com.yunchendun.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模块: 人脸识别请假离校
 * 功能: 请假申请全流程（提交/审批/查询）
 */
@Tag(name = "请假申请管理")
@RestController
@RequestMapping("/api/leave/applications")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationMapper leaveMapper;
    private final FaceRecordMapper faceRecordMapper;
    private final TempSupplementOrderMapper supplementMapper;
    private final SysMessageMapper messageMapper;
    private final GateVerificationMapper gateVerificationMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentMapper studentMapper;
    private final DataPermissionHelper dataPermissionHelper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 10000);

    private String genLeaveNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "YCD-LEAVE-" + date + "-" + String.format("%04d", SEQ.incrementAndGet() % 10000);
    }

    // ======================== 查询 ========================

    /** 请假申请列表（支持状态/班级/姓名/日期范围筛选，按角色隔离数据） */
    @Operation(summary = "请假申请列表")
    @GetMapping
    public ApiResponse<Page<LeaveApplication>> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long uid = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<LeaveApplication> w = new LambdaQueryWrapper<LeaveApplication>()
                .eq(StringUtils.hasText(status), LeaveApplication::getStatus, status)
                .eq(classId != null, LeaveApplication::getClassId, classId)
                .like(StringUtils.hasText(keyword), LeaveApplication::getStudentName, keyword);
        
        // 日期范围筛选：按 leave_start 过滤
        if (StringUtils.hasText(startDate)) {
            w.ge(LeaveApplication::getLeaveStart, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            w.le(LeaveApplication::getLeaveStart, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        
        w.orderByDesc(LeaveApplication::getCreatedAt);

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            // 班主任/任课老师：按班级 + 学生双重过滤
            List<Long> classIds = dp.getClassIds();
            if (classIds != null && !classIds.isEmpty()) {
                List<Long> studentIds = dataPermissionHelper.studentIdsByClasses(classIds);
                if (studentIds.isEmpty()) {
                    w.in(LeaveApplication::getClassId, classIds);
                } else {
                    w.and(w2 -> w2
                            .in(LeaveApplication::getClassId, classIds)
                            .or().in(LeaveApplication::getStudentId, studentIds));
                }
            }
            // 如果 classIds 为空（teacher_class 表无绑定），不做 class 过滤
            // 但仍通过下面 status 条件限制只查待审批等状态
        } else if (dp.isSelf()) {
            // 家长/学生：仅本人关联学生
            if (dp.hasNoStudent()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            w.in(LeaveApplication::getStudentId, dp.getStudentIds());
        }
        // ALL / GATE_VALID：不额外限制（门卫核验需看全校请假）

        // 额外：role=my 仅看本人发起的
        if ("my".equals(role)) {
            w.eq(LeaveApplication::getApplicantId, uid);
        }
        Page<LeaveApplication> page = leaveMapper.selectPage(new Page<>(pageNo, pageSize), w);
        fillApplicantLabels(page.getRecords()); // 申请人标签，便于班主任识别来源
        return ApiResponse.ok(page);
    }

    /** 今日有效假条（给门卫端用：含已批准/已离校/临时待补） */
    @Operation(summary = "今日有效假条列表（门卫）")
    @GetMapping("/today-valid")
    public ApiResponse<List<LeaveApplication>> todayValid() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<LeaveApplication> list = leaveMapper.selectList(
                new LambdaQueryWrapper<LeaveApplication>()
                        .in(LeaveApplication::getStatus, "APPROVED", "TEMP_PENDING", "DEPARTED")
                        .ge(LeaveApplication::getLeaveStart, startOfDay)
                        .lt(LeaveApplication::getLeaveStart, endOfDay)
                        .orderByAsc(LeaveApplication::getLeaveStart));
        fillApplicantLabels(list);
        return ApiResponse.ok(list);
    }

    /** 单条详情（含申请人姓名、核验记录、刷脸照片）- 按角色校验数据权限 */
    @Operation(summary = "请假申请详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        LeaveApplication leave = leaveMapper.selectById(id);
        if (leave == null) return ApiResponse.ok(null);

        // 数据权限校验
        Long uid = StpUtil.getLoginIdAsLong();
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isSelf()) {
            // 家长/学生：只能看自己关联学生的请假
            if (leave.getStudentId() == null || !dp.getStudentIds().contains(leave.getStudentId())) {
                return ApiResponse.fail(403, "您无权查看此请假记录");
            }
        } else if (dp.isClass()) {
            // 班主任/教师：只能看自己管辖班级学生的请假
            if (leave.getClassId() == null || !dp.getClassIds().contains(leave.getClassId())) {
                return ApiResponse.fail(403, "您无权查看此请假记录");
            }
        }
        // GATE / ALL：不限制

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        // 基础字段
        result.put("id", leave.getId());
        result.put("leaveNo", leave.getLeaveNo());
        result.put("studentId", leave.getStudentId());
        result.put("studentNo", leave.getStudentNo());
        result.put("studentName", leave.getStudentName());
        result.put("classId", leave.getClassId());
        result.put("className", leave.getClassName());
        result.put("leaveType", leave.getLeaveType());
        result.put("reason", leave.getReason());
        result.put("leaveStart", leave.getLeaveStart());
        result.put("leaveEnd", leave.getLeaveEnd());
        result.put("proofPhotoUrl", leave.getProofPhotoUrl());
        result.put("isTemp", leave.getIsTemp());
        result.put("status", leave.getStatus());
        result.put("approveRemark", leave.getApproveRemark());
        result.put("approveSignatureUrl", leave.getApproveSignatureUrl());
        result.put("approvedBy", leave.getApprovedBy());
        result.put("approvedAt", leave.getApprovedAt());
        result.put("departAt", leave.getDepartAt());
        result.put("returnAt", leave.getReturnAt());
        result.put("tempDeadline", leave.getTempDeadline());
        result.put("createdAt", leave.getCreatedAt());

        // 申请人角色 + 姓名（如"家长王xx"、"老师张英语"）
        result.put("applicantId", leave.getApplicantId());
        result.put("applicantRole", leave.getApplicantRole());
        if (leave.getApplicantId() != null) {
            SysUser applicant = sysUserMapper.selectById(leave.getApplicantId());
            result.put("applicantName", applicant != null ? applicant.getRealName() : "");
        } else {
            result.put("applicantName", "");
        }
        result.put("applicantLabel", buildApplicantLabel(leave)); // 申请人标签

        // 关联的核验记录（人脸通行记录）
        List<GateVerification> verifications = gateVerificationMapper.selectList(
                new LambdaQueryWrapper<GateVerification>()
                        .eq(GateVerification::getLeaveAppId, id)
                        .orderByDesc(GateVerification::getVerifiedAt));
        result.put("verifications", verifications);

        return ApiResponse.ok(result);
    }

    // ======================== 提交 ========================

    /** 提交常规请假申请 */
    @Operation(summary = "提交请假申请")
    @PostMapping
    public ApiResponse<LeaveApplication> submit(@RequestBody LeaveApplication req) {
        Long uid = StpUtil.getLoginIdAsLong();
        req.setApplicantId(uid);
        req.setLeaveNo(genLeaveNo());
        req.setStatus("PENDING");
        req.setIsTemp(0);
        req.setTenantId(1L);
        req.setClassId(resolveClassId(req)); // classId 缺失时从学生档案补全
        leaveMapper.insert(req);

        // 人脸录入不再随请假提交直接落库（合规要求先阅读并同意《告知同意书》），
        // 统一走 POST /api/leave/face（含同意校验+权限校验+审计留痕），前端请假页会引导跳转录入。

        // 精准通知该班级的班主任/任课教师审批
        notifyClassTeachers(req.getClassId(), uid,
                "新请假申请待审批",
                req.getStudentName() + " 提交了请假申请（" + leaveTypeText(req.getLeaveType())
                        + "），请及时审批。",
                "LEAVE_APPLY", req.getId());
        return ApiResponse.ok(req);
    }

    // ======================== 审批 ========================

    /** 班主任审批（通过/驳回）- 仅限班主任/教师操作其管辖班级的学生 */
    @Operation(summary = "审批请假申请")
    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long uid = StpUtil.getLoginIdAsLong();
        SysUser approver = sysUserMapper.selectById(uid);
        if (approver == null) return ApiResponse.fail(401, "请先登录");

        // 角色校验：仅班主任/教师可审批
        String approverRole = approver.getRoleCode();
        if (!"HEAD_TEACHER".equals(approverRole) && !"TEACHER".equals(approverRole)) {
            return ApiResponse.fail(403, "您没有审批权限，仅班主任和教师可审批");
        }

        LeaveApplication app = leaveMapper.selectById(id);
        if (app == null) return ApiResponse.fail(404, "记录不存在");
        if (!"PENDING".equals(app.getStatus()) && !"TEMP_PENDING".equals(app.getStatus())) {
            return ApiResponse.fail(400, "当前状态不可审批");
        }

        // 班级绑定校验：审批人必须是该学生所在班级的教师
        if (app.getClassId() != null) {
            Long count = teacherClassMapper.selectCount(new LambdaQueryWrapper<TeacherClass>()
                    .eq(TeacherClass::getTeacherUserId, uid)
                    .eq(TeacherClass::getClassId, app.getClassId()));
            if (count == null || count == 0) {
                return ApiResponse.fail(403, "您不是该学生所在班级的教师，无权审批");
            }
        }

        String action = body.get("action"); // APPROVED / REJECTED
        app.setApprovedBy(uid);
        app.setApprovedAt(LocalDateTime.now());
        app.setApproveRemark(body.get("remark"));
        // 审批签字（批准时必填，前端校验）
        if (StringUtils.hasText(body.get("signatureUrl"))) {
            app.setApproveSignatureUrl(body.get("signatureUrl"));
        }

        if ("APPROVED".equals(action)) {
            app.setStatus("APPROVED");
            // 同步更新补批工单状态（如果是临时补批）
            if (app.getIsTemp() == 1) {
                TempSupplementOrder order = supplementMapper.selectOne(
                        new LambdaQueryWrapper<TempSupplementOrder>()
                                .eq(TempSupplementOrder::getLeaveAppId, id)
                                .eq(TempSupplementOrder::getSupplementStatus, "PENDING"));
                if (order != null) {
                    order.setSupplementStatus("APPROVED");
                    order.setSupplementedBy(uid);
                    order.setSupplementedAt(LocalDateTime.now());
                    supplementMapper.updateById(order);
                }
            }
            pushMessage(app.getApplicantId(), uid,
                    "请假审批通过",
                    app.getStudentName() + " 的请假申请已通过，请准时返校。",
                    "LEAVE_APPROVED", id);
        } else {
            app.setStatus("REJECTED");
            pushMessage(app.getApplicantId(), uid,
                    "请假申请已驳回",
                    app.getStudentName() + " 的请假申请被驳回：" + app.getApproveRemark(),
                    "LEAVE_REJECTED", id);
        }
        leaveMapper.updateById(app);
        return ApiResponse.ok(null);
    }

    // ======================== 门卫临时登记 ========================

    /** 门卫登记临时紧急离校（生成补批工单） */
    @Operation(summary = "门卫临时紧急登记离校")
    @PostMapping("/temp-depart")
    public ApiResponse<LeaveApplication> tempDepart(@RequestBody LeaveApplication req) {
        Long uid = StpUtil.getLoginIdAsLong();
        // 角色校验：仅门卫/管理员可临时放行
        String opRole = dataPermissionHelper.current().getRoleCode();
        if (!"GATE".equals(opRole) && !"ADMIN".equals(opRole)) {
            return ApiResponse.fail(403, "仅门卫可登记临时放行");
        }
        // studentId/姓名/班级缺失时从学籍号补全（前端正常会传，这里兜底防止脏数据/500）
        if (req.getStudentId() == null && StringUtils.hasText(req.getStudentNo())) {
            Student stu = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getStudentNo, req.getStudentNo()).last("LIMIT 1"));
            if (stu != null) {
                req.setStudentId(stu.getId());
                if (!StringUtils.hasText(req.getStudentName())) req.setStudentName(stu.getName());
                if (req.getClassId() == null) req.setClassId(stu.getClassId());
                if (!StringUtils.hasText(req.getClassName())) req.setClassName(stu.getClassName());
            }
        }
        if (req.getStudentId() == null) {
            return ApiResponse.fail(400, "未找到该学籍号对应的学生，无法临时放行");
        }
        req.setApplicantId(uid);
        req.setApplicantRole("GATE");
        req.setLeaveNo(genLeaveNo());
        req.setIsTemp(1);
        req.setStatus("TEMP_PENDING");
        req.setTenantId(1L);
        if (!StringUtils.hasText(req.getLeaveType())) req.setLeaveType("PERSONAL"); // 临时放行默认事假
        if (!StringUtils.hasText(req.getReason())) req.setReason("门卫临时紧急放行");
        req.setClassId(resolveClassId(req)); // classId 缺失时从学生档案补全
        LocalDateTime now = LocalDateTime.now();
        req.setDepartAt(now);
        req.setLeaveStart(now);
        if (req.getLeaveEnd() == null) req.setLeaveEnd(now.plusHours(8));
        req.setTempDeadline(now.plusHours(24));
        leaveMapper.insert(req);

        // 创建补批工单
        TempSupplementOrder order = new TempSupplementOrder();
        order.setLeaveAppId(req.getId());
        order.setStudentName(req.getStudentName());
        order.setClassName(req.getClassName());
        order.setGateVerifierId(uid);
        order.setDepartAt(now);
        order.setDeadline(req.getTempDeadline());
        order.setSupplementStatus("PENDING");
        order.setWarnSent(0);
        order.setTenantId(1L);
        supplementMapper.insert(order);

        // 推送班主任补批提醒
        notifyClassTeachers(req.getClassId(), uid,
                "⚠️ 临时离校待补批",
                req.getStudentName() + " 已由门卫临时放行，请在24小时内线上补批。假条号：" + req.getLeaveNo(),
                "TEMP_SUPPLEMENT", req.getId());
        return ApiResponse.ok(req);
    }

    // ======================== 待补批工单 ========================

    /** 查询待补批工单列表（班主任：只看自己管辖班级的临时放行） */
    @Operation(summary = "临时补批工单列表")
    @GetMapping("/supplements")
    public ApiResponse<Page<TempSupplementOrder>> supplements(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        Long uid = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(uid);
        String role = user != null ? user.getRoleCode() : "";

        LambdaQueryWrapper<TempSupplementOrder> w = new LambdaQueryWrapper<TempSupplementOrder>()
                .eq(StringUtils.hasText(status), TempSupplementOrder::getSupplementStatus, status)
                .orderByAsc(TempSupplementOrder::getDeadline);

        // 班主任/教师：只查自己班级的补批工单
        if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            List<Long> classIds = dataPermissionHelper.loadTeacherClassIds(uid);
            if (classIds.isEmpty()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            // 通过 leave_app_id 关联 LeaveApplication → class_id 过滤
            List<LeaveApplication> classLeaves = leaveMapper.selectList(
                    new LambdaQueryWrapper<LeaveApplication>()
                            .in(LeaveApplication::getClassId, classIds)
                            .eq(LeaveApplication::getIsTemp, 1));
            if (classLeaves.isEmpty()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            List<Long> leaveAppIds = classLeaves.stream()
                    .map(LeaveApplication::getId).distinct().collect(java.util.stream.Collectors.toList());
            w.in(TempSupplementOrder::getLeaveAppId, leaveAppIds);
        }
        // GATE/ADMIN：查看全部
        return ApiResponse.ok(supplementMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    // ======================== 私有工具 ========================

    private void pushMessage(Long receiverId, Long senderId, String title, String content,
                             String bizType, Long bizId) {
        pushMessage(receiverId, senderId, title, content, bizType, bizId, null, 2);
    }

    private void pushMessage(Long receiverId, Long senderId, String title, String content,
                             String bizType, Long bizId, String targetRole, Integer priority) {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(receiverId != null ? receiverId : 0L);
        msg.setSenderId(senderId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setBizType(bizType);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        msg.setPriority(priority != null ? priority : 2);
        msg.setTargetRole(targetRole);
        msg.setTenantId(1L);
        messageMapper.insert(msg);
    }

    /**
     * 通知某班级的全部关联教师（班主任优先）。
     * 找不到绑定教师时回退为广播(receiverId=0)，避免消息丢失。
     * TODO: 接入微信订阅消息(subscribeMessage)实现"班主任微信收到通知"，需配置模板ID与用户订阅授权。
     */
    private void notifyClassTeachers(Long classId, Long senderId, String title, String content,
                                     String bizType, Long bizId) {
        if (classId != null) {
            List<TeacherClass> teachers = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>()
                            .eq(TeacherClass::getClassId, classId));
            if (teachers != null && !teachers.isEmpty()) {
                for (TeacherClass tc : teachers) {
                    pushMessage(tc.getTeacherUserId(), senderId, title, content, bizType, bizId,
                            "TEACHER", 1); // 教师审批消息 = 紧急
                }
                return;
            }
        }
        // 回退：广播给所有教师
        pushMessage(0L, senderId, title, content, bizType, bizId, "TEACHER", 1);
    }

    private String leaveTypeText(String type) {
        if ("SICK".equals(type)) return "病假";
        if ("PERSONAL".equals(type)) return "事假";
        return type == null ? "请假" : type;
    }

    /** classId 缺失时从学生档案补全（保证班主任通知/数据权限命中） */
    private Long resolveClassId(LeaveApplication req) {
        if (req.getClassId() != null) return req.getClassId();
        if (req.getStudentId() != null) {
            Student s = studentMapper.selectById(req.getStudentId());
            if (s != null && s.getClassId() != null) {
                if (!StringUtils.hasText(req.getClassName()) && StringUtils.hasText(s.getClassName())) {
                    req.setClassName(s.getClassName());
                }
                return s.getClassId();
            }
        }
        return null;
    }

    /**
     * 构造申请人标签，便于班主任审批/后台识别来源：
     *   家长 → "家长张大大 申请"
     *   老师 → "语文老师张英语 代申请"（带任教学科，取不到则"老师张英语 代申请"）
     *   门卫 → "门卫王大爷 临时放行"
     */
    private String buildApplicantLabel(LeaveApplication leave) {
        String role = leave.getApplicantRole();
        String name = "";
        if (leave.getApplicantId() != null) {
            SysUser u = sysUserMapper.selectById(leave.getApplicantId());
            if (u != null) name = u.getRealName() == null ? "" : u.getRealName();
        }
        if ("GATE".equals(role)) {
            return "门卫" + name + " 临时放行";
        }
        if ("TEACHER".equals(role) || "HEAD_TEACHER".equals(role)) {
            // 取该老师任教学科
            String subject = "";
            if (leave.getApplicantId() != null) {
                TeacherClass tc = teacherClassMapper.selectOne(new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherUserId, leave.getApplicantId())
                        .last("LIMIT 1"));
                if (tc != null && StringUtils.hasText(tc.getSubjectName())) subject = tc.getSubjectName();
            }
            return subject + "老师" + name + " 代申请";
        }
        // 默认家长
        return "家长" + name + " 申请";
    }

    /** 给一批记录填充申请人标签 */
    private void fillApplicantLabels(List<LeaveApplication> list) {
        if (list == null) return;
        for (LeaveApplication la : list) la.setApplicantLabel(buildApplicantLabel(la));
    }
}
