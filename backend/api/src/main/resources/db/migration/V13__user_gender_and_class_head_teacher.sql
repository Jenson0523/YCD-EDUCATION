-- V13: 用户管理增强 + 班级管理增强
-- 1. sys_user 增加 gender 字段
-- 2. edu_class 增加 head_teacher_name 字段（用于列表展示班主任姓名）

-- 1. sys_user 增加 gender 字段
ALTER TABLE sys_user
    ADD COLUMN gender VARCHAR(10) DEFAULT NULL COMMENT '性别: MALE-男 FEMALE-女 OTHER-其他';

-- 2. edu_class 增加 head_teacher_name 字段
ALTER TABLE edu_class
    ADD COLUMN head_teacher_name VARCHAR(50) DEFAULT NULL COMMENT '班主任姓名（冗余字段，方便列表展示）';
