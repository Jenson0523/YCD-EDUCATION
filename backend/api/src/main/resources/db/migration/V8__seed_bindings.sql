-- 模块: 数据权限
-- 功能: 自动补全班级绑定、教师-班级关联、家长-学生关联、历史请假记录修复
-- 创建: 2026-06-10
-- 说明: 如果 teacher_class / parent_student 表为空，自动基于现有数据创建绑定关系

-- ==========================================
-- Step 1: 基于 class_name 为 stu_student 分配 class_id
-- ==========================================
UPDATE stu_student s
SET s.class_id = (
    SELECT cm.new_id FROM (
        SELECT class_name, ROW_NUMBER() OVER (ORDER BY MIN(id)) as new_id
        FROM stu_student
        WHERE class_id IS NULL AND class_name IS NOT NULL AND class_name != '' AND deleted = 0
        GROUP BY class_name
    ) cm
    WHERE cm.class_name = s.class_name
)
WHERE s.class_id IS NULL
  AND s.class_name IS NOT NULL
  AND s.class_name != ''
  AND s.deleted = 0;

-- 处理无班级名称的学生（兜底为 class_id=1）
UPDATE stu_student
SET class_id = 1, class_name = '默认班级'
WHERE class_id IS NULL AND deleted = 0;

-- ==========================================
-- Step 2: 为 HEAD_TEACHER 创建 teacher_class 绑定
-- ==========================================
INSERT IGNORE INTO teacher_class (tenant_id, teacher_user_id, teacher_name, class_id, class_name, is_head_teacher)
SELECT
    1,
    u.id,
    u.real_name,
    s.class_id,
    s.class_name,
    1
FROM sys_user u
CROSS JOIN (SELECT DISTINCT class_id, class_name FROM stu_student WHERE class_id IS NOT NULL AND deleted = 0) s
WHERE u.role_code = 'HEAD_TEACHER'
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM teacher_class tc
      WHERE tc.teacher_user_id = u.id AND tc.class_id = s.class_id AND tc.deleted = 0
  )
ORDER BY u.id, s.class_id;

-- ==========================================
-- Step 3: 为 PARENT 创建 parent_student 绑定
-- ==========================================
INSERT IGNORE INTO parent_student (tenant_id, parent_user_id, parent_name, student_id, student_no, student_name, relation)
SELECT
    1,
    u.id,
    u.real_name,
    s.id,
    s.student_no,
    s.name,
    'OTHER'
FROM sys_user u
CROSS JOIN stu_student s
WHERE u.role_code = 'PARENT'
  AND u.deleted = 0
  AND s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM parent_student ps
      WHERE ps.parent_user_id = u.id AND ps.student_id = s.id AND ps.deleted = 0
  )
LIMIT 50;

-- ==========================================
-- Step 4: 修复历史请假记录中 class_id 为 NULL 的数据
-- ==========================================
UPDATE leave_application la
INNER JOIN stu_student s ON la.student_id = s.id AND s.deleted = 0
SET
    la.class_id   = s.class_id,
    la.class_name = COALESCE(s.class_name, la.class_name)
WHERE la.class_id IS NULL
  AND s.class_id IS NOT NULL
  AND la.deleted = 0;

-- ==========================================
-- Step 5: 修复历史请假记录中 student_id 无对应学生的（极少数）
-- 将其 class_id 设为 -1 以便数据权限排除
-- ==========================================
UPDATE leave_application
SET class_id = -1
WHERE class_id IS NULL AND deleted = 0;

-- ==========================================
-- Step 6: 为 TEACHER 角色也做班级绑定（非班主任的任课教师）
-- ==========================================
INSERT IGNORE INTO teacher_class (tenant_id, teacher_user_id, teacher_name, class_id, class_name, is_head_teacher)
SELECT
    1,
    u.id,
    u.real_name,
    s.class_id,
    s.class_name,
    0
FROM sys_user u
CROSS JOIN (SELECT DISTINCT class_id, class_name FROM stu_student WHERE class_id IS NOT NULL AND deleted = 0) s
WHERE u.role_code = 'TEACHER'
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM teacher_class tc
      WHERE tc.teacher_user_id = u.id AND tc.class_id = s.class_id AND tc.deleted = 0
  )
ORDER BY u.id, s.class_id;
