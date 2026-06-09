package com.yunchendun.common.enums;

/**
 * 模块: 平台级 / common
 * 功能: 统一审批状态枚举
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
public enum ApprovalStatusEnum {
    DRAFT("草稿"),
    PENDING("待审核"),
    PROCESSING("审核中"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    CANCELED("已撤销");

    private final String label;

    ApprovalStatusEnum(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
