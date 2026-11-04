-- =====================================================================
-- 人员行为监测功能 · 算法历史任务种子数据（契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §2）
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：
--   01_create_person_behavior_tables.sql、04_seed_person_behavior_demo_data.sql
--   与 05_create_person_algorithm_task.sql 均已执行。
--
-- 本脚本给「前 40 条」行为事件补历史算法任务（200 条只补 40 条，避免灌垃圾；
-- 按 event_no 尾号即灌入序号 0001~0040 取，恰为最近的 40 条事件）。
-- 本功能特有自洽要求：
--   - task_type 固定 PERSON_BEHAVIOR_DETECT；
--   - detections JSON 的目标框数量与事件 person_count 自洽
--     （CROWD 事件框数 = person_count，其余 1 框）；
--   - 回写事件 algorithm_task_id / algorithm_status / confidence / behavior_type，
--     绝不触碰 process_status 与 event_version（人工处理链路不受影响）。
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 确认这是【演示数据】，不得灌入生产环境。
-- 任一条件为假，立即停止。
--
-- ⚠️ 时间戳必须相对 NOW() 生成（与 04 脚本同一约定）：写死日期会让
--    「最近检测」在几天后变成陈旧数据，验收时看着像坏了。
--    任务开始时间锚定事件 occurred_at（04 灌入时即为相对值），
--    进行中（RUNNING/QUEUED）两条锚定 NOW()，验收时可当场观察到进行中态。
--
-- ⚠️ 本脚本只写数据，不建表：ai_person_algorithm_task 的 DML 权限
--    已在 05 脚本 GRANT 给 zhlc，无需新增授权。
--
-- 可重复执行：先按 task_no 前缀 'PAT' 删除本脚本产物再重建（不 DROP TABLE，
-- 运行期「重新识别」产出的 MOCK-TASK-* 行不受影响）。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
SELECT current_database();


-- ---------- 步骤 2：幂等清理（只删本脚本的产物） ----------
-- 前缀 PAT 为本功能算法任务种子专用；运行期重新识别的任务号是 MOCK-TASK-*，
-- 与 'PAT%' 无交集，不会被误删。
DELETE FROM ai_person_algorithm_task WHERE task_no LIKE 'PAT%';

-- 事件上指向已被删除任务的引用要一起清掉，避免 algorithm_task_id 悬挂
UPDATE ai_person_event
SET algorithm_task_id = NULL
WHERE algorithm_task_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM ai_person_algorithm_task t WHERE t.id = ai_person_event.algorithm_task_id
  );


-- ---------- 步骤 3：插入前 40 条任务的行号与派生值 ----------
-- 选取规则：created_by='seed' 的事件按 event_no 尾 4 位（灌入序号）升序取前 40。
-- 状态分布（40 条）：SUCCEEDED 34 / FAILED 2 / TIMEOUT 2 / RUNNING 1 / QUEUED 1，
-- 即约 85% / 5% / 5% / 5%；进行中两条放末尾号，验收时可观察。
WITH picked AS (
    SELECT e.id, e.event_no, e.point_id, e.camera_id, e.camera_code,
           e.warehouse_id, e.region_id, e.org_id,
           e.behavior_type, e.person_count, e.occurred_at,
           row_number() OVER (ORDER BY right(e.event_no, 4)::int ASC) AS rn
    FROM ai_person_event e
    WHERE e.created_by = 'seed'
      AND e.event_no LIKE 'PR%'
), first40 AS (
    SELECT * FROM picked WHERE rn <= 40
), decided AS (
    SELECT f.*,
        'PAT' || to_char(NOW(), 'YYYYMMDD') || lpad(f.rn::text, 4, '0') AS task_no,
        CASE
            WHEN f.rn = 39           THEN 'RUNNING'
            WHEN f.rn = 40           THEN 'QUEUED'
            WHEN f.rn IN (17, 37)    THEN 'FAILED'
            WHEN f.rn IN (9, 29)     THEN 'TIMEOUT'
            ELSE 'SUCCEEDED'
        END AS execution_status,
        -- 置信度 0.55~0.98，NUMERIC(5,4)，满足 ck_psn_confidence 检查
        CASE
            WHEN f.rn <= 38 AND f.rn NOT IN (9, 17, 29, 37)
                THEN ROUND((0.55 + ((f.rn * 37) % 44) * 0.01)::numeric, 4)
            ELSE NULL
        END AS confidence,
        (18 + (f.rn * 97) % 403)::bigint AS duration_ms
    FROM first40 f
)


-- =====================================================================
-- 步骤 4：写入 ai_person_algorithm_task（39 列全列出）
-- =====================================================================
INSERT INTO ai_person_algorithm_task (
    id, task_no, event_id, point_id, camera_id, camera_code,
    warehouse_id, region_id, org_id,
    task_type, media_type, media_refs, normal_reference_ref, roi,
    execution_status, conclusion, behavior_type, confidence,
    model_name, model_version, algorithm_version, config_version,
    confidence_threshold, media_digest,
    started_at, finished_at, inference_duration_ms,
    detections, evidence_refs, raw_result_ref,
    error_code, error_message, retry_count, trace_id,
    is_simulated, created_by, created_at, updated_by, updated_at
)
SELECT
    -- id 无序列，与雪花（19 位）区间错开的小常数段，重跑先删后插无冲突
    91000 + d.rn,
    d.task_no,
    d.id,
    d.point_id, d.camera_id, d.camera_code,
    d.warehouse_id, d.region_id, d.org_id,

    'PERSON_BEHAVIOR_DETECT',
    'SINGLE_IMAGE',
    -- 受控内部引用（mock://），禁止 http / 本地绝对路径
    jsonb_build_array('mock://person/' || d.id || '/behavior')::text,
    'mock://person/' || d.id || '/normal',
    NULL,

    d.execution_status,
    CASE WHEN d.execution_status = 'SUCCEEDED' THEN 'ANOMALY' ELSE NULL END,
    -- 行为类型取值同 ai_person_event.behavior_type（CROWD/NO_HELMET/FALL/INTRUSION/OTHER）
    CASE WHEN d.execution_status = 'SUCCEEDED' THEN d.behavior_type ELSE NULL END,
    d.confidence,

    'person-behavior-detector',
    'v1.3.0',
    'algo-2026.09.1',
    'cfg-person-v3',
    0.7500,
    'md5:mock-person-' || d.id,

    CASE
        WHEN d.execution_status = 'QUEUED' THEN NULL
        WHEN d.execution_status = 'RUNNING' THEN NOW() - INTERVAL '2 minutes'
        ELSE d.occurred_at + INTERVAL '2 seconds'
    END,
    -- 失败/超时/进行中没有 finished_at
    CASE WHEN d.execution_status = 'SUCCEEDED'
        THEN d.occurred_at + INTERVAL '2 seconds' + (d.duration_ms::text || ' milliseconds')::interval
        ELSE NULL
    END,
    CASE WHEN d.execution_status = 'SUCCEEDED' THEN d.duration_ms ELSE NULL END,

    -- detections：CROWD 框数 = 事件 person_count，其余 1 框；非成功任务为 NULL
    det.detections_json,
    CASE WHEN d.execution_status = 'SUCCEEDED' THEN jsonb_build_array(
        jsonb_build_object(
            'evidenceType', 'NORMAL_REFERENCE',
            'objectKey', 'mock://person/' || d.id || '/normal',
            'capturedAt', to_char(d.occurred_at, 'YYYY-MM-DD HH24:MI:SS')),
        jsonb_build_object(
            'evidenceType', 'BEHAVIOR_FRAME',
            'objectKey', 'mock://person/' || d.id || '/behavior',
            'capturedAt', to_char(d.occurred_at, 'YYYY-MM-DD HH24:MI:SS'))
    )::text ELSE NULL END,
    CASE WHEN d.execution_status = 'SUCCEEDED'
        THEN 'mock://task/' || d.task_no || '/raw' ELSE NULL END,

    CASE d.execution_status
        WHEN 'FAILED'  THEN 'MOCK_FAILURE'
        WHEN 'TIMEOUT' THEN 'MOCK_TIMEOUT'
        ELSE NULL
    END,
    CASE d.execution_status
        WHEN 'FAILED'  THEN '模拟算法执行失败'
        WHEN 'TIMEOUT' THEN '模拟算法执行超时'
        ELSE NULL
    END,
    0,
    'trace-pat-' || to_char(NOW(), 'YYYYMMDD') || '-' || lpad(d.rn::text, 3, '0'),

    -- 铁律：模拟数据 is_simulated 必须为 TRUE
    TRUE,
    'seed-07',
    NOW(),
    NULL,
    NULL
FROM decided d
LEFT JOIN LATERAL (
    SELECT (jsonb_agg(jsonb_build_object(
        'category', CASE WHEN d.behavior_type = 'NO_HELMET' AND g.k = 1
                        THEN 'no_helmet' ELSE 'person' END,
        'confidence', d.confidence,
        'bbox', format('[%s,%s,%s,%s]',
                       10 + (g.k * 55) % 400,
                       60 + (g.k % 4) * 45,
                       150 + (g.k * 55) % 400,
                       240 + (g.k % 4) * 45),
        'trackId', 'track-' || d.id::text || '-' || lpad(g.k::text, 3, '0')
    )))::text AS detections_json
    FROM generate_series(
        1,
        CASE WHEN d.execution_status = 'SUCCEEDED'
             THEN CASE WHEN d.behavior_type = 'CROWD'
                       THEN COALESCE(d.person_count, 1) ELSE 1 END
             ELSE 0 END
    ) AS g(k)
) det ON TRUE;


-- =====================================================================
-- 步骤 5：回写事件（algorithm_task_id / algorithm_status / confidence / behavior_type）
--   - algorithm_status 同步任务执行状态（含 RUNNING/QUEUED，供页面观察进行中态）；
--   - behavior_type 仅在任务给出行为类型时覆盖（COALESCE 保留原值）；
--   - 绝不触碰 process_status / event_version，人工处理链路不受影响。
-- =====================================================================
UPDATE ai_person_event e
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    algorithm_result  = t.conclusion,
    confidence        = t.confidence,
    behavior_type     = COALESCE(t.behavior_type, e.behavior_type),
    updated_by        = 'seed-07',
    updated_at        = NOW()
FROM ai_person_algorithm_task t
WHERE t.task_no LIKE 'PAT%'
  AND t.event_id = e.id;


-- =====================================================================
-- 步骤 6：自检查询
-- =====================================================================
-- 1) 任务总数：期望 40
SELECT count(*) AS seeded_task_rows
FROM ai_person_algorithm_task
WHERE task_no LIKE 'PAT%';

-- 2) 状态分布：期望 SUCCEEDED 34 / FAILED 2 / TIMEOUT 2 / RUNNING 1 / QUEUED 1
SELECT execution_status, count(*) AS n
FROM ai_person_algorithm_task
WHERE task_no LIKE 'PAT%'
GROUP BY execution_status
ORDER BY execution_status;

-- 3) 事件回填数：期望 40
SELECT count(*) AS backfilled_event_rows
FROM ai_person_event
WHERE algorithm_task_id IS NOT NULL;

-- 4) 自洽性：CROWD 成功任务的检测框数必须等于事件 person_count，期望 0
SELECT count(*) AS crowd_box_mismatched_rows
FROM ai_person_event e
JOIN ai_person_algorithm_task t ON t.id = e.algorithm_task_id
WHERE t.task_no LIKE 'PAT%'
  AND t.execution_status = 'SUCCEEDED'
  AND e.behavior_type = 'CROWD'
  AND jsonb_array_length(t.detections::jsonb) IS DISTINCT FROM e.person_count;

-- 5) 受控引用：media_refs / evidence_refs / raw_result_ref 不得出现 http 或本地绝对路径，期望 0
SELECT count(*) AS uncontrolled_ref_rows
FROM ai_person_algorithm_task
WHERE task_no LIKE 'PAT%'
  AND (media_refs ~* 'https?://' OR media_refs ~ '^/'
       OR evidence_refs ~* 'https?://' OR evidence_refs ~ '^/'
       OR raw_result_ref ~* 'https?://' OR raw_result_ref ~ '^/');

-- 6) 模拟标识：期望 40（铁律：全部为 TRUE）
SELECT count(*) AS simulated_rows
FROM ai_person_algorithm_task
WHERE task_no LIKE 'PAT%' AND is_simulated = TRUE;

-- 7) 时间新鲜度：失败/超时/进行中的任务不得有 finished_at，期望 0
SELECT count(*) AS illegal_finished_rows
FROM ai_person_algorithm_task
WHERE task_no LIKE 'PAT%'
  AND execution_status IN ('FAILED', 'TIMEOUT', 'RUNNING', 'QUEUED')
  AND finished_at IS NOT NULL;
