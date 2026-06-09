package com.yunchendun.modules.permission.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块: 数据权限
 * 功能: 家长学生关联（一个家长可关联多个孩子）
 */
@Data
@TableName("parent_student")
public class ParentStudent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long parentUserId;
    private String parentName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String relation; // FATHER / MOTHER / OTHER
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
