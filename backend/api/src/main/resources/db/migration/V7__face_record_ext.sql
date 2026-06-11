-- 模块: 人脸识别请假离校
-- 功能: 人脸档案补充字段——年级、班主任姓名、与 stu_student 的数据同步
-- 创建: 2026-06-10
-- 作者: 云辰盾项目组

-- face_record 补充字段
ALTER TABLE face_record
  ADD COLUMN grade_name VARCHAR(64) NULL COMMENT '年级名称' AFTER class_name,
  ADD COLUMN head_teacher_name VARCHAR(64) NULL COMMENT '班主任姓名' AFTER grade_name;

-- 从 stu_student 同步年级、班级信息到 face_record（用于列表直接展示，避免多表 JOIN）
-- 注意：仅同步已有的 face_record，后续新增由应用层维护
UPDATE face_record fr
JOIN stu_student ss ON fr.student_no = ss.student_no AND fr.deleted = 0 AND ss.deleted = 0
SET fr.grade_name = ss.grade_name,
    fr.class_name = COALESCE(fr.class_name, ss.class_name),
    fr.class_id = COALESCE(fr.class_id, ss.class_id);

-- 从 teacher_class 同步班主任姓名到 face_record
UPDATE face_record fr
JOIN teacher_class tc ON fr.class_id = tc.class_id AND tc.is_head_teacher = 1 AND tc.deleted = 0 AND fr.deleted = 0
SET fr.head_teacher_name = tc.teacher_name;
