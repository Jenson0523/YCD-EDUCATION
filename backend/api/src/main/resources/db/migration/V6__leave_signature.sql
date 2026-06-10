-- 模块: 人脸识别请假离校
-- 功能: 审批签字 + 门卫核验补充学籍号字段
-- 创建: 2026-06

-- 请假审批签字图片URL
ALTER TABLE leave_application ADD COLUMN approve_signature_url VARCHAR(500) NULL COMMENT '审批人签字图片URL';

-- 门卫核验记录补充学籍号（便于台账检索/详情展示）
ALTER TABLE gate_verification ADD COLUMN student_no VARCHAR(64) NULL COMMENT '学籍号';
