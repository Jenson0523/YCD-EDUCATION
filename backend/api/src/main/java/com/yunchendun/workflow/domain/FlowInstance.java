package com.yunchendun.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunchendun.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 模块: 统一审批流
 * 功能: 审批实例
 * 创建: 2026-06
 * 作者: 云辰盾项目组
 */
@Getter
@Setter
@TableName("flow_instance")
public class FlowInstance extends BaseEntity {
    private String flowCode;
    private String businessModule;
    private String businessType;
    private Long businessId;
    private String title;
    private String status;
    private Long applicantId;
    private Long currentNodeId;
}
