package com.yunchendun.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunchendun.common.api.ApiResponse;
import com.yunchendun.system.domain.SysMessage;
import com.yunchendun.system.domain.SysUser;
import com.yunchendun.system.mapper.SysMessageMapper;
import com.yunchendun.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 模块: 系统
 * 功能: 通知公告（管理员发布，按角色/全员推送）
 *
 * 采用"扇出"方式：为每个目标用户生成一条个人消息（receiverId=用户ID），
 * 这样每位用户可独立标记已读、独立计未读数、首页弹窗只弹自己未读的，
 * 与现有 /sys/messages 的读/未读逻辑完全兼容。
 * 微信订阅消息推送另见 WxNotifyService（需配置模板，本地不可测）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/announcements")
public class AnnouncementController {

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    private static final Set<String> VALID_ROLES =
            Set.of("ADMIN", "PRINCIPAL", "ACADEMIC", "HEAD_TEACHER", "TEACHER", "GATE", "PARENT", "STUDENT");

    /**
     * 发布通知公告（仅管理员/校长/教务）。
     * body: { title, content, targetRoles: ["ALL"] 或 ["PARENT","TEACHER"], priority }
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> publish(@RequestBody Map<String, Object> body) {
        Long uid = StpUtil.getLoginIdAsLong();
        SysUser sender = userMapper.selectById(uid);
        String senderRole = sender != null ? sender.getRoleCode() : "";
        if (!"ADMIN".equals(senderRole) && !"PRINCIPAL".equals(senderRole) && !"ACADEMIC".equals(senderRole)) {
            return ApiResponse.fail(403, "仅管理员可发布通知公告");
        }

        String title = (String) body.get("title");
        String content = (String) body.get("content");
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return ApiResponse.fail(400, "标题和内容不能为空");
        }
        int priority = body.get("priority") != null ? Integer.parseInt(body.get("priority").toString()) : 2;

        // 解析目标角色
        @SuppressWarnings("unchecked")
        List<String> targetRoles = body.get("targetRoles") instanceof List
                ? (List<String>) body.get("targetRoles") : Collections.emptyList();
        if (targetRoles.isEmpty()) return ApiResponse.fail(400, "请选择推送对象");

        // 查询目标用户
        LambdaQueryWrapper<SysUser> uw = new LambdaQueryWrapper<>();
        boolean toAll = targetRoles.contains("ALL");
        if (!toAll) {
            List<String> roles = new ArrayList<>();
            for (String r : targetRoles) if (VALID_ROLES.contains(r)) roles.add(r);
            if (roles.isEmpty()) return ApiResponse.fail(400, "推送对象无效");
            uw.in(SysUser::getRoleCode, roles);
        }
        uw.eq(SysUser::getStatus, "ACTIVE");
        List<SysUser> targets = userMapper.selectList(uw);
        if (targets.isEmpty()) return ApiResponse.fail(400, "没有符合条件的接收用户");

        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (SysUser u : targets) {
            if (u.getId().equals(uid)) continue; // 不给自己发
            SysMessage m = new SysMessage();
            m.setReceiverId(u.getId());
            m.setSenderId(uid);
            m.setTitle(title);
            m.setContent(content);
            m.setBizType("ANNOUNCEMENT");
            m.setBizId(null);
            m.setIsRead(0);
            m.setPriority(priority);
            m.setTargetRole(null); // 个人消息（扇出）
            m.setTenantId(1L);
            m.setCreatedAt(now);
            messageMapper.insert(m);
            count++;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sentCount", count);
        r.put("targetRoles", toAll ? List.of("ALL") : targetRoles);
        return ApiResponse.ok(r);
    }

    /** 已发布公告列表（管理员查看：按标题+时间聚合展示，简单返回去重后的公告） */
    @GetMapping("/sent")
    public ApiResponse<List<Map<String, Object>>> sent(
            @RequestParam(defaultValue = "30") int limit) {
        Long uid = StpUtil.getLoginIdAsLong();
        // 取本人发布的公告，按创建时间倒序，按(标题+时间分钟)聚合统计发送数
        List<SysMessage> msgs = messageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getBizType, "ANNOUNCEMENT")
                .eq(SysMessage::getSenderId, uid)
                .orderByDesc(SysMessage::getCreatedAt)
                .last("LIMIT 2000"));
        // 聚合：key = title|content|分钟级时间
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();
        for (SysMessage m : msgs) {
            String tkey = m.getTitle() + "|" + (m.getCreatedAt() != null
                    ? m.getCreatedAt().toString().substring(0, 16) : "");
            Map<String, Object> g = agg.get(tkey);
            if (g == null) {
                g = new LinkedHashMap<>();
                g.put("title", m.getTitle());
                g.put("content", m.getContent());
                g.put("priority", m.getPriority());
                g.put("createdAt", m.getCreatedAt());
                g.put("total", 0);
                g.put("readCount", 0);
                agg.put(tkey, g);
            }
            g.put("total", (int) g.get("total") + 1);
            if (m.getIsRead() != null && m.getIsRead() == 1) {
                g.put("readCount", (int) g.get("readCount") + 1);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(agg.values());
        if (result.size() > limit) result = result.subList(0, limit);
        return ApiResponse.ok(result);
    }
}
