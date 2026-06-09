package com.yunchendun.common.security;

import lombok.Data;
import java.util.List;

/**
 * 模块: 数据权限
 * 功能: 当前登录用户的数据可见范围载体
 *
 * scope 取值:
 *   ALL        - 全部数据（管理员/校长/教务/人事/财务）
 *   CLASS      - 本人关联班级（班主任/任课老师）
 *   SELF       - 本人关联学生（家长/学生）
 *   GATE_VALID - 全校有效假条（门卫）
 */
@Data
public class DataPermission {
    private Long userId;
    private String roleCode;
    private String scope;
    /** 可见班级ID列表（CLASS 范围时有效；空列表=无任何班级） */
    private List<Long> classIds;
    /** 可见学生ID列表（SELF 范围时有效；空列表=无任何学生） */
    private List<Long> studentIds;

    public boolean isAll() { return "ALL".equals(scope); }
    public boolean isClass() { return "CLASS".equals(scope); }
    public boolean isSelf() { return "SELF".equals(scope); }
    public boolean isGate() { return "GATE_VALID".equals(scope); }

    /** CLASS 范围但未分配任何班级 → 不应看到任何数据 */
    public boolean hasNoClass() {
        return isClass() && (classIds == null || classIds.isEmpty());
    }

    /** SELF 范围但未关联任何学生 → 不应看到任何数据 */
    public boolean hasNoStudent() {
        return isSelf() && (studentIds == null || studentIds.isEmpty());
    }
}
