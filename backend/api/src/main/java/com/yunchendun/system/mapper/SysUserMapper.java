package com.yunchendun.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunchendun.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模块: 系统权限
 * 功能: 系统用户 Mapper
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    default SysUser selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, "ACTIVE"));
    }
}
