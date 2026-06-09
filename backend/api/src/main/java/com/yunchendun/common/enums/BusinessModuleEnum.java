package com.yunchendun.common.enums;

/**
 * 模块: 平台级 / common
 * 功能: 十大业务板块枚举
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
public enum BusinessModuleEnum {
    FS("family-school", "/api/fs", "fs_", "家校共同体"),
    EDU("academic", "/api/edu", "edu_", "教务管理"),
    HR("hr", "/api/hr", "hr_", "人事薪资"),
    STU("student", "/api/stu", "stu_", "学生档案"),
    FIN("finance", "/api/fin", "fin_", "财务收费"),
    INS("insurance", "/api/ins", "ins_", "保险服务"),
    LOGI("logistics", "/api/logi", "logi_", "后勤服务"),
    PSY("psychology-safety", "/api/psy", "psy_", "心理安全"),
    ENR("enrollment", "/api/enr", "enr_", "招生升学"),
    DASH("dashboard", "/api/dash", "dash_", "数据驾驶舱");

    private final String directory;
    private final String apiPrefix;
    private final String tablePrefix;
    private final String label;

    BusinessModuleEnum(String directory, String apiPrefix, String tablePrefix, String label) {
        this.directory = directory;
        this.apiPrefix = apiPrefix;
        this.tablePrefix = tablePrefix;
        this.label = label;
    }

    public String directory() {
        return directory;
    }

    public String apiPrefix() {
        return apiPrefix;
    }

    public String tablePrefix() {
        return tablePrefix;
    }

    public String label() {
        return label;
    }
}
