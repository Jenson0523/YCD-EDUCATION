-- V12: 学生档案增加家长自动开通账号功能
-- stu_student: 增加 parent_create_account（是否自动开通家长账号）、parent_user_id（关联的家长用户ID）

ALTER TABLE stu_student
    ADD COLUMN parent_create_account INT DEFAULT 0 COMMENT '是否自动开通家长账号: 0-否 1-是',
    ADD COLUMN parent_user_id        BIGINT DEFAULT NULL COMMENT '关联的家长用户ID(sys_user.id)';
