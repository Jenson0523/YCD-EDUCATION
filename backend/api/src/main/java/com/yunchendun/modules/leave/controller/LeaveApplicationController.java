package com.yunchendun.modules.leave.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.common.security.DataPermission;
import com.yunchendun.common.security.DataPermissionHelper;
import com.yunchendun.modules.leave.domain.LeaveApplication;
import com.yunchendun.modules.leave.domain.TempSupplementOrder;
import com.yunchendun.modules.leave.mapper.LeaveApplicationMapper;
import com.yunchendun.modules.leave.mapper.TempSupplementOrderMapper;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.mapper.SysMessageMapper;
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
    private final TempSupplementOrderMapper supplementMapper;
    private final SysMessageMapper messageMapper;
    private final DataPermissionHelper dataPermissionHelper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 10000);

    private String genLeaveNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "YCD-LEAVE-" + date + "-" + String.format("%04d", SEQ.incrementAndGet() % 10000);
    }

    // ======================== 查询 ========================

    /** 我的请假记录（家长/老师：我发起的；班主任：我班级的待审批） */
    @Operation(summary = "请假申请列表")
    @GetMapping
    public ApiResponse<Page<LeaveApplication>> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        Long uid = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<LeaveApplication> w = new LambdaQueryWrapper<LeaveApplication>()
                .eq(StringUtils.hasText(status), LeaveApplication::getStatus, status)
                .eq(classId != null, LeaveApplication::getClassId, classId)
                .like(StringUtils.hasText(keyword), LeaveApplication::getStudentName, keyword)
                .orderByDesc(LeaveApplication::getCreatedAt);

        // 数据权限过滤
        DataPermission dp = dataPermissionHelper.current();
        if (dp.isClass()) {
            // 班主任/任课老师：仅本人关联班级
            if (dp.hasNoClass()) return ApiResponse.ok(new Page<>(pageNo, pageSize));
            w.in(LeaveApplication::getClassId, dp.getClassIds());
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
        return ApiResponse.ok(leaveMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    /** 今日有效假条（给门卫端用） */
    @Operation(summary = "今日有效假条列表（门卫）")
    @GetMapping("/today-valid")
    public ApiResponse<List<LeaveApplication>> todayValid() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<LeaveApplication> list = leaveMapper.selectList(
                new LambdaQueryWrapper<LeaveApplication>()
                        .in(LeaveApplication::getStatus, "APPROVED", "TEMP_PENDING")
                        .ge(LeaveApplication::getLeaveStart, startOfDay)
                        .lt(LeaveApplication::getLeaveStart, endOfDay)
                        .orderByAsc(LeaveApplication::getLeaveStart));
        return ApiResponse.ok(list);
    }

    /** 单条详情 */
    @Operation(summary = "请假申请详情")
    @GetMapping("/{id}")
    public ApiResponse<LeaveApplication> detail(@PathVariable Long id) {
        return ApiResponse.ok(leaveMapper.selectById(id));
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
        leaveMapper.insert(req);

        // 通知班主任（如果 classId 存在则精准推，否则广播给所有班主任 - 此处简化：存系统消息）
        pushMessage(null, uid,
                "新请假申请提醒",
                req.getStudentName() + " 的请假申请待您审批（" + req.getLeaveType() + "）",
                "LEAVE_APPLY", req.getId());
        return ApiResponse.ok(req);
    }

    // ======================== 审批 ========================

    /** 班主任审批（通过/驳回） */
    @Operation(summary = "审批请假申请")
    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LeaveApplication app = leaveMapper.selectById(id);
        if (app == null) return ApiResponse.fail(404, "记录不存在");
        if (!"PENDING".equals(app.getStatus()) && !"TEMP_PENDING".equals(app.getStatus())) {
            return ApiResponse.fail(400, "当前状态不可审批");
        }
        String action = body.get("action"); // APPROVED / REJECTED
        Long uid = StpUtil.getLoginIdAsLong();
        app.setApprovedBy(uid);
        app.setApprovedAt(LocalDateTime.now());
        app.setApproveRemark(body.get("remark"));

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
        req.setApplicantId(uid);
        req.setApplicantRole("GATE");
        req.setLeaveNo(genLeaveNo());
        req.setIsTemp(1);
        req.setStatus("TEMP_PENDING");
        req.setTenantId(1L);
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
        pushMessage(null, uid,
                "⚠️ 临时离校待补批",
                req.getStudentName() + " 已由门卫临时放行，请在24小时内线上补批。假条号：" + req.getLeaveNo(),
                "TEMP_SUPPLEMENT", req.getId());
        return ApiResponse.ok(req);
    }

    // ======================== 待补批工单 ========================

    /** 查询待补批工单列表（班主任） */
    @Operation(summary = "临时补批工单列表")
    @GetMapping("/supplements")
    public ApiResponse<Page<TempSupplementOrder>> supplements(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<TempSupplementOrder> w = new LambdaQueryWrapper<TempSupplementOrder>()
                .eq(StringUtils.hasText(status), TempSupplementOrder::getSupplementStatus, status)
                .orderByAsc(TempSupplementOrder::getDeadline);
        return ApiResponse.ok(supplementMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    // ======================== 私有工具 ========================

    private void pushMessage(Long receiverId, Long senderId, String title, String content,
                             String bizType, Long bizId) {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(receiverId != null ? receiverId : 0L);
        msg.setSenderId(senderId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setBizType(bizType);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        msg.setTenantId(1L);
        messageMapper.insert(msg);
    }
}
