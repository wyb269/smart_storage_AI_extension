-- =====================================================================
-- 皮带机下站人监测功能 · 算法任务表回滚脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
--
-- ⚠️ 本脚本会【删除数据】，不可恢复。执行前必须：
--   1. 已获得用户明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 已确认没有真实算法在往这张表写数据。
--
-- 只删本功能新增的这一张表，绝不触碰仓门异动的 ai_algorithm_task、
-- 视频遮挡的 ai_occlusion_algorithm_task，
-- 也不触碰 01 脚本建的那四张表。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认待删对象确实存在 ----------
-- 预期返回 1 行。若数量不符，先停下来核对，不要直接往下执行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'ai_belt_algorithm_task';

-- ---------- 步骤 3：删除 ----------
-- ⚠️ 表名显式写出，不使用任何 LIKE / 通配模式。
DROP TABLE IF EXISTS ai_belt_algorithm_task;

-- ---------- 步骤 4：复核 ----------
-- 预期返回 0 行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'ai_belt_algorithm_task';
