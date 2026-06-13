package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.modules.leave.domain.LeaveApplication;
import com.yunchendun.modules.leave.mapper.LeaveApplicationMapper;
import com.yunchendun.modules.permission.domain.TeacherClass;
import com.yunchendun.modules.permission.mapper.TeacherClassMapper;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysMessageMapper;
import com.yunchendun.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块: 系统
 * 功能: 站内消息接口（列表、未读数、标已读、待办数）
 *       按角色过滤：老师只看教师相关+全校通知，家长只看家长相关+全校通知
 *       待办数直接查 leave_application 表（精确按班级统计，杜绝广播消息虚高）
 *       排序：紧急程度优先 + 时间倒序
 * 创建: 2026-06 / 修复: 2026-06-12
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/messages")
public class SysMessageController {

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final LeaveApplicationMapper leaveAppMapper;

    @GetMapping
    public ApiResponse<IPage<SysMessage>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        String role = user != null && user.getRoleCode() != null ? user.getRoleCode() : "";

        LambdaQueryWrapper<SysMessage> w = new LambdaQueryWrapper<>();

        // 角色过滤：个人消息(receiverId=当前用户) + 按target_role过滤的广播消息
        // 不再使用 isNull(targetRole) 作为后门——所有消息必须在创建时设置 targetRole
        if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)                      // 个人消息
                .or(w3 -> w3
                    .eq(SysMessage::getReceiverId, 0L)
                    .in(SysMessage::getTargetRole, "ALL", "TEACHER", "HEAD_TEACHER"))  // 广播
                .or(w3 -> w3
                    .isNull(SysMessage::getTargetRole)                       // 老数据兼容
                    .eq(SysMessage::getReceiverId, 0L)
                    .notIn(SysMessage::getBizType, "LEAVE_APPLY", "TEMP_SUPPLEMENT"))); // 但不泄审批消息
        } else if ("PARENT".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)                      // 个人消息
                .or(w3 -> w3
                    .eq(SysMessage::getReceiverId, 0L)
                    .in(SysMessage::getTargetRole, "ALL", "PARENT")));       // 家长广播
            // 家长绝对不能看到待审批类的教师专属消息
            w.notIn(SysMessage::getBizType, "LEAVE_APPLY", "TEMP_SUPPLEMENT");
        } else if ("GATE".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or(w3 -> w3
                    .eq(SysMessage::getReceiverId, 0L)
                    .in(SysMessage::getTargetRole, "ALL", "GATE")));
            // 门卫也不应看到待审批消息
            w.notIn(SysMessage::getBizType, "LEAVE_APPLY", "TEMP_SUPPLEMENT");
        } else {
            // 管理员：所有消息（含老数据）
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or().eq(SysMessage::getReceiverId, 0L));
        }

        w.orderByAsc(SysMessage::getPriority)
         .orderByDesc(SysMessage::getCreatedAt);

        return ApiResponse.ok(messageMapper.selectPage(Page.of(pageNo, pageSize), w));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        String role = user != null && user.getRoleCode() != null ? user.getRoleCode() : "";

        LambdaQueryWrapper<SysMessage> w = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getIsRead, 0);

        if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or(w3 -> w3
                    .eq(SysMessage::getReceiverId, 0L)
                    .in(SysMessage::getTargetRole, "ALL", "TEACHER", "HEAD_TEACHER"))
                .or(w3 -> w3
                    .isNull(SysMessage::getTargetRole)
                    .eq(SysMessage::getReceiverId, 0L)
                    .notIn(SysMessage::getBizType, "LEAVE_APPLY", "TEMP_SUPPLEMENT")));
        } else if ("PARENT".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or(w3 -> w3
                    .eq(SysMessage::getReceiverId, 0L)
                    .in(SysMessage::getTargetRole, "ALL", "PARENT")));
            w.notIn(SysMessage::getBizType, "LEAVE_APPLY", "TEMP_SUPPLEMENT");
        } else if ("GATE".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or(w3 -> w3
                    .eq(SysMessage::getReceiverId, 0L)
                    .in(SysMessage::getTargetRole, "ALL", "GATE")));
            w.notIn(SysMessage::getBizType, "LEAVE_APPLY", "TEMP_SUPPLEMENT");
        } else {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or().eq(SysMessage::getReceiverId, 0L));
        }

        long count = messageMapper.selectCount(w);
        return ApiResponse.ok(Map.of("count", count));
    }

    /** 标记单条已读（仅限自己的消息） */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysMessage origin = messageMapper.selectById(id);
        if (origin == null) return ApiResponse.fail(404, "消息不存在");
        // 校验归属：必须是发给当前用户的个人消息；广播消息（receiverId=0）不允许个人标已读
        if (!userId.equals(origin.getReceiverId())) {
            return ApiResponse.fail(403, "无权操作此消息");
        }
        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setIsRead(1);
        messageMapper.updateById(msg);
        return ApiResponse.ok(null);
    }

    /** 全部已读（仅标记个人消息，不触碰广播消息以免影响其他用户） */
    @PutMapping("/read-all")
    public ApiResponse<Void> readAll() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysMessage update = new SysMessage();
        update.setIsRead(1);
        messageMapper.update(update, new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, userId)
                .eq(SysMessage::getIsRead, 0));
        return ApiResponse.ok(null);
    }

    /**
     * 待办数量 — 直接查询 leave_application 表（精确按班级统计）。
     * 
     * 之前查询 sys_message 表会把 receiverId=0 的广播消息全计给每位教师，
     * 导致一位教师看到全校所有待审批（数量虚高 50+）。
     * 现在改为直接查他管辖班级中状态为 PENDING/TEMP_PENDING 的请假单。
     */
    @GetMapping("/pending-count")
    public ApiResponse<Map<String, Long>> pendingCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        String role = user != null && user.getRoleCode() != null ? user.getRoleCode() : "";

        long count = 0;
        if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            // 查出该教师管辖的所有班级ID
            List<Long> classIds = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>()
                            .eq(TeacherClass::getTeacherUserId, userId))
                    .stream()
                    .map(TeacherClass::getClassId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            if (!classIds.isEmpty()) {
                count = leaveAppMapper.selectCount(new LambdaQueryWrapper<LeaveApplication>()
                        .in(LeaveApplication::getStatus, "PENDING", "TEMP_PENDING")
                        .in(LeaveApplication::getClassId, classIds));
            }
        }
        return ApiResponse.ok(Map.of("count", count));
    }
}
