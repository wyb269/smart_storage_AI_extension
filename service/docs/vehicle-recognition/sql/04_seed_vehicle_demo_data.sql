-- =====================================================================
-- 车辆智能识别功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_vehicle_tables.sql 已成功执行
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 确认这是【演示数据】，不得灌入生产环境。
-- 任一条件为假，立即停止。
--
-- ⚠️⚠️ 时间戳必须相对 NOW()/CURRENT_DATE 生成，绝不能写死成固定日期 ⚠️️
--   两个理由：
--   (a) 左侧「今日识别车辆」按 recognized_at 落在数据库当天过滤；若把识别时间
--       写成固定历史日期，左侧列表永远为空。因此今日记录一律用
--       CURRENT_DATE + (NOW()-CURRENT_DATE)*frac 保证落在 [今天0点, NOW()] 内。
--   (b) 前端 computeDisplayStatus 用 90 分钟心跳阈值判离线；在线点位的心跳必须
--       相对 NOW() 生成并落在 90 分钟内，否则整页点位被判离线（仓门/遮挡都踩过）。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过，先执行 02_ 清理或确认是否要重复插入。
SELECT
  (SELECT count(*) FROM ai_vehicle_monitor_point)  AS point_rows,
  (SELECT count(*) FROM ai_vehicle_event)          AS event_rows,
  (SELECT count(*) FROM ai_vehicle_evidence)       AS evidence_rows,
  (SELECT count(*) FROM ai_vehicle_process_record) AS record_rows;


-- =====================================================================
-- C1  识别点位：6 个大门点位
--     状态分布：多数在线 + 一路离线 + 一路未知，覆盖取流在线判定与点位健康。
-- =====================================================================
INSERT INTO ai_vehicle_monitor_point (
    id, point_code, point_name, warehouse_id, warehouse_name,
    region_id, org_id, camera_code, camera_name, camera_id,
    device_status, alarm_status, enabled, sort_no,
    last_heartbeat_at, latest_event_at, created_by, created_at, remark
)
VALUES
    (501, 'GATE-E-IN',  '东大门入口', 9001, '一号粮库', '000000', NULL, 'CAM-003', '东大门入口立柱', 'C003',
     'ONLINE',  'NORMAL', TRUE, 1, NOW() - INTERVAL '2 minutes',  NULL, 'seed', NOW(), '演示数据，非真实设备'),
    (502, 'GATE-E-OUT', '东大门出口', 9001, '一号粮库', '000000', NULL, 'CAM-004', '东大门出口横杆', 'C004',
     'ONLINE',  'NORMAL', TRUE, 2, NOW() - INTERVAL '4 minutes',  NULL, 'seed', NOW(), '演示数据，非真实设备'),
    (503, 'GATE-W-IN',  '西大门入口', 9002, '二号粮库', '000000', NULL, 'CAM-005', '西大门入口灯杆', 'C005',
     'ONLINE',  'NORMAL', TRUE, 3, NOW() - INTERVAL '6 minutes',  NULL, 'seed', NOW(), '演示数据，非真实设备'),
    (504, 'GATE-W-OUT', '西大门出口', 9002, '二号粮库', '000000', NULL, 'CAM-006', '西大门出口雨棚', 'C006',
     'OFFLINE', 'NORMAL', TRUE, 4, NOW() - INTERVAL '200 minutes', NULL, 'seed', NOW(), '演示数据，非真实设备'),
    (505, 'GATE-S-IN',  '南大门入口', 9001, '一号粮库', '000000', NULL, 'CAM-007', '南大门入口门柱', 'C007',
     'ONLINE',  'NORMAL', TRUE, 5, NOW() - INTERVAL '3 minutes',  NULL, 'seed', NOW(), '演示数据，非真实设备'),
    (506, 'GATE-N-OUT', '北大门出口', 9002, '二号粮库', '000000', NULL, 'CAM-008', '北大门出口临时杆', 'C008',
     'UNKNOWN', 'NORMAL', TRUE, 6, NULL,                          NULL, 'seed', NOW(), '演示数据，非真实设备')
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  识别记录：30 条（12 条今日 + 18 条历史）
--     每 3 条留 1 条待核实，用于验证「查看即标记为已核实」的写入链路。
--     方向由点位名派生（出口=OUT，入口=IN），保证与点位语义一致。
-- =====================================================================
INSERT INTO ai_vehicle_event (
    id, event_no, plate_no, vehicle_type, vehicle_color, direction,
    point_id, gate_name, recognized_at, warehouse_id, warehouse_name, region_id, org_id,
    camera_code, camera_id, event_desc,
    algorithm_status, confidence,
    verify_status, processed_by, processed_at, verify_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    7000 + g.i,
    'VC' || to_char(g.recognized_at, 'YYYYMMDD') || lpad(g.i::text, 4, '0'),
    (ARRAY['黑A00000', '黑A00001', '黑A12345', '黑B67890'])[1 + (g.i - 1) % 4],
    (ARRAY['柴油货车', '空车', '客车车型', '新能源轿车', '未识别车型'])[1 + (g.i - 1) % 5],
    (ARRAY['白色', '黑色', '银色'])[1 + (g.i - 1) % 3],
    CASE WHEN p.point_name LIKE '%出口' THEN 'OUT' ELSE 'IN' END,
    p.id,
    p.point_name,
    g.recognized_at,
    p.warehouse_id,
    p.warehouse_name,
    p.region_id,
    NULL,
    p.camera_code,
    p.camera_id,
    p.point_name || ' 于识别时间捕获车辆并识别车牌、车型与颜色',
    'SUCCEEDED',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    -- 每 3 条留 1 条待核实
    CASE WHEN g.i % 3 = 0 THEN 'PENDING' ELSE 'VERIFIED' END,
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE '系统' END,
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE g.recognized_at + INTERVAL '1 minute' END,
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE '查看识别记录后自动标记为已核实' END,
    1,
    TRUE,
    'seed',
    NOW()
FROM (
    SELECT
        i,
        -- 前 12 条为今日：CURRENT_DATE + (NOW()-CURRENT_DATE)*frac 保证落在今天已过去区间
        -- 其余为历史：往前 1~6 天
        CASE
          WHEN i <= 12 THEN CURRENT_DATE + ((NOW()::timestamp - CURRENT_DATE) * ((i - 1)::numeric / 12))
          ELSE CURRENT_DATE - (((i - 12) % 6 + 1) * INTERVAL '1 day')
                   + (i % 12) * INTERVAL '1 hour'
        END AS recognized_at
    FROM generate_series(1, 30) AS i
) g
JOIN LATERAL (
    SELECT pt.* FROM ai_vehicle_monitor_point pt
    WHERE pt.created_by = 'seed'
    ORDER BY pt.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_vehicle_monitor_point WHERE created_by = 'seed')
    LIMIT 1
) p ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有记录的点位字段重新按 point 表校正
--      上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--      因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
-- =====================================================================
UPDATE ai_vehicle_event e
SET point_id       = p.id,
    gate_name      = p.point_name,
    camera_code    = p.camera_code,
    camera_id      = p.camera_id,
    warehouse_id   = p.warehouse_id,
    warehouse_name = p.warehouse_name,
    region_id      = p.region_id,
    direction      = CASE WHEN p.point_name LIKE '%出口' THEN 'OUT' ELSE 'IN' END
FROM ai_vehicle_monitor_point p
WHERE e.point_id = p.id
  AND e.created_by = 'seed'
  AND (e.gate_name IS DISTINCT FROM p.point_name
       OR e.camera_code IS DISTINCT FROM p.camera_code);

-- 复核：记录行的点位名与摄像头编号必须一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_vehicle_event e
JOIN ai_vehicle_monitor_point p ON p.id = e.point_id
WHERE e.gate_name <> p.point_name OR e.camera_code IS DISTINCT FROM p.camera_code;


-- =====================================================================
-- C2c 心跳续命：把在线点位的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 只要距离上次执行超过 90 分钟，在线点位就会被
--    computeDisplayStatus 全部判成「离线」。因此每次演示前必须重跑本段。
-- =====================================================================
UPDATE ai_vehicle_monitor_point
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5))
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线点位心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_vehicle_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条记录两张（车辆全景图 + 车牌特写）
--     只写受控引用（mock://），不写任何真实地址或本地路径。
-- =====================================================================
INSERT INTO ai_vehicle_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    8000 + e.id,
    e.id,
    'VEHICLE_IMAGE',
    'mock://vehicle/' || e.id || '/full',
    NULL,
    'mock://vehicle/' || e.id || '/full-thumb',
    e.recognized_at,
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_vehicle_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_vehicle_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    9000 + e.id,
    e.id,
    'PLATE_CROP',
    'mock://vehicle/' || e.id || '/plate',
    'mock://vehicle/' || e.id || '/plate-marked',
    NULL,
    e.recognized_at,
    e.camera_code,
    2,
    'seed',
    NOW()
FROM ai_vehicle_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  核实审计：为已核实的记录各追加一条
--     处理意见明确标注是「查看即标记」的自动记录，与真实人工结论区分。
-- =====================================================================
INSERT INTO ai_vehicle_process_record (
    id, event_id, event_no, status_before, status_after,
    process_result, process_opinion, processed_by, processed_at, source, created_at
)
SELECT
    10000 + e.id,
    e.id,
    e.event_no,
    'PENDING',
    'VERIFIED',
    'VERIFIED',
    '查看识别记录后自动标记为已核实',
    e.processed_by,
    e.processed_at,
    'WEB',
    e.processed_at
FROM ai_vehicle_event e
WHERE e.verify_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 点位状态分布：期望 ONLINE 4 / OFFLINE 1 / UNKNOWN 1
SELECT device_status, count(*) AS n
FROM ai_vehicle_monitor_point
GROUP BY device_status
ORDER BY device_status;

-- 2) ⚠️ 关键复核：今日识别记录数必须 > 0，否则左侧「今日识别车辆」为空。
--    期望今日 12 条。
SELECT count(*) AS today_recognized_rows
FROM ai_vehicle_event
WHERE recognized_at >= CURRENT_DATE
  AND recognized_at <  CURRENT_DATE + INTERVAL '1 day';

-- 3) 核实状态分布：期望 PENDING 10 条、VERIFIED 20 条
SELECT verify_status, count(*) AS n
FROM ai_vehicle_event
GROUP BY verify_status;

-- 4) 方向分布：入口点位记 IN、出口点位记 OUT
SELECT direction, count(*) AS n
FROM ai_vehicle_event
GROUP BY direction;

-- 5) 置信度为空的条数：用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_vehicle_event WHERE confidence IS NULL;
