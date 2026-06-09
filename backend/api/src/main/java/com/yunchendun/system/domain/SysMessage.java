package com.yunchendun.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模块: 系统
 * 功能: 站内消息
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long receiverId;
    private Long senderId;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
