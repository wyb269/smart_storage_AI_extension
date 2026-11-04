-- =====================================================================
-- 人员行为监测功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_person_behavior_tables.sql 已成功执行
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
--   ONLINE，但种子把 last_heartbeat_at 写成了几天前，于是页面整片离线，
--   而右侧视频区却显示在线，两处判定互相矛盾，排查时极易误判成前端 bug。
--   因此本脚本所有心跳一律用 NOW() - INTERVAL 表达，
--   在线区域的心跳必须落在 90 分钟以内。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过；本脚本用 ON CONFLICT DO NOTHING 幂等，
-- 但重复执行不会改动已插入的行，需要改数据请配合下面的 C2b 重对齐段。
SELECT
  (SELECT count(*) FROM ai_person_monitor_point)   AS point_rows,
  (SELECT count(*) FROM ai_person_event)           AS event_rows,
  (SELECT count(*) FROM ai_person_evidence)        AS evidence_rows,
  (SELECT count(*) FROM ai_person_process_record)  AS record_rows;


-- =====================================================================
-- C1  监测区域：12 个
--     状态分布刻意做成「多数在线 + 聚集告警 + 未戴安全帽告警 + 离线 + 未知」，
--     以便同时验证原型上的五种卡片文案：
--     正常 / 人员聚集 / 未戴安全帽 / 离线 / 未知。
--     区域名按原型图取「一号仓库前作业区」「二号仓库门口作业区」这类形态。
-- =====================================================================
INSERT INTO ai_person_monitor_point (
    id, point_code, point_name, warehouse_id, warehouse_name,
    region_id, org_id, camera_id, camera_code, platform_camera_id,
    device_status, alarm_status, alarm_behavior_type,
    enabled, sort_no, last_heartbeat_at, latest_event_at,
    created_by, created_at, remark
)
SELECT
    100 + g.i,
    'AREA-' || lpad(g.i::text, 3, '0'),
    g.warehouse_name || g.suffix,
    g.warehouse_id,
    g.warehouse_name,
    '000000',
    NULL,
    'C' || lpad(g.i::text, 3, '0'),
    'CAM-' || lpad(g.i::text, 3, '0'),
    'C' || lpad(g.i::text, 3, '0'),
    -- 第 2、9 路离线；第 12 路状态未知；其余在线
    CASE
      WHEN g.i IN (2, 9) THEN 'OFFLINE'
      WHEN g.i = 12      THEN 'UNKNOWN'
      ELSE 'ONLINE'
    END,
    -- 第 3、7 路在线且人员聚集告警；第 5、10 路在线且未戴安全帽告警
    CASE WHEN g.i IN (3, 5, 7, 10) THEN 'ACTIVE' ELSE 'NORMAL' END,
    CASE
      WHEN g.i IN (3, 7)  THEN 'CROWD'
      WHEN g.i IN (5, 10) THEN 'NO_HELMET'
      ELSE NULL
    END,
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
           (ARRAY[9001, 9002, 9003, 9004])[1 + (i - 1) % 4]                     AS warehouse_id,
           (ARRAY['一号仓库', '二号仓库', '三号仓库', '散粮仓库'])[1 + (i - 1) % 4] AS warehouse_name,
           (ARRAY['前作业区', '门口作业区', '卸粮坑作业区', '工作塔一层',
                  '罩棚下作业区', '北侧装卸区', '输送廊道', '西门值守区'])[1 + (i - 1) % 8] AS suffix
    FROM generate_series(1, 12) AS i
) g
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  人员行为事件：200 条（与原型图分页「共 200 条」对齐）
--     每 4 条留 1 条未核实、1 条处理中、2 条已处理，
--     用于同时验证三态展示与「查看即标记已处理」的写入链路。
-- =====================================================================
INSERT INTO ai_person_event (
    id, event_no, point_id, camera_id, camera_code, area_name,
    warehouse_id, warehouse_name, region_id, org_id,
    behavior_type, behavior_type_name, event_desc,
    occurred_at, recovered_at, duration_seconds, person_count,
    algorithm_status, algorithm_result, confidence,
    process_status, processed_by, processed_at, process_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    2000 + g.i,
    -- 编号含日期，与原型图的 PR202607290001 同形
    'PR' || to_char(NOW() - make_interval(mins => 50 * g.i), 'YYYYMMDD')
         || lpad(g.i::text, 4, '0'),
    pt.id,
    pt.camera_id,
    pt.camera_code,
    -- 区域名称与摄像头编号一律从 ai_person_monitor_point 派生。
    -- 遮挡那一版用两套独立数组分别生成 camera_code 与 camera_position，
    -- 结果出现「CAM-002 配 一号粮库东门」这种自相矛盾的行，列表看着就是脏数据。
    pt.point_name,
    pt.warehouse_id,
    pt.warehouse_name,
    pt.region_id,
    NULL,
    g.behavior_type,
    g.behavior_type_name,
    pt.point_name || ' 检测到' || g.behavior_type_name || '，已超过判定阈值',
    NOW() - make_interval(mins => 50 * g.i),
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 50 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE 120 END,
    -- 聚集人数只有 CROWD 有；其余留 NULL 而不是 0，
    -- 0 会被读成「画面里没人」，与「该指标不适用」是两回事
    CASE WHEN g.behavior_type = 'CROWD' THEN 5 + (g.i % 6) ELSE NULL END,
    'SUCCEEDED',
    'ANOMALY',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    CASE g.i % 4 WHEN 0 THEN 'PENDING' WHEN 1 THEN 'PROCESSING' ELSE 'DONE' END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE '系统' END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 50 * g.i - 5) END,
    CASE g.i % 4
      WHEN 0 THEN NULL
      WHEN 1 THEN '已通知现场班组长跟进'
      ELSE '查看事件详情后自动标记为已处理'
    END,
    1,
    TRUE,
    'seed',
    NOW()
FROM (
    SELECT i,
           (ARRAY['CROWD', 'NO_HELMET', 'FALL', 'INTRUSION', 'OTHER'])[1 + (i - 1) % 5] AS behavior_type,
           (ARRAY['人员聚集', '未戴安全帽', '倒地', '区域闯入', '其他'])[1 + (i - 1) % 5] AS behavior_type_name
    FROM generate_series(1, 200) AS i
) g
JOIN LATERAL (
    SELECT p.* FROM ai_person_monitor_point p
    WHERE p.created_by = 'seed'
    ORDER BY p.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_person_monitor_point WHERE created_by = 'seed')
    LIMIT 1
) pt ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有事件的区域字段重新按 monitor_point 表校正
--       上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--       因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
-- =====================================================================
UPDATE ai_person_event e
SET point_id       = p.id,
    camera_id      = p.camera_id,
    camera_code    = p.camera_code,
    area_name      = p.point_name,
    warehouse_id   = p.warehouse_id,
    warehouse_name = p.warehouse_name,
    region_id      = p.region_id
FROM ai_person_monitor_point p
WHERE e.point_id = p.id
  AND e.created_by = 'seed'
  AND (e.camera_code IS DISTINCT FROM p.camera_code
       OR e.area_name IS DISTINCT FROM p.point_name);

-- 复核：事件行的摄像头编号与区域名必须一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_person_event e
JOIN ai_person_monitor_point p ON p.id = e.point_id
WHERE e.camera_code <> p.camera_code OR e.area_name <> p.point_name;


-- =====================================================================
-- C2c 心跳续命：把在线区域的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 这是 90 分钟心跳阈值那个坑的另一种形态。
--    C1 的心跳是相对「灌数那一刻的 NOW()」生成的，
--    只要距离上次执行超过 HEARTBEAT_TIMEOUT_MINUTES（前端为 90 分钟），
--    在线区域就会被 computeDisplayStatus 全部判成「离线」，
--    而库里 device_status 依然是 ONLINE，右侧视频区也显示在线，
--    两处互相矛盾，极难一眼看出是时间问题。
--
--    因此每次演示前必须重跑本脚本（或至少执行这一段）。
-- =====================================================================
UPDATE ai_person_monitor_point
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5)),
    latest_event_at   = CASE WHEN sort_no % 3 = 0
                             THEN NOW() - INTERVAL '60 minutes'
                             ELSE latest_event_at END
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线区域心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_person_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条事件两张（行为异常帧 + 正常参考图）
--     只写受控引用（mock://），不写任何真实地址或本地路径。
--     标注图与原图分离存放，不得互相覆盖。
-- =====================================================================
INSERT INTO ai_person_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id,
    e.id,
    'BEHAVIOR_FRAME',
    'mock://person/' || e.id || '/behavior',
    'mock://person/' || e.id || '/marked',
    'mock://person/' || e.id || '/thumb',
    e.occurred_at,
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_person_event e
-- 按灌入者过滤，不写死 id 区间：事件 id 是 2000 + i（i 从 1 起），
-- 实际落在 2001~2200，用 BETWEEN 2000 AND 2200 之类的边界极易差一条。
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_person_evidence (
    id, event_id, evidence_type, original_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    5000 + e.id,
    e.id,
    'NORMAL_REFERENCE',
    'mock://person/' || e.id || '/normal',
    'mock://person/' || e.id || '/normal-t',
    e.occurred_at,
    e.camera_code,
    2,
    'seed',
    NOW()
FROM ai_person_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  处理审计：为非「未核实」的事件各追加一条
--     来源按处理状态分开：DONE 是「查看即标记」的自动记录 → SYSTEM；
--     PROCESSING 是人工跟进 → WEB。两者都记成同一个值的话，
--     事后无法区分「有人判断过这条」和「有人点开看了一眼」。
-- =====================================================================
INSERT INTO ai_person_process_record (
    id, event_id, event_no, status_before, status_after,
    process_result, process_opinion, processed_by, processed_at, source, created_at
)
SELECT
    7000 + e.id,
    e.id,
    e.event_no,
    'PENDING',
    e.process_status,
    e.process_status,
    e.process_opinion,
    e.processed_by,
    e.processed_at,
    CASE WHEN e.process_status = 'PROCESSING' THEN 'WEB' ELSE 'SYSTEM' END,
    e.processed_at
FROM ai_person_event e
WHERE e.process_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 区域状态分布：期望 ONLINE 9 / OFFLINE 2 / UNKNOWN 1，其中 4 路 ACTIVE
--    （2 路 CROWD、2 路 NO_HELMET）
SELECT device_status, alarm_status, alarm_behavior_type, count(*) AS n
FROM ai_person_monitor_point
GROUP BY device_status, alarm_status, alarm_behavior_type
ORDER BY device_status, alarm_status, alarm_behavior_type;

-- 2) ⚠️ 关键复核：在线区域的心跳必须全部落在 90 分钟以内。
--    若本查询返回任何行，说明种子时间写错了，页面会把在线误判成离线。
SELECT point_code, device_status, last_heartbeat_at,
       EXTRACT(EPOCH FROM (NOW() - last_heartbeat_at)) / 60 AS minutes_ago
FROM ai_person_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');

-- 3) 事件与处理状态分布：期望 PENDING 50 / PROCESSING 50 / DONE 100
SELECT process_status, count(*) AS n
FROM ai_person_event
GROUP BY process_status
ORDER BY process_status;

-- 4) 行为类型分布：期望 5 类各 40 条
SELECT behavior_type, behavior_type_name, count(*) AS n
FROM ai_person_event
GROUP BY behavior_type, behavior_type_name
ORDER BY behavior_type;

-- 5) 置信度为空的条数：期望 28 条（i % 7 = 3 且 i ∈ 1..200），用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_person_event WHERE confidence IS NULL;

-- 6) 聚集人数只在 CROWD 上有值：期望 0 行不一致
SELECT count(*) AS person_count_mismatched_rows
FROM ai_person_event
WHERE (behavior_type = 'CROWD' AND person_count IS NULL)
   OR (behavior_type <> 'CROWD' AND person_count IS NOT NULL);

-- 7) 证据与审计行数：期望 evidence 400 行、process_record 150 行
SELECT 'evidence_rows='||count(*) FROM ai_person_evidence;
SELECT 'record_rows='||count(*) FROM ai_person_process_record;
