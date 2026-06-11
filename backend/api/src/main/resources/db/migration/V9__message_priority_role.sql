-- 模块: 系统
-- 功能: 消息增加紧急程度、目标角色字段
-- 创建: 2026-06-10
-- 作者: 云辰盾项目组

ALTER TABLE sys_message
    ADD COLUMN priority INT NOT NULL DEFAULT 3 COMMENT '1=紧急 2=重要 3=普通',
    ADD COLUMN target_role VARCHAR(32) NULL COMMENT '目标角色 ALL/TEACHER/PARENT/HEAD_TEACHER，NULL=个人消息',
    ADD KEY idx_sys_message_role_priority (target_role, priority, created_at);
