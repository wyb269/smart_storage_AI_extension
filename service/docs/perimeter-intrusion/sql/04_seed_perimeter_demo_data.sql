-- =====================================================================
-- 周界入侵识别功能 · 演示种子数据
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：01_create_perimeter_tables.sql 已成功执行
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
--   在线防区段的心跳必须落在 90 分钟以内。
--
-- ⚠️ 本脚本灌完后约 90 分钟，「在线」的防区段会集体变成离线。
--    每次演示前必须重跑本脚本（或至少执行 C2c 那一段心跳续命）。
-- =====================================================================

-- ---------- 步骤 1：门禁校验 ----------
SELECT current_database();

-- ---------- 步骤 2：确认表已存在且为空 ----------
-- 若计数不为 0，说明已灌过；本脚本用 ON CONFLICT DO NOTHING 幂等，
-- 但重复灌之前仍应确认分布是否符合预期。
SELECT
  (SELECT count(*) FROM ai_perimeter_monitor_point)   AS point_rows,
  (SELECT count(*) FROM ai_perimeter_event)           AS event_rows,
  (SELECT count(*) FROM ai_perimeter_evidence)        AS evidence_rows,
  (SELECT count(*) FROM ai_perimeter_process_record)  AS record_rows;


-- =====================================================================
-- C1  防区段状态：12 段，四面防区各 3 段
--     状态分布刻意做成「多数正常布防 + 2 段入侵报警 + 2 段未布防
--     + 2 段离线 + 1 段未知」，以便同时验证四种展示状态与颜色语义。
--
--     卡片标题形如「东防区-东墙段01」，由 zone_cn || '防区-' ||
--     zone_cn || '墙段0N' 拼出。
-- =====================================================================
INSERT INTO ai_perimeter_monitor_point (
    id, point_code, point_name, zone, zone_name, segment_name,
    warehouse_id, warehouse_name, region_id, org_id,
    camera_config_id, camera_code, camera_name, camera_id,
    device_status, alarm_status, defense_status,
    enabled, sort_no, last_heartbeat_at, latest_event_at,
    created_by, created_at, remark
)
SELECT
    100 + g.i,
    'PD-' || lpad(g.i::text, 3, '0'),
    g.zone_cn || '防区-' || g.zone_cn || lpad(g.seg::text, 2, '0'),
    g.zone,
    g.zone_cn || '区',
    g.zone_cn || '墙段' || lpad(g.seg::text, 2, '0'),
    g.warehouse_id,
    g.warehouse_name,
    '000000',
    NULL,
    100 + g.i,
    'CAM-' || lpad(g.i::text, 3, '0'),
    g.zone_cn || '侧围墙中段',
    'C' || lpad(g.i::text, 3, '0'),
    -- 第 2、9 段离线；第 12 段状态未知；其余在线
    CASE
      WHEN g.i IN (2, 9)  THEN 'OFFLINE'
      WHEN g.i = 12       THEN 'UNKNOWN'
      ELSE 'ONLINE'
    END,
    -- 第 4、7 段在线且正在入侵告警
    CASE WHEN g.i IN (4, 7) THEN 'ACTIVE' ELSE 'NORMAL' END,
    -- 第 5、10 段人工撤防：页面必须显示「未布防」，
    -- 这两段特意保持 ONLINE + NORMAL，用来验证「未布防」不是从
    -- 设备态或告警态推出来的，而是独立的人工开关。
    CASE WHEN g.i IN (5, 10) THEN 'DISARMED' ELSE 'ARMED' END,
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
           (ARRAY['EAST', 'WEST', 'SOUTH', 'NORTH'])[1 + ((i - 1) / 3)]        AS zone,
           (ARRAY['东', '西', '南', '北'])[1 + ((i - 1) / 3)]                  AS zone_cn,
           1 + ((i - 1) % 3)                                                   AS seg,
           (ARRAY[9001, 9002, 9003, 9004])[1 + (i - 1) % 4]                    AS warehouse_id,
           (ARRAY['一号粮库', '二号粮库', '三号粮库', '散粮库'])[1 + (i - 1) % 4] AS warehouse_name
    FROM generate_series(1, 12) AS i
) g
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2  入侵事件：30 条
--     每 4 条留 1 条未核实，每 8 条留 1 条处理中，
--     用于验证三值处理状态与「查看即标记已处理」的写入链路。
-- =====================================================================
INSERT INTO ai_perimeter_event (
    id, event_no, point_id, point_name, zone, zone_name,
    camera_config_id, camera_id, camera_code, camera_position,
    warehouse_id, warehouse_name, region_id, org_id,
    intrusion_type, intrusion_type_name, event_desc,
    occurred_at, recovered_at, duration_seconds,
    algorithm_status, algorithm_result, confidence,
    process_status, processed_by, processed_at, process_opinion,
    event_version, is_simulated, created_by, created_at
)
SELECT
    2000 + g.i,
    -- 编号含日期，与原型图的 PD202607290001 同形
    'PD' || to_char(NOW() - make_interval(mins => 30 * g.i), 'YYYYMMDD')
         || lpad(g.i::text, 4, '0'),
    pm.id,
    pm.point_name,
    pm.zone,
    pm.zone_name,
    pm.camera_config_id,
    pm.camera_id,
    pm.camera_code,
    -- 摄像头编号与位置一律从 ai_perimeter_monitor_point 派生。
    -- 遮挡那一版吃过亏：用两套独立数组分别生成 code 与 position，
    -- 结果出现「CAM-002 配 一号粮库东门」这种自相矛盾的行。
    pm.camera_name,
    pm.warehouse_id,
    pm.warehouse_name,
    pm.region_id,
    NULL,
    g.intrusion_type,
    g.intrusion_type_name,
    '周界围栏检测到' || g.intrusion_type_name || '行为，越线持续超过阈值',
    NOW() - make_interval(mins => 30 * g.i),
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE NOW() - make_interval(mins => 30 * g.i - 5) END,
    CASE WHEN g.i % 4 = 0 THEN NULL ELSE 120 END,
    'SUCCEEDED',
    'ANOMALY',
    -- 部分条置信度留空，用于验证页面显示「暂无」而不是 0%
    CASE WHEN g.i % 7 = 3 THEN NULL ELSE 0.8500 END,
    CASE
      WHEN g.i % 4 = 0 THEN 'PENDING'
      WHEN g.i % 8 = 3 THEN 'PROCESSING'
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
           (ARRAY['CLIMB', 'CROSS', 'INTRUSION', 'LOITER', 'OTHER'])[1 + (i - 1) % 5] AS intrusion_type,
           (ARRAY['攀爬', '翻越', '闯入', '徘徊', '其他'])[1 + (i - 1) % 5]            AS intrusion_type_name
    FROM generate_series(1, 30) AS i
) g
JOIN LATERAL (
    SELECT p.* FROM ai_perimeter_monitor_point p
    WHERE p.created_by = 'seed'
    ORDER BY p.sort_no
    OFFSET (g.i - 1) % (SELECT count(*) FROM ai_perimeter_monitor_point WHERE created_by = 'seed')
    LIMIT 1
) pm ON TRUE
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C2b 重对齐：把已有事件的防区段字段重新按 monitor_point 表校正
--       上面 C2 用 ON CONFLICT DO NOTHING，重复执行不会改动已插入的行，
--       因此这里显式再对齐一次，让本脚本可以安全重跑修数据。
-- =====================================================================
UPDATE ai_perimeter_event e
SET point_id         = p.id,
    point_name       = p.point_name,
    zone             = p.zone,
    zone_name        = p.zone_name,
    camera_config_id = p.camera_config_id,
    camera_id        = p.camera_id,
    camera_code      = p.camera_code,
    camera_position  = p.camera_name,
    warehouse_id     = p.warehouse_id,
    warehouse_name   = p.warehouse_name,
    region_id        = p.region_id
FROM ai_perimeter_monitor_point p
WHERE e.point_id = p.id
  AND e.created_by = 'seed'
  AND (e.camera_code IS DISTINCT FROM p.camera_code
       OR e.point_name IS DISTINCT FROM p.point_name);

-- 复核：事件行的防区段名称与摄像头编号必须一一对应，期望 0 行不一致
SELECT count(*) AS mismatched_event_rows
FROM ai_perimeter_event e
JOIN ai_perimeter_monitor_point p ON p.id = e.point_id
WHERE e.camera_code <> p.camera_code OR e.point_name <> p.point_name;


-- =====================================================================
-- C2c 心跳续命：把在线防区段的心跳重新拉回当前时刻
--
-- ⚠️ 种子数据会腐烂 —— 这是指南 6.39 那个坑的另一种形态。
--    C1 的心跳是相对「灌数那一刻的 NOW()」生成的，
--    只要距离上次执行超过 HEARTBEAT_TIMEOUT_MINUTES（前端为 90 分钟），
--    在线防区段就会被判成「离线」，而库里 device_status 依然是 ONLINE，
--    两处互相矛盾，极难一眼看出是时间问题。
--
--    因此每次演示前必须重跑本脚本（或至少执行这一段）。
-- =====================================================================
UPDATE ai_perimeter_monitor_point
SET last_heartbeat_at = NOW() - make_interval(mins => 1 + (sort_no % 5)),
    latest_event_at   = CASE WHEN sort_no % 3 = 0
                             THEN NOW() - INTERVAL '60 minutes'
                             ELSE latest_event_at END
WHERE created_by = 'seed'
  AND device_status = 'ONLINE';

-- 复核：在线防区段心跳必须全部落在 90 分钟内，期望 0
SELECT 'online_stale_heartbeats='||count(*)
FROM ai_perimeter_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');


-- =====================================================================
-- C3  证据：每条事件一张入侵关键帧 + 一张正常参考图
--     只写受控引用（mock://），不写任何真实地址或本地路径。
-- =====================================================================
INSERT INTO ai_perimeter_evidence (
    id, event_id, evidence_type, original_ref, annotated_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3000 + e.id,
    e.id,
    'INTRUSION_FRAME',
    'mock://perimeter/' || e.id || '/intrusion',
    'mock://perimeter/' || e.id || '/marked',
    'mock://perimeter/' || e.id || '/thumb',
    e.occurred_at,
    e.camera_code,
    1,
    'seed',
    NOW()
FROM ai_perimeter_event e
-- 按灌入者过滤，不写死 id 区间：事件 id 是 2000 + i（i 从 1 起），
-- 实际落在 2001~2030，用 BETWEEN 2000 AND 2030 之类的边界极易差一条。
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_perimeter_evidence (
    id, event_id, evidence_type, original_ref, thumbnail_ref,
    captured_at, camera_code, sort_no, created_by, created_at
)
SELECT
    3500 + e.id,
    e.id,
    'NORMAL_REFERENCE',
    'mock://perimeter/' || e.id || '/normal',
    'mock://perimeter/' || e.id || '/normal-t',
    e.occurred_at - INTERVAL '2 minutes',
    e.camera_code,
    2,
    'seed',
    NOW()
FROM ai_perimeter_event e
WHERE e.created_by = 'seed'
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- C4  处理审计：为非「未核实」的事件各追加一条
--     处理意见明确标注是「查看即标记」的自动记录，与真实人工结论区分。
-- =====================================================================
INSERT INTO ai_perimeter_process_record (
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
FROM ai_perimeter_event e
WHERE e.process_status <> 'PENDING'
  AND e.processed_at IS NOT NULL
ON CONFLICT (id) DO NOTHING;


-- =====================================================================
-- 复核
-- =====================================================================
-- 1) 防区段分布：期望四面防区各 3 段
SELECT zone, zone_name, count(*) AS n
FROM ai_perimeter_monitor_point
GROUP BY zone, zone_name
ORDER BY zone;

-- 2) 页面四态分布：期望 正常布防 5 / 入侵报警 2 / 未布防 2 / 离线 2 / 未知 1
--    判定口径与前端本功能目录内的包装层一致：
--    DISARMED 优先，其次设备离线或心跳超时，再次 ACTIVE 告警。
SELECT CASE
         WHEN defense_status = 'DISARMED' THEN '未布防'
         WHEN device_status <> 'ONLINE'
              OR last_heartbeat_at IS NULL
              OR NOW() - last_heartbeat_at > INTERVAL '90 minutes' THEN '离线'
         WHEN alarm_status = 'ACTIVE' THEN '入侵报警'
         WHEN device_status = 'UNKNOWN' THEN '未知'
         ELSE '正常布防'
       END AS display_status,
       count(*) AS n
FROM ai_perimeter_monitor_point
GROUP BY 1
ORDER BY 1;

-- 3) ⚠️ 关键复核：在线防区段的心跳必须全部落在 90 分钟以内。
--    若本查询返回任何行，说明种子时间写错了，页面会把在线误判成离线。
SELECT point_code, device_status, last_heartbeat_at,
       EXTRACT(EPOCH FROM (NOW() - last_heartbeat_at)) / 60 AS minutes_ago
FROM ai_perimeter_monitor_point
WHERE device_status = 'ONLINE'
  AND (last_heartbeat_at IS NULL
       OR NOW() - last_heartbeat_at > INTERVAL '90 minutes');

-- 4) 事件处理状态分布：期望 PENDING 7 / PROCESSING 3 / DONE 20
SELECT process_status, count(*) AS n
FROM ai_perimeter_event
GROUP BY process_status
ORDER BY process_status;

-- 5) 入侵类型分布：期望 5 类各 6 条
SELECT intrusion_type, intrusion_type_name, count(*) AS n
FROM ai_perimeter_event
GROUP BY intrusion_type, intrusion_type_name
ORDER BY intrusion_type;

-- 6) 置信度为空的条数：期望 4 条，用于验证页面显示「暂无」
SELECT count(*) AS null_confidence_rows
FROM ai_perimeter_event WHERE confidence IS NULL;

-- 7) 事件防区分布：用于确认历史页防区下拉四个选项都有数据
SELECT zone, zone_name, count(*) AS n
FROM ai_perimeter_event
GROUP BY zone, zone_name
ORDER BY zone;
