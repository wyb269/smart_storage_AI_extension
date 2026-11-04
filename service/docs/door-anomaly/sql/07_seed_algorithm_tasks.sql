-- =====================================================================
-- 仓门异动功能 · 算法任务历史数据补种（07）
--
-- 目的：契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §2
--       为库里现有的 16 条 ai_anomaly_event 各补 1 条算法任务，
--       让验收时「算法任务」表格与详情弹窗有真实内容，
--       并把事件的 algorithm_task_id / algorithm_status / confidence /
--       anomaly_type 回填到对应任务。
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 表结构依据：01_create_door_anomaly_tables.sql 的 ai_algorithm_task
--            （列名与 CHECK 均以该 DDL 为准，见文件末尾「列差异」说明）
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次 DML 执行动作的明确授权；
--   2. 连接显式指定数据库名 smart_storage_ai_extension；
--   3. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   4. 目标对象已逐项确认。
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
--
-- 权限：本步只写数据，不建表、不建约束/索引；zhlc 已有 ai_algorithm_task
--       的 DML 权限，无需 GRANT。
--
-- 可重复执行：先按 task_no 前缀 'DAAT-' 删除本脚本产物，再重新插入。
--            不 DROP、不 DELETE 其它来源（如运行时「重新识别」产生的任务）。
-- =====================================================================

SELECT current_database();


-- ---------- 1. 幂等清理：只删除本脚本补种的种子任务 ----------
-- 运行时「重新识别」写入的任务 task_no 由算法网关生成（MOCK-TASK-...），
-- 与本前缀不同，不会被误删。
DELETE FROM ai_algorithm_task WHERE task_no LIKE 'DAAT-%';


-- ---------- 2. 逐条事件补一条算法任务 ----------
-- 数据驱动：从现有 ai_anomaly_event 出发，因此适配库里真实的事件行数与取值。
-- 状态分布（16 行，尽力贴近契约 §2 的 85/5/5/5，且保留可观察的进行中态）：
--   rn 1..13  -> SUCCEEDED   （13/16 ≈ 81%）
--   rn 14     -> FAILED
--   rn 15     -> TIMEOUT
--   rn 16     -> QUEUED       （给「重新识别」留出可观察的进行中态）
-- 时间全部相对 now() 生成，避免写死日期导致「最近检测」变陈旧。
-- candidate_event_id = 事件 id，供详情弹窗的 listAlgorithmTasks 反查。

-- 预计算每行需要的所有派生值（状态、置信度、两个目标框 JSON、起止时间）。
-- 把复杂表达式集中在 CTE 里，外层只做拼装，避免 json_build_array(...,NULL)
-- 产出 [框, null] 这种「null 元素」污染前端解析出来的目标框数量。
WITH s AS (
    SELECT
        e.id,
        e.event_no,
        e.door_id,
        e.camera_id,
        e.warehouse_id,
        e.anomaly_type,
        row_number() OVER (ORDER BY e.id)                          AS rn,
        CASE
            WHEN row_number() OVER (ORDER BY e.id) <= 13 THEN 'SUCCEEDED'
            WHEN row_number() OVER (ORDER BY e.id) = 14  THEN 'FAILED'
            WHEN row_number() OVER (ORDER BY e.id) = 15  THEN 'TIMEOUT'
            ELSE 'QUEUED'
        END                                                        AS status,
        round((0.55 + (row_number() OVER (ORDER BY e.id))::numeric / 16.0 * 0.43)::numeric, 4) AS conf
    FROM ai_anomaly_event e
),
b AS (
    SELECT
        s.*,
        CASE
            WHEN s.status = 'SUCCEEDED'
                 THEN (CASE WHEN s.anomaly_type = 'OTHER' THEN 'NORMAL' ELSE 'ANOMALY' END)
            ELSE NULL
        END                                                        AS concl,
        -- 1 个仓门框；异常（非 OTHER）事件再补 1 个目标框
        json_build_object(
            'category', 'door_panel',
            'confidence', s.conf,
            'bbox', '[120,80,460,420]',
            'trackId', 'track-' || lpad(s.rn::text, 4, '0'))       AS box_panel,
        json_build_object(
            'category', s.anomaly_type,
            'confidence', s.conf,
            'bbox', '[210,150,360,405]',
            'trackId', 'track-tgt-' || lpad(s.rn::text, 4, '0'))   AS box_target,
        now() - make_interval(mins => (s.rn * 5)::int)            AS started,
        (18 + (s.rn * 29) % 403)                                   AS dur_ms
    FROM s
)
INSERT INTO ai_algorithm_task (
    id, task_no, candidate_event_id, door_id, camera_id, warehouse_id,
    task_type, media_type, media_refs, normal_reference_ref, roi,
    execution_status, conclusion, anomaly_type, confidence,
    model_name, model_version, algorithm_version, config_version, confidence_threshold,
    media_digest, started_at, finished_at, inference_duration_ms,
    detections, evidence_refs, raw_result_ref, error_code, error_message,
    retry_count, trace_id, is_simulated,
    created_by, created_at, updated_by, updated_at
)
SELECT
    -- 稳定、确定、不与运行时雪花 ID（量级极大）和历史 3001~3016 冲突的 BIGINT 主键
    8000000000 + b.id                                             AS id,
    'DAAT-' || to_char(now() AT TIME ZONE 'Asia/Shanghai', 'YYYYMMDD')
        || '-' || lpad(b.rn::text, 3, '0')                        AS task_no,
    b.id                                                          AS candidate_event_id,
    b.door_id, b.camera_id, b.warehouse_id,
    'DOOR_ANOMALY_DETECT'                                         AS task_type,
    'DUAL_IMAGE'                                                  AS media_type,
    -- 受控内部引用 JSON，仅 mock:// scheme，禁止 http/本地绝对路径
    CASE WHEN b.status IN ('SUCCEEDED','FAILED','QUEUED')
         THEN json_build_array(
                  'mock://media/' || b.door_id || '/normal',
                  'mock://media/' || b.door_id || '/abnormal')::text
         ELSE NULL END                                            AS media_refs,
    CASE WHEN b.status IN ('SUCCEEDED','QUEUED')
         THEN 'mock://media/' || b.door_id || '/normal' ELSE NULL END AS normal_reference_ref,
    CASE WHEN b.status = 'SUCCEEDED' THEN '[120,80,460,420]' ELSE NULL END AS roi,

    b.status                                                      AS execution_status,
    b.concl                                                       AS conclusion,
    CASE WHEN b.status = 'SUCCEEDED' THEN b.anomaly_type ELSE NULL END AS anomaly_type,
    -- 置信度仅在成功时有值；范围 0.55~0.98，落在 ck_at_confidence (0~1) 内
    CASE WHEN b.status = 'SUCCEEDED' THEN b.conf ELSE NULL END    AS confidence,

    'door-anomaly-v1'                                             AS model_name,
    '1.2.3'                                                       AS model_version,
    'algo-2.0.1'                                                  AS algorithm_version,
    'cfg-20261001'                                                AS config_version,
    0.75                                                          AS confidence_threshold,

    'sha256-' || b.event_no                                       AS media_digest,
    -- started_at 相对 now() 往前错开，全部落在最近窗口内，不写死日期
    b.started                                                     AS started_at,
    -- 失败/超时/排队无 finished_at
    CASE WHEN b.status = 'SUCCEEDED'
         THEN b.started + make_interval(secs => b.dur_ms / 1000.0)
         ELSE NULL END                                            AS finished_at,
    CASE WHEN b.status = 'SUCCEEDED' THEN b.dur_ms::bigint ELSE NULL END AS inference_duration_ms,

    -- 目标框数量与事件类型自洽：OTHER=1 框，其余异常=2 框；非成功无 detections
    CASE
        WHEN b.status <> 'SUCCEEDED' THEN NULL
        WHEN b.anomaly_type = 'OTHER'
             THEN json_build_array(b.box_panel)::text
        ELSE json_build_array(b.box_panel, b.box_target)::text
    END                                                           AS detections,
    CASE WHEN b.status IN ('SUCCEEDED','QUEUED')
         THEN json_build_array(
                  'mock://evidence/' || b.door_id || '/normal',
                  'mock://evidence/' || b.door_id || '/abnormal')::text
         ELSE NULL END                                            AS evidence_refs,
    CASE WHEN b.status = 'SUCCEEDED'
         THEN 'mock://task/DAAT-'
              || to_char(now() AT TIME ZONE 'Asia/Shanghai','YYYYMMDD')
              || '-' || lpad(b.rn::text, 3, '0') || '/raw'
         ELSE NULL END                                            AS raw_result_ref,
    CASE b.status
        WHEN 'FAILED'  THEN 'ALGO_INTERNAL_ERROR'
        WHEN 'TIMEOUT' THEN 'ALGO_TIMEOUT'
        ELSE NULL END                                             AS error_code,
    CASE b.status
        WHEN 'FAILED'  THEN '算法推理异常（种子历史数据）'
        WHEN 'TIMEOUT' THEN '算法推理超时（种子历史数据）'
        ELSE NULL END                                             AS error_message,

    CASE WHEN b.status IN ('FAILED','TIMEOUT') THEN 1 ELSE 0 END  AS retry_count,
    'TRACE-DAAT-' || lpad(b.rn::text, 3, '0')                     AS trace_id,
    -- 铁律 §0.1：所有算法结果都是模拟的，is_simulated 必须为 TRUE
    TRUE                                                          AS is_simulated,

    'seed-07', now(), 'seed-07', now()
FROM b;


-- ---------- 3. 回填事件：指向种子任务并同步算法维度 ----------
-- 只写算法相关列，绝不触碰 process_status（人工处理状态独立，第 23 节）。
-- anomaly_type 为空时保留事件原值（NOT NULL，不能被算法空值抹掉）。
-- algorithm_result：未成功产出结论时置空，不伪造为 NORMAL。
UPDATE ai_anomaly_event ae
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    confidence        = t.confidence,
    anomaly_type      = COALESCE(t.anomaly_type, ae.anomaly_type),
    algorithm_result  = CASE WHEN t.execution_status = 'SUCCEEDED'
                             THEN t.conclusion ELSE NULL END,
    updated_by        = 'seed-07',
    updated_at        = now()
FROM ai_algorithm_task t
WHERE t.candidate_event_id = ae.id
  AND t.task_no LIKE 'DAAT-%';


-- ---------- 4. 结尾自检 ----------
-- 4.1 种子任务总数（期望 = ai_anomaly_event 行数，当前 16）
SELECT count(*) AS seeded_task_count
FROM ai_algorithm_task
WHERE task_no LIKE 'DAAT-%';

-- 4.2 状态分布（期望 SUCCEEDED 13 / FAILED 1 / TIMEOUT 1 / QUEUED 1）
SELECT execution_status, count(*) AS n
FROM ai_algorithm_task
WHERE task_no LIKE 'DAAT-%'
GROUP BY execution_status
ORDER BY execution_status;

-- 4.3 事件回填数（期望 = 种子任务数；algorithm_task_id 已指向 DAAT 任务）
SELECT count(*) AS events_backfilled
FROM ai_anomaly_event ae
JOIN ai_algorithm_task t ON t.id = ae.algorithm_task_id
WHERE t.task_no LIKE 'DAAT-%';

-- 4.4 一致性校验：is_simulated 必须全部为 TRUE（期望 bad=0）
SELECT count(*) FILTER (WHERE is_simulated IS DISTINCT FROM TRUE) AS bad_simulated
FROM ai_algorithm_task
WHERE task_no LIKE 'DAAT-%';
