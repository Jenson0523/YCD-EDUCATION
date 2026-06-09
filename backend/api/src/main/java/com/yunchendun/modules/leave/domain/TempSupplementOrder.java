package com.yunchendun.modules.leave.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块: 人脸识别请假离校
 * 功能: 临时放行补批工单
 */
@Data
@TableName("temp_supplement_order")
public class TempSupplementOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long leaveAppId;
    private String studentName;
    private String className;
    private Long gateVerifierId;
    private LocalDateTime departAt;
    private LocalDateTime deadline;
    /** PENDING=待补批 / APPROVED=已补批 / OVERDUE=已超时 */
    private String supplementStatus;
    private Long supplementedBy;
    private LocalDateTime supplementedAt;
    private Integer warnSent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableLogic
    private Integer deleted;
}
