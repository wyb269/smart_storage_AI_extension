-- =====================================================================
-- 仓门异动功能 · 回滚脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 依据：AI_HANDOFF.md 第 39.7 节「删除与回滚规则」
--
-- ======================== 高危操作警告 ========================
-- 本脚本包含破坏性操作（DROP TABLE）。
--
-- 依第 39.7 节：
--   1. DROP DATABASE 是破坏性操作，即使当前为空也必须再次取得
--      用户针对删除动作的明确确认；
--   2. 删除前必须确认当前不存在有效连接；
--   3. 必须再次核对目标名称严格等于 smart_storage_ai_extension，
--      不得使用变量、通配符或相似名称推断目标。
--
-- 本脚本仅删除本功能自建的 6 张表，不触碰其他任何对象。
-- 它不是 DROP DATABASE，不影响库中其他内容。
-- =====================================================================


-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension

SELECT current_database();


-- ---------- 步骤 2：确认无有效连接（人工确认，非 SQL 可判定） ----------
-- 请在执行前于操作系统或数据库客户端侧确认：
--   - 应用进程已停止
--   - 无其他会话正在使用 ai_door% 相关表
-- 可辅助执行：
SELECT count(*) AS active_connections
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid <> pg_backend_pid();


-- ---------- 步骤 3：列出将被删除的对象（人工核对） ----------
-- 执行前请逐行核对，确认只有本功能自建的 6 张表。

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name LIKE 'ai_door%'
ORDER BY table_name;


-- =====================================================================
-- 以下 DDL 仅在步骤 1-3 全部通过、且已获得明确授权后执行
--
-- 顺序：先删依赖方，再删被依赖方
-- =====================================================================

DROP TABLE IF EXISTS ai_event_process_record;
DROP TABLE IF EXISTS ai_event_evidence;
DROP TABLE IF EXISTS ai_anomaly_event;
DROP TABLE IF EXISTS ai_algorithm_task;
DROP TABLE IF EXISTS ai_camera_mapping;
DROP TABLE IF EXISTS ai_door_config;


-- ---------- 回滚后自检：预期返回 0 行 ----------
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name LIKE 'ai_door%'
ORDER BY table_name;


-- =====================================================================
-- 附：如需撤销整个数据库（本功能之外的更彻底回滚）
-- =====================================================================
-- ⚠️⚠️ 极高危：不得随本脚本自动执行 ⚠️⚠️
--
-- DROP DATABASE smart_storage_ai_extension;
--
-- 依第 39.7 节，此动作必须由用户针对该动作单独、明确授权，
-- 且需先确认无任何有效连接。本脚本不包含该语句，
-- 也不得由 AI 自行补全执行。
