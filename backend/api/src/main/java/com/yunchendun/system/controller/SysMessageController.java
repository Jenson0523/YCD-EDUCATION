package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模块: 系统
 * 功能: 站内消息接口（列表、未读数、标已读）
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/messages")
public class SysMessageController {

    private final SysMessageMapper messageMapper;

    @GetMapping
    public ApiResponse<IPage<SysMessage>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(messageMapper.selectPage(
                Page.of(pageNo, pageSize),
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getReceiverId, userId)
                        .orderByDesc(SysMessage::getCreatedAt)));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, userId)
                .eq(SysMessage::getIsRead, false));
        return ApiResponse.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        SysMessage msg = new SysMessage();
        msg.setId(id);
        msg.setIsRead(true);
        messageMapper.updateById(msg);
        return ApiResponse.ok(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> readAll() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysMessage update = new SysMessage();
        update.setIsRead(true);
        messageMapper.update(update, new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, userId)
                .eq(SysMessage::getIsRead, false));
        return ApiResponse.ok(null);
    }
}
