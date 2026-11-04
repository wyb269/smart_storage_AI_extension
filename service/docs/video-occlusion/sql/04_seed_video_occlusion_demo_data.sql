-- =====================================================================
-- 视频遮挡检测功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_video_occlusion_tables.sql 已成功执行
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 确认这是【演示数据】，不得灌入生产环境。
-- 任一条件为假，立即停止。
--
-- ⚠️️ 时间戳必须相对 NOW() 生成，绝不能写死成固定日期 ⚠️⚠️
--   前端 computeDisplayStatus 有一条判定：
--     device_status = 'ONLINE' 但 last_heartbeat_at 距今超过 90 分钟
--     → 页面显示「离线」
--   仓门异动那一版就踩过这个坑：库里 13 个仓门 device_status 明明是
--   ONLINE，但种子把 last_heartbeat_at 写成了几天前，于是页面
--   「16 个仓门 15 个离线」，而右侧视频区却显示「摄像头 CAM-001 在线」，
--   两处判定互相矛盾，排查时极易误判成前端 bug。
--   因此本脚本所有心跳一律用 NOW() - INTERVAL 表达，
--   在线摄像头的心跳必须落在 90 分钟以内。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过，先执行 03_ 清理或确认是否要重复插入。
SELECT
  (SELECT count(*) FROM ai_occlusion_camera)         AS camera_rows,
  (SELECT count(*) FROM ai_occlusion_event)          AS event_rows,
  (SELECT count(*) FROM ai_occlusion_evidence)       AS evidence_rows,
  (SELECT count(*) FROM ai_occlusion_process_record) AS record_rows;


-- =====================================================================
-- C1  摄像头状态：12 路
--     状态分布刻意做成「多数在线 + 少量遮挡 + 少量离线 + 一路未知」，
--     以便同时验证四种展示状态与颜色语义。
-- =====================================================================
INSERT INTO ai_occlusion_camera (
    id, camera_code, camera_name, warehouse_id, warehouse_name,
    region_id, org_id, camera_id, device_status, occlusion_status,
    enabled, sort_no, last_heartbeat_at, latest_event_at,
    created_by, created_at, remark
)
SELECT
    100 + g.i,
    'CAM-' || lpad(g.i::text, 3, '0'),
    g.warehouse_name || g.position,
    g.warehouse_id,
    g.warehouse_name,
    '000000',
    NULL,
    'C' || lpad(g.i::text, 3, '0'),
    -- 第 2、9 路离线；第 12 路状态未知；其余在线
    CASE
      WHEN g.i IN (2, 9)  THEN 'OFFLINE'
      WHEN g.i = 12       THEN 'UNKNOWN'
      ELSE 'ONLINE'
    END,
    -- 第 4、7 路在线且正在遮挡告警
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
           (ARRAY['东门', '西门', '南门', '北门',
                  '主通道西侧', '主通道东侧', '卸粮坑', '工作塔一层'])[1 + (i - 1) % 8] AS position
    FROM generate_series(1, 12) AS i
) g
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  遮挡事件：30 条
--     每 4 条留 1 条未处理，用于验证「查看即标记已处理」的写入链路。
-- =====================================================================
INSERT INTO ai_occlusion_event (
    id, event_no, camera_config_id, camera_id, camera_code, camera_position,
    warehouse_id, warehouse_name, region_id, org_id,
    occlusion_type, occlusion_type_name, event_desc,
    occurred_at, recovered_at, duration_seconds,
    algorithm_status, algorithm_result, confidence,
    process_status, processed_by, processed_at, process_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    2000 + g.i,
    -- 编号含日期，与原型图的 VD202607290001 同形
    'VD' || to_char(NOW() - make_interval(mins => 30 * g.i), 'YYYYMMDD')
         || lpad(g.i::text, 4, '0'),
    cam.id,
    cam.camera_id,
    cam.camera_code,
    -- 摄像头编号与安装位置一律从 ai_occlusion_camera 派生。
    -- 早先用两套独立数组分别生成 camera_code 与 camera_position，
    -- 结果出现「CAM-002 配 一号粮库东门」这种自相矛盾的行
    -- （CAM-002 实际装在二号粮库西门），列表看着就是脏数据。
    cam.camera_name,
    cam.warehouse_id,
    cam.warehouse_name,
    cam.region_id,
    NULL,
    g.occlusion_type,
    g.occlusion_type_name,
    '画面检测到镜头被遮挡，遮挡面积超过阈值',
    NOW() - make_interval(mins => 30 * g.i),
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 30 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE 120 END,
    'SUCCEEDED',
    'ANOMALY',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    CASE WHEN g.i % 4 = 0 THEN 'PENDING' ELSE 'CLOSED' END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE '系统' END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 30 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE '查看事件详情后自动标记为已处理' END,
    1,
    TRUE,
    'seed',
    NOW()
FROM (
    SELECT i,
           (ARRAY['FULL', 'PARTIAL', 'PAINT', 'LENS_COVER', 'OTHER'])[1 + (i - 1) % 5] AS occlusion_type,
           (ARRAY['全幅遮挡', '部分遮挡', '镜头污损', '镜头被遮盖', '其他或未知'])[1 + (i - 1) % 5]
             AS occlusion_type_name
    FROM generate_series(1, 30) AS i
) g
JOIN LATERAL (
    SELECT c.* FROM ai_occlusion_camera c
    WHERE c.created_by = 'seed'
    ORDER BY c.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_occlusion_camera WHERE created_by = 'seed')
    LIMIT 1
) cam ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有事件的摄像头字段重新按 camera 表校正
--      上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--      因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
-- =====================================================================
UPDATE ai_occlusion_event e
SET camera_config_id = c.id,
    camera_id        = c.camera_id,
    camera_code      = c.camera_code,
    camera_position  = c.camera_name,
    warehouse_id     = c.warehouse_id,
    warehouse_name   = c.warehouse_name,
    region_id        = c.region_id
FROM ai_occlusion_camera c
WHERE e.camera_config_id = c.id
  AND e.created_by = 'seed'
  AND (e.camera_code IS DISTINCT FROM c.camera_code
       OR e.camera_position IS DISTINCT FROM c.camera_name);

-- 复核：事件行的摄像头编号与位置必须一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_occlusion_event e
JOIN ai_occlusion_camera c ON c.id = e.camera_config_id
WHERE e.camera_code <> c.camera_code OR e.camera_position <> c.camera_name;


-- =====================================================================
-- C2c 心跳续命：把在线摄像头的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 这是指南 6.39 那个坑的另一种形态。
--    C1 的心跳是相对「灌数那一刻的 NOW()」生成的，
--    只要距离上次执行超过 HEARTBEAT_TIMEOUT_MINUTES（前端为 90 分钟），
--    在线摄像头就会被 computeDisplayStatus 全部判成「离线」，
--    而库里 device_status 依然是 ONLINE，右侧视频区也显示在线，
--    两处互相矛盾，极难一眼看出是时间问题。
--
--    实测复现：10:50 灌数时自检返回 0 行；13:20 重跑时返回 9 行。
--
--    因此每次演示前必须重跑本脚本（或至少执行这一段）。
-- =====================================================================
UPDATE ai_occlusion_camera
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5)),
    latest_event_at   = CASE WHEN sort_no % 3 = 0
                             THEN NOW() - INTERVAL '60 minutes'
                             ELSE latest_event_at END
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线摄像头心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_occlusion_camera
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条事件一张遮挡关键帧
--     只写受控引用（mock://），不写任何真实地址或本地路径。
-- =====================================================================
INSERT INTO ai_occlusion_evidence (
    id, event_id, evidence_type, original_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id,
    e.id,
    'OCCLUDED_FRAME',
    'mock://occlusion/' || e.id || '/occluded',
    'mock://occlusion/' || e.id || '/thumb',
    e.occurred_at,
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_occlusion_event e
-- 按灌入者过滤，不写死 id 区间：事件 id 是 2000 + i（i 从 1 起），
-- 实际落在 2001~2030，用 BETWEEN 2000 AND 2030 之类的边界极易差一条。
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  处理审计：为已处理的事件各追加一条
--     处理意见明确标注是「查看即标记」的自动记录，与真实人工结论区分。
-- =====================================================================
INSERT INTO ai_occlusion_process_record (
    id, event_id, event_no, status_before, status_after,
    process_result, process_opinion, processed_by, processed_at, source, created_at
)
SELECT
    4000 + e.id,
    e.id,
    e.event_no,
    'PENDING',
    'CLOSED',
    'CLOSED',
    '查看事件详情后自动标记为已处理',
    e.processed_by,
    e.processed_at,
    'WEB',
    e.processed_at
FROM ai_occlusion_event e
WHERE e.process_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 摄像头状态分布：期望 ONLINE 9 / OFFLINE 2 / UNKNOWN 1，其中 2 路 occlusion=ACTIVE
SELECT device_status, occlusion_status, count(*) AS n
FROM ai_occlusion_camera
GROUP BY device_status, occlusion_status
ORDER BY device_status, occlusion_status;

-- 2) ⚠️ 关键复核：在线摄像头的心跳必须全部落在 90 分钟以内。
--    若本查询返回任何行，说明种子时间写错了，页面会把在线误判成离线。
SELECT camera_code, device_status, last_heartbeat_at,
       EXTRACT(EPOCH FROM (NOW() - last_heartbeat_at)) / 60 AS minutes_ago
FROM ai_occlusion_camera
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');

-- 3) 事件与处理状态分布：期望 PENDING 8 条、CLOSED 22 条
SELECT process_status, count(*) AS n
FROM ai_occlusion_event
GROUP BY process_status;

-- 4) 置信度为空的条数：期望 4 条，用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_occlusion_event WHERE confidence IS NULL;
