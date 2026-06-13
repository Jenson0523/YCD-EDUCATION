-- V11: 修复历史消息中 target_role=NULL 的数据泄露问题
-- 背景：V9 之前创建的消息 target_role 可能为 NULL，
--       导致 IS NULL 后门使家长/门卫看到教师专属的待审批消息。
-- 修复：根据 biz_type 推断正确的 target_role 并设置。

-- 1. LEAVE_APPLY / TEMP_SUPPLEMENT → 仅教师可见
UPDATE sys_message
   SET target_role = 'TEACHER'
 WHERE target_role IS NULL
   AND biz_type IN ('LEAVE_APPLY', 'TEMP_SUPPLEMENT')
   AND receiver_id = 0;

-- 2. LEAVE_DEPART / LEAVE_APPROVED / LEAVE_REJECTED → 全校可见（家长也需知晓）
UPDATE sys_message
   SET target_role = 'ALL'
 WHERE target_role IS NULL
   AND biz_type IN ('LEAVE_DEPART', 'LEAVE_APPROVED', 'LEAVE_REJECTED', 'LEAVE_ABNORMAL')
   AND receiver_id = 0;

-- 3. HOME_REPORT_REPLY → 家长可见
UPDATE sys_message
   SET target_role = 'PARENT'
 WHERE target_role IS NULL
   AND biz_type = 'HOME_REPORT_REPLY'
   AND receiver_id = 0;

-- 4. 其余未分类广播消息 → 全校可见（保守策略）
UPDATE sys_message
   SET target_role = 'ALL'
 WHERE target_role IS NULL
   AND receiver_id = 0;
