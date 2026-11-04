-- =====================================================================
-- 地面积水监测功能 · 建表回滚脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
--
-- ⚠️ 本脚本会【删除数据】，不可恢复。执行前必须：
--   1. 已获得用户明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 已确认没有其它功能依赖这四张表。
--
-- 只删 01 脚本建的四张表，绝不触碰 05 建的 ai_water_algorithm_task，
-- 也不触碰仓门异动 / 视频遮挡 / 其余 aiVision 功能的任何表。
--
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认待删对象确实存在 ----------
-- 预期返回 4 行。若数量不符，先停下来核对，不要直接往下执行。
-- 表名逐项显式列出，不用 LIKE 'ai_water%'：模式匹配会把
-- ai_water_algorithm_task 一起卷进来，那不是本脚本的职责。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_water_monitor_point',
      'ai_water_event',
      'ai_water_evidence',
      'ai_water_process_record'
  )
ORDER BY table_name;

-- ---------- 步骤 3：删除 ----------
-- 逆序删除。本期未建物理外键，顺序只作为习惯保留。
DROP TABLE IF EXISTS ai_water_process_record;
DROP TABLE IF EXISTS ai_water_evidence;
DROP TABLE IF EXISTS ai_water_event;
DROP TABLE IF EXISTS ai_water_monitor_point;

-- ---------- 步骤 4：复核 ----------
-- 预期返回 0 行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_water_monitor_point',
      'ai_water_event',
      'ai_water_evidence',
      'ai_water_process_record'
  );
