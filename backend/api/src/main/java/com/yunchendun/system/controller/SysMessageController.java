package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysMessageMapper;
import com.yunchendun.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块: 系统
 * 功能: 站内消息接口（列表、未读数、标已读）
 *       按角色过滤：老师只看到教师相关+全校通知，家长只看到家长相关+全校通知
 *       排序：紧急程度优先 + 时间倒序
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/messages")
public class SysMessageController {

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    @GetMapping
    public ApiResponse<IPage<SysMessage>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        String role = user != null && user.getRoleCode() != null ? user.getRoleCode() : "";

        LambdaQueryWrapper<SysMessage> w = new LambdaQueryWrapper<>();

        // 角色过滤：个人消息(receiverId=当前用户) + 按target_role过滤的广播消息
        if ("HEAD_TEACHER".equals(role) || "TEACHER".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)                    // 个人消息
                .or().eq(SysMessage::getReceiverId, 0L)
                     .in(SysMessage::getTargetRole, "ALL", "TEACHER", "HEAD_TEACHER")  // 教师相关广播
                .or().isNull(SysMessage::getTargetRole)                   // 兼容老数据(NULL=通用)
                     .eq(SysMessage::getReceiverId, 0L));
        } else if ("PARENT".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)                    // 个人消息
                .or().eq(SysMessage::getReceiverId, 0L)
                     .in(SysMessage::getTargetRole, "ALL", "PARENT")      // 家长相关广播
                .or().isNull(SysMessage::getTargetRole)                   // 兼容老数据
                     .eq(SysMessage::getReceiverId, 0L));
        } else if ("GATE".equals(role)) {
            // 门卫：个人消息 + 门卫/全校广播（不包括教师专属的待审批消息）
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or().eq(SysMessage::getReceiverId, 0L)
                     .in(SysMessage::getTargetRole, "ALL", "GATE")
                .or().isNull(SysMessage::getTargetRole)
                     .eq(SysMessage::getReceiverId, 0L));
        } else {
            // 管理员：所有消息
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
                .or().eq(SysMessage::getReceiverId, 0L)
                     .in(SysMessage::getTargetRole, "ALL", "TEACHER", "HEAD_TEACHER")
                .or().isNull(SysMessage::getTargetRole)
                     .eq(SysMessage::getReceiverId, 0L));
        } else if ("PARENT".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or().eq(SysMessage::getReceiverId, 0L)
                     .in(SysMessage::getTargetRole, "ALL", "PARENT")
                .or().isNull(SysMessage::getTargetRole)
                     .eq(SysMessage::getReceiverId, 0L));
        } else if ("GATE".equals(role)) {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or().eq(SysMessage::getReceiverId, 0L)
                     .in(SysMessage::getTargetRole, "ALL", "GATE")
                .or().isNull(SysMessage::getTargetRole)
                     .eq(SysMessage::getReceiverId, 0L));
        } else {
            w.and(w2 -> w2
                .eq(SysMessage::getReceiverId, userId)
                .or().eq(SysMessage::getReceiverId, 0L));
        }

        long count = messageMapper.selectCount(w);
        return ApiResponse.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setIsRead(1);
        messageMapper.updateById(msg);
        return ApiResponse.ok(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> readAll() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysMessage update = new SysMessage();
        update.setIsRead(1);
        messageMapper.update(update, new LambdaQueryWrapper<SysMessage>()
                .and(w -> w.eq(SysMessage::getReceiverId, userId)
                           .or().eq(SysMessage::getReceiverId, 0L))
                .eq(SysMessage::getIsRead, 0));
        return ApiResponse.ok(null);
    }
}
