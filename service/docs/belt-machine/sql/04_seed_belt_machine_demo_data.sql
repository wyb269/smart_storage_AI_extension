-- =====================================================================
-- 皮带机下站人监测功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_belt_machine_tables.sql 已成功执行
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
--   仓门异动与视频遮挡两版都踩过这个坑：库里 device_status 明明是
--   ONLINE，但种子把 last_heartbeat_at 写成了几天前，于是整页只剩离线，
--   正常/报警两态永远演示不出来，排查时极易误判成前端 bug。
--   因此本脚本所有心跳一律用 NOW() - INTERVAL 表达，
--   在线点位的心跳必须落在 90 分钟以内（见 C2c 的续命段与自检）。
--
-- ⚠️ 本脚本产出的每一行都是演示数据，is_simulated 一律写 TRUE。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过；本脚本各 INSERT 均带 ON CONFLICT DO NOTHING，
-- 可安全重跑，但请先确认是否真的要重复灌数。
SELECT
  (SELECT count(*) FROM ai_belt_monitor_point)  AS point_rows,
  (SELECT count(*) FROM ai_belt_event)          AS event_rows,
  (SELECT count(*) FROM ai_belt_evidence)       AS evidence_rows,
  (SELECT count(*) FROM ai_belt_process_record) AS record_rows;


-- =====================================================================
-- C1  皮带机监测点位：12 台
--     状态分布刻意做成「多数在线 + 2 台在线报警 + 2 台离线 + 2 台未部署」，
--     以便同时验证原型图的四种展示态：正常 / 报警 / 离线 / 未部署。
--
--     未部署 = device_status='UNKNOWN' 且 camera_id IS NULL 且 camera_code IS NULL
--     （满足 01 脚本的 ck_belt_point_camera_pair 约束）。
-- =====================================================================
INSERT INTO ai_belt_monitor_point (
    id, device_code, device_name, area_name,
    warehouse_id, warehouse_name, region_id, org_id,
    camera_id, camera_code, camera_name,
    device_status, alarm_status, enabled, sort_no,
    last_heartbeat_at, latest_event_at,
    created_by, created_at, remark
)
SELECT
    100 + g.i,
    'BELT-' || lpad(g.i::text, 3, '0'),
    g.cn_num || '皮带机',
    g.area_name,
    g.warehouse_id,
    g.warehouse_name,
    '000000',
    NULL,
    -- 第 10、12 台未部署：没绑摄像头，camera_id / camera_code 必须同时为空
    CASE WHEN g.i IN (10, 12) THEN NULL ELSE 'C' || lpad(g.i::text, 3, '0') END,
    CASE WHEN g.i IN (10, 12) THEN NULL ELSE 'CAM-' || lpad(g.i::text, 3, '0') END,
    CASE WHEN g.i IN (10, 12) THEN NULL ELSE g.area_name END,
    CASE
      WHEN g.i IN (10, 12) THEN 'UNKNOWN'
      WHEN g.i IN (5, 8)   THEN 'OFFLINE'
      ELSE 'ONLINE'
    END,
    -- 第 3、6 台在线且正在报警
    CASE WHEN g.i IN (3, 6) THEN 'ACTIVE' ELSE 'NORMAL' END,
    TRUE,
    g.i,
    -- 心跳：在线一律 1~5 分钟内；离线 200 分钟前（超过 90 分钟阈值）；未部署为 NULL
    CASE
      WHEN g.i IN (10, 12) THEN NULL
      WHEN g.i IN (5, 8)   THEN NOW() - INTERVAL '200 minutes'
      ELSE NOW() - make_interval(mins => 1 + (g.i % 5))
    END,
    CASE WHEN g.i % 3 = 0 THEN NOW() - INTERVAL '60 minutes' ELSE NULL END,
    'seed',
    NOW(),
    '演示数据，非真实设备'
FROM (
    SELECT i,
           (ARRAY['一号', '二号', '三号', '四号', '五号', '六号',
                  '七号', '八号', '九号', '十号', '十一号', '十二号'])[i] AS cn_num,
           (ARRAY['一号仓库作业区', '二号仓库作业区', '三号仓库作业区', '罩棚作业区',
                  '工作塔一层', '卸粮坑', '主通廊东段', '主通廊西段',
                  '铁路专用线', '汽车接收棚', '烘干塔进料口', '浅圆仓B区'])[i] AS area_name,
           (ARRAY[9001, 9002, 9003, 9004])[1 + (i - 1) % 4]              AS warehouse_id,
           (ARRAY['一号粮库', '二号粮库', '三号粮库', '散粮库'])[1 + (i - 1) % 4] AS warehouse_name
    FROM generate_series(1, 12) AS i
) g
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  皮带机下站人事件：30 条
--     每 4 条留 1 条未核实（PENDING），每 7 条留 1 条处理中（PROCESSING），
--     其余已处理（DONE）——用于验证三值处理状态与「查看即标记」写入链路。
--
--     事件只挂在【已部署】的 10 台皮带机上：未部署的点位没有画面，
--     给它造事件等于伪造检测结论。
-- =====================================================================
INSERT INTO ai_belt_event (
    id, event_no, point_id, device_code, device_name, camera_id, camera_code,
    warehouse_id, warehouse_name, region_id, org_id,
    conclusion_type, conclusion_type_name, event_desc, person_count,
    occurred_at, recovered_at, duration_seconds,
    algorithm_status, algorithm_result, confidence,
    process_status, processed_by, processed_at, process_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    2000 + g.i,
    -- 编号含日期，与原型图的 BC202607290001 同形
    'BC' || to_char(NOW() - make_interval(mins => 30 * g.i), 'YYYYMMDD')
         || lpad(g.i::text, 4, '0'),
    p.id,
    p.device_code,
    -- 皮带机名与摄像头编号一律从 ai_belt_monitor_point 派生。
    -- 视频遮挡那一版早先用两套独立数组分别生成 camera_code 与 camera_position，
    -- 结果出现「CAM-002 配 一号粮库东门」这种自相矛盾的行，列表看着就是脏数据。
    p.device_name,
    p.camera_id,
    p.camera_code,
    p.warehouse_id,
    p.warehouse_name,
    p.region_id,
    NULL,
    g.conclusion_type,
    g.conclusion_type_name,
    '皮带机运行区域检测到人员停留，触发下站人告警',
    -- 约 1/6 的行人数留空，验证页面显示「暂无」而不是 0 人
    CASE WHEN g.i % 6 = 3 THEN NULL ELSE 1 + (g.i % 3) END,
    NOW() - make_interval(mins => 30 * g.i),
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 30 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE 120 END,
    'SUCCEEDED',
    'ANOMALY',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    CASE
      WHEN g.i % 4 = 0 THEN 'PENDING'
      WHEN g.i % 7 = 2 THEN 'PROCESSING'
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
           (ARRAY['STANDING_UNDER', 'CROSSING', 'LOITERING', 'OTHER'])[1 + (i - 1) % 4] AS conclusion_type,
           (ARRAY['皮带机下站人', '跨越运行设备', '区域徘徊', '其他'])[1 + (i - 1) % 4]   AS conclusion_type_name
    FROM generate_series(1, 30) AS i
) g
JOIN LATERAL (
    -- 只在已绑定摄像头（非未部署）的点位里轮转
    SELECT c.* FROM ai_belt_monitor_point c
    WHERE c.created_by = 'seed'
      AND c.camera_id IS NOT NULL
    ORDER BY c.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_belt_monitor_point
                        WHERE created_by = 'seed' AND camera_id IS NOT NULL)
    LIMIT 1
) p ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有事件的点位字段重新按 monitor_point 表校正
--       上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--       因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
-- =====================================================================
UPDATE ai_belt_event e
SET point_id       = p.id,
    device_code    = p.device_code,
    device_name    = p.device_name,
    camera_id      = p.camera_id,
    camera_code    = p.camera_code,
    warehouse_id   = p.warehouse_id,
    warehouse_name = p.warehouse_name,
    region_id      = p.region_id
FROM ai_belt_monitor_point p
WHERE e.point_id = p.id
  AND e.created_by = 'seed'
  AND (e.device_name IS DISTINCT FROM p.device_name
       OR e.camera_code IS DISTINCT FROM p.camera_code);

-- 复核：事件行的皮带机名称与摄像头编号必须与点位表一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_belt_event e
JOIN ai_belt_monitor_point p ON p.id = e.point_id
WHERE e.device_name <> p.device_name
   OR COALESCE(e.camera_code, '') <> COALESCE(p.camera_code, '');


-- =====================================================================
-- C2c 心跳续命：把在线点位的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 这是指南 6.39 那个坑的另一种形态。
--    C1 的心跳是相对「灌数那一刻的 NOW()」生成的，
--    只要距离上次执行超过 HEARTBEAT_TIMEOUT_MINUTES（前端为 90 分钟），
--    在线点位就会被 computeDisplayStatus 全部判成「离线」，
--    而库里 device_status 依然是 ONLINE，右侧视频区也可能显示在线，
--    两处互相矛盾，极难一眼看出是时间问题。
--
--    因此每次演示前必须重跑本脚本（或至少执行这一段）。
-- =====================================================================
UPDATE ai_belt_monitor_point
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5)),
    latest_event_at   = CASE WHEN sort_no % 3 = 0
                             THEN NOW() - INTERVAL '60 minutes'
                             ELSE latest_event_at END
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线点位心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_belt_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条事件两张（正常参考图 + 异常证据图，异常证据图带标注引用）
--     只写受控引用（mock://），不写任何真实地址或本地路径。
-- =====================================================================
INSERT INTO ai_belt_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id,
    e.id,
    'NORMAL_REFERENCE',
    'mock://belt/' || e.id || '/normal',
    NULL,
    'mock://belt/' || e.id || '/normal-thumb',
    e.occurred_at - INTERVAL '2 minutes',
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_belt_event e
-- 按灌入者过滤，不写死 id 区间：事件 id 是 2000 + i（i 从 1 起），
-- 实际落在 2001~2030，用 BETWEEN 2000 AND 2030 之类的边界极易差一条。
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_belt_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3500 + e.id,
    e.id,
    'ANOMALY_FRAME',
    'mock://belt/' || e.id || '/anomaly',
    -- 标注图与原图分离，不覆盖原图
    'mock://belt/' || e.id || '/annotated',
    'mock://belt/' || e.id || '/anomaly-thumb',
    e.occurred_at,
    e.camera_code,
    2,
    'seed',
    NOW()
FROM ai_belt_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  处理审计：为非 PENDING 的事件各追加一条
--     source 一律写 SYSTEM：这批记录是「查看即标记」的自动产物，
--     不是任何人对真实站人/误报做过判断，必须与人工处理（WEB）可区分。
--     处理意见也明确标注，避免污染审计。
-- =====================================================================
INSERT INTO ai_belt_process_record (
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
    '查看事件详情后自动标记为已处理',
    e.processed_by,
    e.processed_at,
    'SYSTEM',
    e.processed_at
FROM ai_belt_event e
WHERE e.process_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 点位展示态分布：期望 ONLINE 8（其中 2 路 alarm=ACTIVE）/
--    OFFLINE 2 / UNKNOWN 2，且 UNKNOWN 的两路 camera_id 为空（未部署）
SELECT device_status, alarm_status,
       (camera_id IS NULL) AS camera_unbound,
       count(*) AS n
FROM ai_belt_monitor_point
GROUP BY device_status, alarm_status, (camera_id IS NULL)
ORDER BY device_status, alarm_status;

-- 2) ⚠️ 关键复核：在线点位的心跳必须全部落在 90 分钟以内。
--    若本查询返回任何行，说明种子时间写错了，页面会把在线误判成离线。
SELECT device_code, device_status, last_heartbeat_at,
       EXTRACT(EPOCH FROM (NOW() - last_heartbeat_at)) / 60 AS minutes_ago
FROM ai_belt_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');

-- 3) 「未部署」判定自检：期望 2 行，且两行的 camera_id / camera_code 均为空
SELECT device_code, device_name, device_status, camera_id, camera_code
FROM ai_belt_monitor_point
WHERE device_status = 'UNKNOWN'
  AND camera_id IS NULL
ORDER BY sort_no;

-- 4) 事件处理状态分布：期望 PENDING 8 / PROCESSING 4 / DONE 18
SELECT process_status, count(*) AS n
FROM ai_belt_event
GROUP BY process_status
ORDER BY process_status;

-- 5) 结论类型分布：期望四类各若干，且只出现四个合法取值
SELECT conclusion_type, count(*) AS n
FROM ai_belt_event
GROUP BY conclusion_type
ORDER BY conclusion_type;

-- 6) 置信度为空的条数：期望 5 条，用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_belt_event WHERE confidence IS NULL;

-- 7) 未部署点位不得有事件：期望 0
SELECT count(*) AS events_on_undeployed_points
FROM ai_belt_event e
JOIN ai_belt_monitor_point p ON p.id = e.point_id
WHERE p.camera_id IS NULL;

-- 8) 审计来源自检：期望只有 SYSTEM 一种（本脚本不产人工处理记录）
SELECT source, count(*) AS n
FROM ai_belt_process_record
GROUP BY source
ORDER BY source;
