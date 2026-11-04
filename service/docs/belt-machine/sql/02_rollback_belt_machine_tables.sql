-- =====================================================================
-- 皮带机下站人监测功能 · 建表回滚脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
--
-- ⚠️ 本脚本会【删除数据】，不可恢复。执行前必须：
--   1. 已获得用户明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 已确认没有其它功能依赖这四张表。
--
-- 只删本功能新增的四张表，绝不触碰仓门异动的 ai_door_* / ai_anomaly_*、
-- 视频遮挡的 ai_occlusion_*，也不触碰 05 脚本建的 ai_belt_algorithm_task
-- （那张表由 06_rollback_belt_algorithm_task.sql 单独回滚）。
--
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认待删对象确实存在 ----------
-- 预期返回 4 行。若数量不符，先停下来核对，不要直接往下执行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_belt_monitor_point',
      'ai_belt_event',
      'ai_belt_evidence',
      'ai_belt_process_record'
  )
ORDER BY table_name;

-- ---------- 步骤 3：删除 ----------
-- 逆序删除。本期未建物理外键，顺序只作为习惯保留。
-- ⚠️ 表名逐个显式列出，不使用任何 LIKE / 通配模式。
DROP TABLE IF EXISTS ai_belt_process_record;
DROP TABLE IF EXISTS ai_belt_evidence;
DROP TABLE IF EXISTS ai_belt_event;
DROP TABLE IF EXISTS ai_belt_monitor_point;

-- ---------- 步骤 4：复核 ----------
-- 预期返回 0 行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_belt_monitor_point',
      'ai_belt_event',
      'ai_belt_evidence',
      'ai_belt_process_record'
  );
