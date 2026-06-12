package com.yunchendun.modules.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunchendun.modules.leave.domain.FaceConsentLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模块: 人脸识别请假离校
 * 功能: 授权同意审计日志 Mapper
 */
@Mapper
public interface FaceConsentLogMapper extends BaseMapper<FaceConsentLog> {
}
