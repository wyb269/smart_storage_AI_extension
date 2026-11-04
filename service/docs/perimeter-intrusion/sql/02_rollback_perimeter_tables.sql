-- =====================================================================
-- 周界入侵识别功能 · 回滚脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
--
-- ⚠️ 本脚本会【删除数据】，不可恢复。执行前必须：
--   1. 已获得用户明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 已确认没有其它功能依赖这四张表。
--
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
--
-- ⚠️ 待删对象必须显式列出全部表名，不得使用 LIKE 'ai_perimeter%'：
--    算法任务表 ai_perimeter_algorithm_task 由 06 号脚本单独回滚，
--    用模式匹配会把那张表一起删掉。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认待删对象确实存在 ----------
-- 预期返回 4 行。若数量不符，先停下来核对，不要直接往下执行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_perimeter_monitor_point',
      'ai_perimeter_event',
      'ai_perimeter_evidence',
      'ai_perimeter_process_record'
  )
ORDER BY table_name;

-- ---------- 步骤 3：删除 ----------
-- 逆序删除。本期未建物理外键，顺序只作为习惯保留。
-- 只删本脚本建的四张表，绝不触碰遮挡的 ai_occlusion_* 与仓门的 ai_door_*。
DROP TABLE IF EXISTS ai_perimeter_process_record;
DROP TABLE IF EXISTS ai_perimeter_evidence;
DROP TABLE IF EXISTS ai_perimeter_event;
DROP TABLE IF EXISTS ai_perimeter_monitor_point;

-- ---------- 步骤 4：复核 ----------
-- 预期返回 0 行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_perimeter_monitor_point',
      'ai_perimeter_event',
      'ai_perimeter_evidence',
      'ai_perimeter_process_record'
  );

-- ---------- 步骤 5：复核未误删算法任务表 ----------
-- 预期返回 1 行：ai_perimeter_algorithm_task 仍然存在（由 06 号脚本负责回滚）。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'ai_perimeter_algorithm_task';
