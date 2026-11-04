-- =====================================================================
-- 视频遮挡检测功能 · 算法任务历史数据灌入（07）
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：videoOcclusion   任务表：ai_occlusion_algorithm_task（05 脚本建表）
-- task_type：VIDEO_OCCLUSION_DETECT（本功能语义名，不与其它功能共用）
--
-- 内容：为库里现有的每条 seed 遮挡事件（当前 30 条，≤40，按契约全量补齐）
--       各补 1 条历史算法任务，并回填事件行的
--       algorithm_task_id / algorithm_status / algorithm_result / confidence / occlusion_type。
--       「重新识别」产生的任务（task_no = MOCK-TASK-…）不在本脚本管辖范围内，
--       DELETE 只按本脚本自己的前缀 OCAT 圈定，不会误删。
--
-- 状态分布（契约 §2，约 85/5/5/5）：
--   SUCCEEDED 26（86.7%）/ FAILED 1 / TIMEOUT 1 / RUNNING 1 / QUEUED 1
--
-- ⚠️ 时间必须相对 NOW() 生成，绝不写死日期。
-- ⚠️ 幂等：先按 task_no 前缀 DELETE 再 INSERT（id 固定 720000+rn）。
--
-- 授权：本步只写数据，不新建表；zhlc 的 DML 权限已由 05 脚本 GRANT。
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 05_create_occlusion_algorithm_task.sql 已执行（任务表存在）；
--   4. 04_seed_video_occlusion_demo_data.sql 已执行（事件行存在）。
-- 任一条件为假，立即停止。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
SELECT current_database();

-- ---------- 步骤 2：前置对象自检 ----------
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('ai_occlusion_event', 'ai_occlusion_algorithm_task')
ORDER BY table_name;

-- 事件规模确认：期望 30
SELECT count(*) AS seed_event_rows FROM ai_occlusion_event WHERE created_by = 'seed';


-- ---------- 步骤 3：删除本脚本管辖的旧数据（只按自己的前缀） ----------
DELETE FROM ai_occlusion_algorithm_task WHERE task_no LIKE 'OCAT%';


-- =====================================================================
-- 步骤 4：插入 30 条历史任务
-- =====================================================================
INSERT INTO ai_occlusion_algorithm_task (
    id, task_no, event_id, camera_config_id, camera_id, camera_code,
    warehouse_id, region_id, org_id,
    task_type, media_type, media_refs, normal_reference_ref, roi,
    execution_status, conclusion, occlusion_type, confidence,
    model_name, model_version, algorithm_version, config_version, confidence_threshold,
    media_digest, started_at, finished_at, inference_duration_ms,
    detections, evidence_refs, raw_result_ref,
    error_code, error_message, retry_count, trace_id,
    is_simulated, created_by, created_at, updated_by, updated_at
)
SELECT
    720000 + b.rn,
    'OCAT' || to_char(NOW(), 'YYYYMMDD') || lpad(b.rn::text, 4, '0'),
    b.event_id,
    b.camera_config_id,
    b.camera_id,
    b.camera_code,
    b.warehouse_id,
    b.region_id,
    b.org_id,

    'VIDEO_OCCLUSION_DETECT',
    'SINGLE_IMAGE',
    -- 受控内部引用：只允许 mock:// 形态，不出现 http/本地绝对路径
    '[{"ref":"mock://occlusion/' || b.event_id || '/occluded","type":"OCCLUSION_FRAME"}]',
    'mock://occlusion/' || b.event_id || '/normal',
    '[40,30,700,520]',

    b.st,
    b.conclusion,
    b.occlusion_type_out,
    b.confidence,

    'mock-occlusion-detector',
    'mock-1.0.0',
    'mock-adapter-1.0.0',
    'occlusion-config-v1',
    0.7500,
    'oc-digest-' || md5(b.event_id::text),

    b.started_at,
    -- 与契约一致：失败/超时/进行中都没有 finished_at，只有成功才有
    CASE WHEN b.st = 'SUCCEEDED'
         THEN b.started_at + make_interval(secs => b.duration_ms / 1000.0)
         ELSE NULL END,
    b.duration_ms,

    CASE
      WHEN b.st = 'SUCCEEDED' AND b.rn % 2 = 0 THEN
        '[{"category":"OCCLUSION","bbox":[60,40,620,480],"confidence":0.94},'
        || '{"category":"OCCLUSION","bbox":[120,220,560,460],"confidence":0.67}]'
      WHEN b.st = 'SUCCEEDED' THEN
        '[{"category":"OCCLUSION","bbox":[80,55,640,470],"confidence":0.92}]'
      WHEN b.st IN ('FAILED', 'TIMEOUT') THEN '[]'
      ELSE NULL
    END,
    CASE WHEN b.st = 'SUCCEEDED' THEN
      '[{"evidenceType":"NORMAL_REFERENCE","objectKey":"mock://occlusion/'
      || b.event_id || '/normal"},'
      || '{"evidenceType":"OCCLUSION_FRAME","objectKey":"mock://occlusion/'
      || b.event_id || '/occluded"}]'
    ELSE NULL END,
    CASE WHEN b.st = 'SUCCEEDED'
         THEN 'mock://task/OCAT-' || b.rn || '/raw'
         ELSE NULL END,

    CASE b.st WHEN 'FAILED'  THEN 'SEED_INFERENCE_ERROR'
              WHEN 'TIMEOUT' THEN 'SEED_TIMEOUT'
              ELSE NULL END,
    CASE b.st WHEN 'FAILED'  THEN '模拟推理失败：画面质量评估异常（历史演示数据）'
              WHEN 'TIMEOUT' THEN '模拟算法执行超时（历史演示数据）'
              ELSE NULL END,
    0,
    'oc-trace-' || to_char(NOW(), 'YYYYMMDD') || '-' || lpad(b.rn::text, 4, '0'),

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
             THEN (0.55 + ((s.rn * 11) % 43) / 100.0)::numeric(5,4)
             ELSE NULL END AS confidence,
        CASE
          WHEN s.st = 'QUEUED'  THEN NOW() - INTERVAL '25 seconds'
          WHEN s.st = 'RUNNING' THEN NOW() - INTERVAL '90 seconds'
          ELSE NOW() - make_interval(mins => ((s.rn - 1) * 30)::int)
                   - make_interval(secs => 40 + s.rn * 7)
        END AS started_at,
        CASE WHEN s.st IN ('SUCCEEDED', 'FAILED', 'TIMEOUT')
             THEN 18 + (s.rn * 13) % 403               -- 18~420ms
             ELSE NULL END AS duration_ms
    FROM (
        SELECT
            g.*,
            CASE g.rn WHEN 27 THEN 'FAILED'
                      WHEN 28 THEN 'TIMEOUT'
                      WHEN 29 THEN 'RUNNING'
                      WHEN 30 THEN 'QUEUED'
                      ELSE 'SUCCEEDED' END AS st,
            CASE WHEN g.rn IN (27, 28, 29, 30) THEN NULL ELSE 'ANOMALY' END AS conclusion,
            -- 遮挡类型只有出结论时才有；事件侧回填走 COALESCE，
            -- 不会把 ai_occlusion_event.occlusion_type（NOT NULL）覆盖成 NULL
            CASE WHEN g.rn IN (27, 28, 29, 30) THEN NULL ELSE g.occlusion_type END
                AS occlusion_type_out
        FROM (
            SELECT
                e.id              AS event_id,
                e.camera_config_id,
                e.camera_id,
                e.camera_code,
                e.warehouse_id,
                e.region_id,
                e.org_id,
                e.occlusion_type,
                row_number() OVER (ORDER BY e.occurred_at DESC) AS rn
            FROM ai_occlusion_event e
            WHERE e.created_by = 'seed'
        ) g
    ) s
) b;


-- =====================================================================
-- 步骤 5：回填事件行
--
-- 只写算法侧字段：algorithm_task_id / algorithm_status / algorithm_result /
-- confidence / occlusion_type。
-- ⚠️ 绝不触碰 process_status / processed_* / event_version。
-- =====================================================================
UPDATE ai_occlusion_event e
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    algorithm_result  = t.conclusion,
    confidence        = t.confidence,
    occlusion_type    = COALESCE(t.occlusion_type, e.occlusion_type),
    updated_by        = 'seed',
    updated_at        = NOW()
FROM ai_occlusion_algorithm_task t
WHERE t.event_id = e.id
  AND t.task_no LIKE 'OCAT%';


-- =====================================================================
-- 步骤 6：自检
-- =====================================================================
-- 1) 任务总数：期望 30，覆盖 30 条事件
SELECT count(*) AS seeded_tasks,
       count(DISTINCT event_id) AS covered_events
FROM ai_occlusion_algorithm_task
WHERE task_no LIKE 'OCAT%';

-- 2) 状态分布：期望 SUCCEEDED 26 / FAILED 1 / TIMEOUT 1 / RUNNING 1 / QUEUED 1
SELECT execution_status, count(*) AS n
FROM ai_occlusion_algorithm_task
WHERE task_no LIKE 'OCAT%'
GROUP BY execution_status
ORDER BY n DESC, execution_status;

-- 3) 事件回填数：期望 = 任务总数
SELECT count(*) AS backfilled_events
FROM ai_occlusion_event e
JOIN ai_occlusion_algorithm_task t ON t.id = e.algorithm_task_id
WHERE t.task_no LIKE 'OCAT%';

-- 4) is_simulated 铁律核查：期望 0
SELECT count(*) AS not_simulated_rows
FROM ai_occlusion_algorithm_task
WHERE task_no LIKE 'OCAT%' AND is_simulated IS NOT TRUE;

-- 5) 受控引用核查：期望 0
SELECT count(*) AS bad_media_refs
FROM ai_occlusion_algorithm_task
WHERE task_no LIKE 'OCAT%'
  AND (media_refs ~ 'https?://' OR media_refs ~ '^/' OR media_refs ~ '\\\\');

-- 6) 任务与事件同摄像头同仓自洽：期望 0
SELECT count(*) AS mismatched_tasks
FROM ai_occlusion_algorithm_task t
JOIN ai_occlusion_event e ON e.id = t.event_id
WHERE t.task_no LIKE 'OCAT%'
  AND (t.camera_config_id <> e.camera_config_id OR t.warehouse_id <> e.warehouse_id);

-- 7) 置信度约束核查：期望 0
SELECT count(*) AS bad_confidence
FROM ai_occlusion_algorithm_task
WHERE task_no LIKE 'OCAT%'
  AND confidence IS NOT NULL
  AND (confidence < 0.55 OR confidence > 0.98);
