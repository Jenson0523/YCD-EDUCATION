package com.yunchendun.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 模块: 系统权限
 * 功能: 用户账号
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private Long tenantId;
    private String username;
    private String passwordHash;
    private String realName;
    private String mobileEncrypted;
    private String status;
    private String roleCode;
    private String avatar;
    private Long classId;
}
