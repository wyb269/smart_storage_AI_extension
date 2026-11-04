-- =====================================================================
-- 地面积水监测功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_ground_water_tables.sql 已成功执行
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 确认这是【演示数据】，不得灌入生产环境。
-- 任一条件为假，立即停止。
--
-- ⚠️ 重复执行防护：所有插入都用 ON CONFLICT (id) DO NOTHING，
--    且只认 created_by = 'seed' 的行。表名逐项显式列出，
--    不使用 LIKE 'ai_water%' 之类的模式匹配。
--
-- ⚠️⚠️ 时间戳必须相对 NOW() 生成，绝不能写死成固定日期 ⚠️️
--   前端 computeDisplayStatus 有一条判定：
--     device_status = 'ONLINE' 但 last_heartbeat_at 距今超过 90 分钟
--     → 页面显示「离线」
--   仓门异动那一版就踩过这个坑：库里 13 个仓门 device_status 明明是
--   ONLINE，但种子把 last_heartbeat_at 写成了几天前，于是页面
--   「16 个仓门 15 个离线」，而右侧视频区却显示「摄像头在线」，
--   两处判定互相矛盾，排查时极易误判成前端 bug。
--   因此本脚本所有心跳一律用 NOW() - INTERVAL 表达，
--   在线点位的心跳必须落在 90 分钟以内。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过；本脚本可安全重跑（ON CONFLICT DO NOTHING + 下方续命段）。
SELECT
  (SELECT count(*) FROM ai_water_monitor_point)  AS point_rows,
  (SELECT count(*) FROM ai_water_event)          AS event_rows,
  (SELECT count(*) FROM ai_water_evidence)       AS evidence_rows,
  (SELECT count(*) FROM ai_water_process_record) AS record_rows;


-- =====================================================================
-- C1  监测点位：12 个
--     状态分布刻意做成「多数在线干燥 + 少量在线积水 + 少量离线 + 一个未知」，
--     以便同时验证四种展示状态与颜色语义。
--     点位名与前端 InMemory / Mock 同一批口径，切换数据源时能逐行对照。
-- =====================================================================
INSERT INTO ai_water_monitor_point (
    id, point_name, warehouse_id, warehouse_name,
    region_id, org_id, camera_id, camera_code,
    device_status, alarm_status, enabled, sort_no,
    last_heartbeat_at, latest_event_at,
    created_by, created_at, remark
)
SELECT
    100 + g.i,
    g.point_name,
    g.warehouse_id,
    g.warehouse_name,
    '000000',
    NULL,
    'C' || lpad(g.i::text, 3, '0'),
    'CAM-' || lpad(g.i::text, 3, '0'),
    -- 第 2、9 路离线；第 12 路状态未知；其余在线
    CASE
      WHEN g.i IN (2, 9)  THEN 'OFFLINE'
      WHEN g.i = 12       THEN 'UNKNOWN'
      ELSE 'ONLINE'
    END,
    -- 第 4、7 路在线且正在积水告警（页面显示「积水」）
    CASE WHEN g.i IN (4, 7) THEN 'ACTIVE' ELSE 'NORMAL' END,
    TRUE,
    g.i,
    -- 心跳：在线一律 1~5 分钟内；离线 200 分钟前（超过 90 分钟阈值）；未知为 NULL
    CASE
      WHEN g.i IN (2, 9) THEN NOW() - INTERVAL '200 minutes'
      WHEN g.i = 12      THEN NULL
      ELSE NOW() - make_interval(mins => 1 + (g.i % 5))
    END,
    CASE WHEN g.i % 3 = 0 THEN NOW() - INTERVAL '60 minutes' ELSE NULL END,
    'seed',
    NOW(),
    '演示数据，非真实设备'
FROM (
    SELECT i,
           (ARRAY[9001, 9002, 9003, 9004])[1 + (i - 1) % 4]              AS warehouse_id,
           (ARRAY['一号粮库', '二号粮库', '三号粮库', '散粮库'])[1 + (i - 1) % 4] AS warehouse_name,
           (ARRAY['主通廊东侧', '主通廊西侧', '主通廊南段', '主通廊北段',
                  '主通廊东南', '主通廊西南', '北通廊东侧', '北通廊西侧',
                  '北通廊南段', '南通廊东侧', '卸粮坑东侧', '工作塔一层'])[i]        AS point_name
    FROM generate_series(1, 12) AS i
) g
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  积水事件：30 条
--     处理状态三态分布：PENDING 7 / PROCESSING 8 / DONE 15，
--     用于验证「查看即标记」只推进 PENDING、不覆盖 PROCESSING。
-- =====================================================================
INSERT INTO ai_water_event (
    id, event_no, point_id, point_name, camera_id, camera_code,
    warehouse_id, warehouse_name, region_id, org_id,
    water_type, water_type_name, event_desc,
    occurred_at, recovered_at, duration_seconds,
    algorithm_status, algorithm_result, confidence,
    process_status, processed_by, processed_at, process_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    2000 + g.i,
    -- 编号含日期，与原型图的 WP202607290001 同形
    'WP' || to_char(NOW() - make_interval(mins => 30 * g.i), 'YYYYMMDD')
         || lpad(g.i::text, 4, '0'),
    p.id,
    p.point_name,
    p.camera_id,
    p.camera_code,
    p.warehouse_id,
    p.warehouse_name,
    p.region_id,
    NULL,
    g.water_type,
    g.water_type_name,
    '画面检测到地面存在积水，湿滑面积超过阈值',
    NOW() - make_interval(mins => 30 * g.i),
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 30 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE 120 END,
    'SUCCEEDED',
    'ANOMALY',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    -- 三态分布：i%4=0 未核实、i%4=1 处理中、其余已处理
    CASE
      WHEN g.i % 4 = 0 THEN 'PENDING'
      WHEN g.i % 4 = 1 THEN 'PROCESSING'
      ELSE 'DONE'
    END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE '系统' END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 30 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE '查看事件详情后自动标记为已处理' END,
    1,
    TRUE,
    'seed',
    NOW()
FROM (
    SELECT i,
           (ARRAY['PONDING', 'LEAK', 'ICE', 'OTHER'])[1 + (i - 1) % 4] AS water_type,
           (ARRAY['积水', '渗漏', '结冰', '其他'])[1 + (i - 1) % 4]    AS water_type_name
    FROM generate_series(1, 30) AS i
) g
JOIN LATERAL (
    SELECT c.* FROM ai_water_monitor_point c
    WHERE c.created_by = 'seed'
    ORDER BY c.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_water_monitor_point WHERE created_by = 'seed')
    LIMIT 1
) p ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有事件的点位字段重新按 monitor_point 表校正
--      上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--      因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
--
--      早年在遮挡那版踩过：用两套独立数组分别生成 camera_code 与位置名，
--      结果出现「CAM-002 配 一号粮库东门」这种自相矛盾的行。
-- =====================================================================
UPDATE ai_water_event e
SET point_id       = p.id,
    point_name     = p.point_name,
    camera_id      = p.camera_id,
    camera_code    = p.camera_code,
    warehouse_id   = p.warehouse_id,
    warehouse_name = p.warehouse_name,
    region_id      = p.region_id
FROM ai_water_monitor_point p
WHERE e.point_id = p.id
  AND e.created_by = 'seed'
  AND (e.point_name IS DISTINCT FROM p.point_name
       OR e.camera_code IS DISTINCT FROM p.camera_code);

-- 复核：事件行的点位名称与摄像头编号必须一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_water_event e
JOIN ai_water_monitor_point p ON p.id = e.point_id
WHERE e.point_name <> p.point_name OR e.camera_code <> p.camera_code;


-- =====================================================================
-- C2c 心跳续命：把在线点位的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 这是指南 6.39 那个坑的另一种形态。
--    C1 的心跳是相对「灌数那一刻的 NOW()」生成的，
--    只要距离上次执行超过 HEARTBEAT_TIMEOUT_MINUTES（前端为 90 分钟），
--    在线点位就会被 computeDisplayStatus 全部判成「离线」，
--    而库里 device_status 依然是 ONLINE，右侧视频区也显示在线，
--    两处互相矛盾，极难一眼看出是时间问题。
--
--    因此每次演示前必须重跑本脚本（或至少执行这一段）。
-- =====================================================================
UPDATE ai_water_monitor_point
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5)),
    latest_event_at   = CASE WHEN sort_no % 3 = 0
                             THEN NOW() - INTERVAL '60 minutes'
                             ELSE latest_event_at END
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线点位心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_water_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条事件一张积水关键帧
--     只写受控引用（mock://），不写任何真实地址或本地路径。
-- =====================================================================
INSERT INTO ai_water_evidence (
    id, event_id, evidence_type, original_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id,
    e.id,
    'WATER_FRAME',
    'mock://ground-water/' || e.id || '/water',
    'mock://ground-water/' || e.id || '/thumb',
    e.occurred_at,
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_water_event e
-- 按灌入者过滤，不写死 id 区间：事件 id 是 2000 + i（i 从 1 起），
-- 实际落在 2001~2030，用 BETWEEN 2000 AND 2030 之类的边界极易差一条。
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  处理审计：为非「未核实」的事件各追加一条
--     处理意见明确标注是「查看即标记」的自动记录，与真实人工结论区分；
--     source 写 SYSTEM，与后端自动标记路径一致。
-- =====================================================================
INSERT INTO ai_water_process_record (
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
    'SYSTEM',
    e.processed_at
FROM ai_water_event e
WHERE e.process_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 点位状态分布：期望 ONLINE 9 / OFFLINE 2 / UNKNOWN 1，其中 2 路 alarm=ACTIVE
SELECT device_status, alarm_status, count(*) AS n
FROM ai_water_monitor_point
GROUP BY device_status, alarm_status
ORDER BY device_status, alarm_status;

-- 2) ⚠️ 关键复核：在线点位的心跳必须全部落在 90 分钟以内。
--    若本查询返回任何行，说明种子时间写错了，页面会把在线误判成离线。
SELECT camera_code, point_name, device_status, last_heartbeat_at,
       EXTRACT(EPOCH FROM (NOW() - last_heartbeat_at)) / 60 AS minutes_ago
FROM ai_water_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');

-- 3) 事件与处理状态分布：期望 PENDING 7 / PROCESSING 8 / DONE 15
SELECT process_status, count(*) AS n
FROM ai_water_event
GROUP BY process_status
ORDER BY process_status;

-- 4) 积水类型分布：期望 4 种各约 7~8 条，保证结论枚举全覆盖
SELECT water_type, water_type_name, count(*) AS n
FROM ai_water_event
GROUP BY water_type, water_type_name
ORDER BY water_type;

-- 5) 置信度为空的条数：期望 4 条，用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_water_event WHERE confidence IS NULL;

-- 6) 重复执行防护自检：本脚本只应产出 12 个 seed 点位与 30 条 seed 事件。
--    多于该数量说明有人手工灌过或区间写错了。
SELECT
  (SELECT count(*) FROM ai_water_monitor_point  WHERE created_by = 'seed') AS seed_points,
  (SELECT count(*) FROM ai_water_event          WHERE created_by = 'seed') AS seed_events,
  (SELECT count(*) FROM ai_water_evidence       WHERE created_by = 'seed') AS seed_evidences;
