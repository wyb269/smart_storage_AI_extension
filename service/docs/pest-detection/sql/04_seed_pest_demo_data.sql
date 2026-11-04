-- =====================================================================
-- 粮面虫害智能识别功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_pest_tables.sql 已成功执行
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 确认这是【演示数据】，不得灌入生产环境。
-- 任一条件为假，立即停止。
--
-- ⚠️ 时间戳必须相对 NOW() 生成，绝不能写死成固定日期 ⚠️⚠️
--   前端 computeDisplayStatus 有一条判定：
--     device_status = 'ONLINE' 但 last_heartbeat_at 距今超过 90 分钟
--     → 页面显示「离线」
--   因此本脚本所有心跳一律用 NOW() - INTERVAL 表达，
--   在线点位的心跳必须落在 90 分钟以内。
--
-- ⚠️ 密度阈值 ⚠️
--   业务逻辑的缺省阈值只来自后端 PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD。
--   本脚本里的 5.00 只是【演示行】的阈值快照（ai_pest_event.density_threshold
--   是 NOT NULL 列，必须给值），不代表逻辑默认值来源；改后端常量不影响判定链路。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过，先执行 02_ 清理或确认是否要重复插入。
SELECT
  (SELECT count(*) FROM ai_pest_monitor_point)  AS point_rows,
  (SELECT count(*) FROM ai_pest_event)          AS event_rows,
  (SELECT count(*) FROM ai_pest_evidence)       AS evidence_rows,
  (SELECT count(*) FROM ai_pest_process_record) AS record_rows;


-- =====================================================================
-- C1  粮面点位：9 个
--     状态分布刻意做成「多数在线 + 少量告警 + 少量离线 + 一个未知」，
--     以便同时验证四种展示状态与颜色语义。
-- =====================================================================
INSERT INTO ai_pest_monitor_point (
    id, point_name, warehouse_id, warehouse_name, grain_type, grain_tonnage,
    region_id, org_id, camera_id, camera_code, device_status, alarm_status,
    enabled, sort_no, last_heartbeat_at, latest_event_at,
    created_by, created_at, remark
)
SELECT
    100 + g.i,
    g.warehouse_name || '粮面' || CASE WHEN g.i % 3 = 0 THEN '中' WHEN g.i % 3 = 1 THEN '' ELSE '北' END,
    g.warehouse_id,
    g.warehouse_name,
    (ARRAY['小麦', '玉米', '稻谷'])[1 + (g.i - 1) % 3],
    (ARRAY[5000, 3000, 6000, 8000])[1 + (g.i - 1) % 4],
    '000000',
    NULL,
    'C' || lpad(g.i::text, 3, '0'),
    'CAM-' || lpad(g.i::text, 3, '0'),
    -- 第 5 路离线；第 9 路状态未知；其余在线
    CASE
      WHEN g.i = 5        THEN 'OFFLINE'
      WHEN g.i = 9        THEN 'UNKNOWN'
      ELSE 'ONLINE'
    END,
    -- 第 2、6 路在线且正在告警
    CASE WHEN g.i IN (2, 6) THEN 'ACTIVE' ELSE 'NORMAL' END,
    TRUE,
    g.i,
    -- 心跳：在线一律 1~5 分钟内；离线 200 分钟前（超过 90 分钟阈值）；未知为 NULL
    CASE
      WHEN g.i = 5  THEN NOW() - INTERVAL '200 minutes'
      WHEN g.i = 9  THEN NULL
      ELSE NOW() - make_interval(mins => 1 + (g.i % 5))
    END,
    CASE WHEN g.i % 3 = 0 THEN NOW() - INTERVAL '60 minutes' ELSE NULL END,
    'seed',
    NOW(),
    '演示数据，非真实设备'
FROM (SELECT i FROM generate_series(1, 9) AS i) s
JOIN LATERAL (
    SELECT s.i,
           (ARRAY[9001, 9002, 9003, 9004])[1 + (s.i - 1) % 4]              AS warehouse_id,
           (ARRAY['一号仓', '二号仓', '三号仓', '四号仓'])[1 + (s.i - 1) % 4] AS warehouse_name
) g ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  采样检测事件：30 条
--     结论 4 种轮转；密度超标类 density > 阈值，其余 <= 阈值；
--     处理状态三值分布（每 3 条：PENDING / PROCESSING / DONE）。
-- =====================================================================
INSERT INTO ai_pest_event (
    id, event_no, point_id, camera_id, camera_code, sample_position,
    warehouse_id, warehouse_name, grain_type, region_id, org_id,
    conclusion_type, conclusion_type_name, insect_species,
    density, density_threshold, event_desc,
    occurred_at, recovered_at, duration_seconds,
    algorithm_status, algorithm_result, confidence,
    process_status, processed_by, processed_at, process_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    2000 + g.i,
    -- 编号含日期，与原型图的 PT202607290001 同形
    'PT' || to_char(NOW() - make_interval(mins => 40 * g.i), 'YYYYMMDD')
         || lpad(g.i::text, 4, '0'),
    p.id,
    p.camera_id,
    p.camera_code,
    g.sample_position,
    p.warehouse_id,
    p.warehouse_name,
    p.grain_type,
    p.region_id,
    NULL,
    g.conclusion_type,
    g.conclusion_type_name,
    -- 霉变/其他无虫种；粮面害虫/密度超标给虫种
    CASE WHEN g.conclusion_type IN ('MOLD', 'OTHER') THEN NULL
         ELSE (ARRAY['玉米象', '赤拟谷盗', '谷蠹', '麦蛾'])[1 + g.i % 4] END,
    -- 密度：超标类 = 阈值 + 2，其余在 [0, 阈值] 之间；阈值固定演示值 5.00（见文件头说明）
    CASE WHEN g.conclusion_type = 'DENSITY_HIGH' THEN 7.00
         ELSE (g.i % 5) * 1.00 END,
    5.00,
    '粮面采样检测发现' || g.conclusion_type_name,
    NOW() - make_interval(mins => 40 * g.i),
    NULL,
    NULL,
    'SUCCEEDED',
    'ANOMALY',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    CASE g.i % 3 WHEN 0 THEN 'PENDING' WHEN 1 THEN 'PROCESSING' ELSE 'DONE' END,
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE '系统' END,
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE NOW() - make_interval(mins => 40 * g.i - 10) END,
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE '查看检测记录详情后自动标记为已处理' END,
    1,
    TRUE,
    'seed',
    NOW()
FROM (
    SELECT i,
           (ARRAY['东墙地上层', '中墙下层', '西北墙中层', '东墙混上层', '南墙表层', '中央取样点'])[1 + (i - 1) % 6] AS sample_position,
           (ARRAY['GRAIN_INSECT', 'DENSITY_HIGH', 'MOLD', 'OTHER'])[1 + (i - 1) % 4] AS conclusion_type,
           (ARRAY['粮面害虫', '密度超标', '霉变', '其他或未知'])[1 + (i - 1) % 4] AS conclusion_type_name
    FROM generate_series(1, 30) AS i
) g
JOIN LATERAL (
    SELECT c.* FROM ai_pest_monitor_point c
    WHERE c.created_by = 'seed'
    ORDER BY c.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_pest_monitor_point WHERE created_by = 'seed')
    LIMIT 1
) p ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有事件的点位字段重新按 monitor_point 表校正
--      上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--      因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
-- =====================================================================
UPDATE ai_pest_event e
SET point_id       = p.id,
    camera_id      = p.camera_id,
    camera_code    = p.camera_code,
    warehouse_id   = p.warehouse_id,
    warehouse_name = p.warehouse_name,
    grain_type     = p.grain_type,
    region_id      = p.region_id
FROM ai_pest_monitor_point p
WHERE e.point_id = p.id
  AND e.created_by = 'seed'
  AND (e.camera_code IS DISTINCT FROM p.camera_code
       OR e.warehouse_name IS DISTINCT FROM p.warehouse_name);

-- 复核：事件行的摄像头编号与仓库必须与点位一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_pest_event e
JOIN ai_pest_monitor_point p ON p.id = e.point_id
WHERE e.camera_code <> p.camera_code OR e.warehouse_id <> p.warehouse_id;


-- =====================================================================
-- C2c 心跳续命：把在线点位的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 心跳是相对「灌数那一刻的 NOW()」生成的，
--    只要距上次执行超过 90 分钟，在线点位就会被全部判成「离线」。
--    因此每次演示前必须重跑本脚本（或至少执行这一段）。
-- =====================================================================
UPDATE ai_pest_monitor_point
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5)),
    latest_event_at   = CASE WHEN sort_no % 3 = 0
                             THEN NOW() - INTERVAL '60 minutes'
                             ELSE latest_event_at END
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线点位心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_pest_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条事件一张正常参考图 + 一张异常采样图
--     只写受控引用（mock://），不写任何真实地址或本地路径。
-- =====================================================================
INSERT INTO ai_pest_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id * 10 + 1,
    e.id,
    'NORMAL_REFERENCE',
    'mock://pest/' || e.id || '/normal',
    NULL,
    'mock://pest/' || e.id || '/normal-thumb',
    e.occurred_at - INTERVAL '2 minutes',
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_pest_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_pest_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id * 10 + 2,
    e.id,
    'PEST_FRAME',
    'mock://pest/' || e.id || '/frame',
    'mock://pest/' || e.id || '/marked',
    'mock://pest/' || e.id || '/thumb',
    e.occurred_at,
    e.camera_code,
    2,
    'seed',
    NOW()
FROM ai_pest_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  处理审计：为已处理/处理中的事件各追加一条
--     处理意见明确标注是「查看即标记」的自动记录，与真实人工结论区分。
-- =====================================================================
INSERT INTO ai_pest_process_record (
    id, event_id, event_no, status_before, status_after,
    process_result, process_opinion, processed_by, processed_at, source, created_at
)
SELECT
    4000 + e.id,
    e.id,
    e.event_no,
    'PENDING',
    e.process_status,
    e.process_status,
    e.process_opinion,
    e.processed_by,
    e.processed_at,
    'WEB',
    e.processed_at
FROM ai_pest_event e
WHERE e.process_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 点位状态分布：期望 ONLINE 6 / OFFLINE 1 / UNKNOWN 1，其中 2 路 alarm=ACTIVE
SELECT device_status, alarm_status, count(*) AS n
FROM ai_pest_monitor_point
GROUP BY device_status, alarm_status
ORDER BY device_status, alarm_status;

-- 2) ⚠️ 关键复核：在线点位的心跳必须全部落在 90 分钟以内。
--    若本查询返回任何行，说明种子时间写错了，页面会把在线误判成离线。
SELECT camera_code, device_status, last_heartbeat_at,
       EXTRACT(EPOCH FROM (NOW() - last_heartbeat_at)) / 60 AS minutes_ago
FROM ai_pest_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');

-- 3) 事件处理状态分布：期望 PENDING / PROCESSING / DONE 各 10 条
SELECT process_status, count(*) AS n
FROM ai_pest_event
GROUP BY process_status;

-- 4) 密度超标条数：期望 8 条（DENSITY_HIGH 类，density > density_threshold）
SELECT count(*) AS exceeded_rows
FROM ai_pest_event
WHERE density > density_threshold;

-- 5) 置信度为空的条数：期望 4 条，用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_pest_event WHERE confidence IS NULL;
