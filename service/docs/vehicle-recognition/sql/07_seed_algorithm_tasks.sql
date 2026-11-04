-- =====================================================================
-- 车辆智能识别功能 · 算法任务历史数据灌入（07）
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：vehicleRecognition   任务表：ai_vehicle_algorithm_task（05 脚本建表）
-- task_type：VEHICLE_RECOGNIZE（本功能语义名，不与其它功能共用）
--
-- 内容：为库里现有的每条 seed 识别记录（当前 30 条，≤40，按契约全量补齐）
--       各补 1 条历史算法任务，并回填事件行的
--       algorithm_task_id / algorithm_status / confidence。
--       「重新识别」产生的任务（task_no = MOCK-TASK-…）不在本脚本管辖范围内，
--       DELETE 只按本脚本自己的前缀 VCAT 圈定，不会误删。
--
-- ⚠️ 车辆事件表没有 algorithm_result 列（结论是 RECOGNIZED/NO_VEHICLE，
--    落在任务表 conclusion 上），回填时不要照抄其它功能的 SQL。
--
-- 状态分布（契约 §2，约 85/5/5/5）：
--   SUCCEEDED 26（86.7%）/ FAILED 1 / TIMEOUT 1 / RUNNING 1 / QUEUED 1
--
-- ⚠️ 时间必须相对 NOW() 生成，绝不写死日期。
-- ⚠️ 幂等：先按 task_no 前缀 DELETE 再 INSERT（id 固定 710000+rn）。
--
-- 授权：本步只写数据，不新建表；zhlc 的 DML 权限已由 05 脚本 GRANT。
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 05_create_vehicle_algorithm_task.sql 已执行（任务表存在）；
--   4. 04_seed_vehicle_demo_data.sql 已执行（识别记录存在）。
-- 任一条件为假，立即停止。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
SELECT current_database();

-- ---------- 步骤 2：前置对象自检 ----------
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('ai_vehicle_event', 'ai_vehicle_algorithm_task')
ORDER BY table_name;

-- 识别记录规模确认：期望 30
SELECT count(*) AS seed_event_rows FROM ai_vehicle_event WHERE created_by = 'seed';


-- ---------- 步骤 3：删除本脚本管辖的旧数据（只按自己的前缀） ----------
DELETE FROM ai_vehicle_algorithm_task WHERE task_no LIKE 'VCAT%';


-- =====================================================================
-- 步骤 4：插入 30 条历史任务
-- =====================================================================
INSERT INTO ai_vehicle_algorithm_task (
    id, task_no, event_id, point_id, camera_id, camera_code,
    warehouse_id, region_id, org_id,
    task_type, media_type, media_refs, normal_reference_ref, roi,
    execution_status, conclusion, vehicle_type, confidence,
    model_name, model_version, algorithm_version, config_version, confidence_threshold,
    media_digest, started_at, finished_at, inference_duration_ms,
    detections, evidence_refs, raw_result_ref,
    error_code, error_message, retry_count, trace_id,
    is_simulated, created_by, created_at, updated_by, updated_at
)
SELECT
    710000 + b.rn,
    'VCAT' || to_char(NOW(), 'YYYYMMDD') || lpad(b.rn::text, 4, '0'),
    b.event_id,
    b.point_id,
    b.camera_id,
    b.camera_code,
    b.warehouse_id,
    b.region_id,
    b.org_id,

    'VEHICLE_RECOGNIZE',
    'SINGLE_IMAGE',
    -- 受控内部引用：只允许 mock:// 形态，不出现 http/本地绝对路径
    '[{"ref":"mock://vehicle/' || b.event_id || '/full","type":"VEHICLE_IMAGE"},'
    || '{"ref":"mock://vehicle/' || b.event_id || '/plate","type":"PLATE_CROP"}]',
    'mock://vehicle/' || b.event_id || '/normal',
    '[80,100,720,560]',

    b.st,
    b.conclusion,
    b.vehicle_type_out,
    b.confidence,

    'mock-vehicle-recognizer',
    'mock-1.0.0',
    'mock-adapter-1.0.0',
    'vehicle-cfg-1.0',
    0.7500,
    'vc-digest-' || md5(b.event_id::text),

    b.started_at,
    -- 与契约一致：失败/超时/进行中都没有 finished_at，只有成功才有
    CASE WHEN b.st = 'SUCCEEDED'
         THEN b.started_at + make_interval(secs => b.duration_ms / 1000.0)
         ELSE NULL END,
    b.duration_ms,

    -- 车牌在算法任务侧永不落原文（PII）：只写掩码（首 2 + *** + 末 2）
    CASE
      WHEN b.st = 'SUCCEEDED' THEN
        '[{"category":"VEHICLE","bbox":[90,120,640,520],"confidence":0.93},'
        || '{"category":"PLATE","bbox":[300,360,470,420],"confidence":0.88,'
        || '"plate_masked":"' || b.plate_masked || '"}]'
      WHEN b.st IN ('FAILED', 'TIMEOUT') THEN '[]'
      ELSE NULL
    END,
    CASE WHEN b.st = 'SUCCEEDED' THEN
      '[{"evidenceType":"VEHICLE_IMAGE","objectKey":"mock://vehicle/'
      || b.event_id || '/full"},'
      || '{"evidenceType":"PLATE_CROP","objectKey":"mock://vehicle/'
      || b.event_id || '/plate"}]'
    ELSE NULL END,
    CASE WHEN b.st = 'SUCCEEDED'
         THEN 'mock://task/VCAT-' || b.rn || '/raw'
         ELSE NULL END,

    CASE b.st WHEN 'FAILED'  THEN 'SEED_INFERENCE_ERROR'
              WHEN 'TIMEOUT' THEN 'SEED_TIMEOUT'
              ELSE NULL END,
    CASE b.st WHEN 'FAILED'  THEN '模拟推理失败：车牌区域定位异常（历史演示数据）'
              WHEN 'TIMEOUT' THEN '模拟算法执行超时（历史演示数据）'
              ELSE NULL END,
    0,
    'vc-trace-' || to_char(NOW(), 'YYYYMMDD') || '-' || lpad(b.rn::text, 4, '0'),

    -- 铁律 1：全部模拟结果，is_simulated 必须为 TRUE
    TRUE,
    'seed',
    NOW(),
    'seed',
    NOW()
FROM (
    SELECT
        s.*,
        CASE WHEN s.st = 'SUCCEEDED'
             THEN (0.55 + ((s.rn * 19) % 43) / 100.0)::numeric(5,4)
             ELSE NULL END AS confidence,
        CASE
          WHEN s.st = 'QUEUED'  THEN NOW() - INTERVAL '25 seconds'
          WHEN s.st = 'RUNNING' THEN NOW() - INTERVAL '90 seconds'
          ELSE NOW() - make_interval(mins => ((s.rn - 1) * 30)::int)
                   - make_interval(secs => 40 + s.rn * 7)
        END AS started_at,
        CASE WHEN s.st IN ('SUCCEEDED', 'FAILED', 'TIMEOUT')
             THEN 18 + (s.rn * 23) % 403               -- 18~420ms
             ELSE NULL END AS duration_ms
    FROM (
        SELECT
            g.*,
            CASE g.rn WHEN 27 THEN 'FAILED'
                      WHEN 28 THEN 'TIMEOUT'
                      WHEN 29 THEN 'RUNNING'
                      WHEN 30 THEN 'QUEUED'
                      ELSE 'SUCCEEDED' END AS st,
            CASE WHEN g.rn IN (27, 28, 29, 30) THEN NULL ELSE 'RECOGNIZED' END AS conclusion,
            CASE WHEN g.rn IN (27, 28, 29, 30) THEN NULL ELSE g.vehicle_type END
                AS vehicle_type_out,
            -- 与 VehicleAlgorithmTaskConverter#maskPlate 同口径；过短脏数据一律 ***
            CASE WHEN length(g.plate_no) < 5 THEN '***'
                 ELSE substring(g.plate_no from 1 for 2) || '***'
                      || right(g.plate_no, 2) END AS plate_masked
        FROM (
            SELECT
                e.id            AS event_id,
                e.point_id,
                e.camera_id,
                e.camera_code,
                e.warehouse_id,
                e.region_id,
                e.org_id,
                e.plate_no,
                e.vehicle_type,
                row_number() OVER (ORDER BY e.recognized_at DESC) AS rn
            FROM ai_vehicle_event e
            WHERE e.created_by = 'seed'
        ) g
    ) s
) b;


-- =====================================================================
-- 步骤 5：回填事件行
--
-- 车辆事件只有 algorithm_task_id / algorithm_status / confidence 三列，
-- 没有 algorithm_result。
-- ⚠️ 绝不触碰 verify_status / processed_* / event_version。
-- =====================================================================
UPDATE ai_vehicle_event e
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    confidence        = t.confidence,
    updated_by        = 'seed',
    updated_at        = NOW()
FROM ai_vehicle_algorithm_task t
WHERE t.event_id = e.id
  AND t.task_no LIKE 'VCAT%';


-- =====================================================================
-- 步骤 6：自检
-- =====================================================================
-- 1) 任务总数：期望 30，覆盖 30 条记录
SELECT count(*) AS seeded_tasks,
       count(DISTINCT event_id) AS covered_events
FROM ai_vehicle_algorithm_task
WHERE task_no LIKE 'VCAT%';

-- 2) 状态分布：期望 SUCCEEDED 26 / FAILED 1 / TIMEOUT 1 / RUNNING 1 / QUEUED 1
SELECT execution_status, count(*) AS n
FROM ai_vehicle_algorithm_task
WHERE task_no LIKE 'VCAT%'
GROUP BY execution_status
ORDER BY n DESC, execution_status;

-- 3) 事件回填数：期望 = 任务总数
SELECT count(*) AS backfilled_events
FROM ai_vehicle_event e
JOIN ai_vehicle_algorithm_task t ON t.id = e.algorithm_task_id
WHERE t.task_no LIKE 'VCAT%';

-- 4) is_simulated 铁律核查：期望 0
SELECT count(*) AS not_simulated_rows
FROM ai_vehicle_algorithm_task
WHERE task_no LIKE 'VCAT%' AND is_simulated IS NOT TRUE;

-- 5) 受控引用核查：期望 0
SELECT count(*) AS bad_media_refs
FROM ai_vehicle_algorithm_task
WHERE task_no LIKE 'VCAT%'
  AND (media_refs ~ 'https?://' OR media_refs ~ '^/' OR media_refs ~ '\\\\');

-- 6) 车牌 PII 核查：任务 detections 中不得出现完整车牌原文，期望 0
SELECT count(*) AS leaked_plate_rows
FROM ai_vehicle_algorithm_task t
JOIN ai_vehicle_event e ON e.id = t.event_id
WHERE t.task_no LIKE 'VCAT%'
  AND t.detections IS NOT NULL
  AND t.detections LIKE '%' || e.plate_no || '%';

-- 7) 任务与事件同点同仓自洽：期望 0
SELECT count(*) AS mismatched_tasks
FROM ai_vehicle_algorithm_task t
JOIN ai_vehicle_event e ON e.id = t.event_id
WHERE t.task_no LIKE 'VCAT%'
  AND (t.point_id <> e.point_id OR t.warehouse_id <> e.warehouse_id);

-- 8) 置信度约束核查：期望 0
SELECT count(*) AS bad_confidence
FROM ai_vehicle_algorithm_task
WHERE task_no LIKE 'VCAT%'
  AND confidence IS NOT NULL
  AND (confidence < 0.55 OR confidence > 0.98);
