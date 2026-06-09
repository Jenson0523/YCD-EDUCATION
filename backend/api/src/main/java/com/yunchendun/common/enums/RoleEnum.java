package com.yunchendun.common.enums;

/**
 * 模块: 平台级 / common
 * 功能: 11 种角色枚举，禁止数字硬编码
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
public enum RoleEnum {
    PRINCIPAL("校长/股东"),
    ACADEMIC_AFFAIRS("教务处"),
    HR_SPECIALIST("人事专员"),
    FINANCE_SPECIALIST("财务专员"),
    ENROLLMENT("招生办"),
    ADMIN_LOGISTICS("行政/后勤"),
    PSYCHOLOGY_SAFETY("心理/安保/校医"),
    HEAD_TEACHER("班主任"),
    TEACHER("科任教师"),
    PARENT("家长"),
    STUDENT("学生");

    private final String label;

    RoleEnum(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
