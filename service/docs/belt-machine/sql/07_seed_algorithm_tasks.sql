-- =====================================================================
-- 皮带机下站人监测功能 · 算法历史任务补数脚本（07）
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：beltMachine
-- 数据对象：ai_belt_algorithm_task（05 脚本建表）+ ai_belt_event 回填
--
-- 目的：验收时「算法任务」不能是空的。库里现有 30 条 seed 事件、
--   任务表 0 行，本脚本给每条事件补 1 条历史算法任务并回填事件算法列。
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 确认 01 / 05 建表脚本与 04 事件种子已执行。
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
--
-- ==================== 未部署点位显式排除（铁律） ====================
-- 未部署 = ai_belt_monitor_point.camera_id IS NULL（皮带机登记了台账但
-- 没接摄像头）。这类点位**没有媒体输入**，给它造算法结论等于伪造取证，
-- 因此下方 seed_rows 的 JOIN 条件里显式过滤 p.camera_id IS NOT NULL，
-- 结尾自检第 4 项验证「未部署点位任务数 = 0」。
-- 与后端 rerunAlgorithm 对 CAMERA_NOT_BOUND 的拒绝口径一致。
--
-- ==================== 状态与结论分布（契约 §2） ====================
-- 30 条任务：26 SUCCEEDED（≈87%）/ 1 FAILED / 1 TIMEOUT /
-- 1 QUEUED + 1 RUNNING（进行中态共 2 条，给「重新识别」留观察样本）。
-- 特判行 i=3/10/17/24 与 04 种子里置信度为空的 4 条事件重合：
--   这些任务没有 conclusion/confidence/detections，事件回填后
--   confidence 仍为 NULL —— 保留页面「暂无」演示行，而不是抹平它。
-- 进行中的任务没有置信度是诚实形态，不许编一个 0.55~0.98 填上；
-- 失败/超时按契约不出 finished_at。
--
-- ==================== 自洽约束 ====================
-- 1. detections JSON 的目标框数量 = 事件 person_count；
--    person_count 为 NULL 的行 detections 也为 NULL（人数未给出，不伪造框数）。
-- 2. task.conclusion_type 取事件同值（STANDING_UNDER/CROSSING/LOITERING/OTHER），
--    仅 SUCCEEDED 行有；事件 conclusion_type 本身 NOT NULL，回填不改它。
-- 3. 全部 is_simulated = TRUE。本表所有行都是模拟产物，不许写 FALSE。
-- 4. media_refs / normal_reference_ref / evidence_refs / raw_result_ref
--    只出现 mock:// 受控引用，无 http、无本地绝对路径（结尾自检第 6 项）。
--
-- ==================== 时间基准 ====================
-- 全部相对 now() / 事件 occurred_at 生成，不写死日期。
-- SUCCEEDED 行 started_at ≈ 事件发生前 1 分钟（任务先于事件落库），
-- QUEUED 行创建时间刻意放在最近 3 分钟内，验收时「进行中」看起来是活的。
--
-- ==================== 可重复执行 ====================
-- 先按 task_no 前缀 'BAT' DELETE 再 INSERT，最后重新回填事件；
-- 运行期「重新识别」产生的任务（task_no 形如 MOCK-TASK-*）不在删除范围，
-- 不受本脚本影响。不 DROP 任何表。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
SELECT current_database();

-- ---------- 步骤 2：前置状态确认 ----------
-- 预期：事件 30（created_by='seed'）、任务表本次灌入前为 0 或旧 BAT 行
SELECT
    (SELECT count(*) FROM ai_belt_event WHERE created_by = 'seed')  AS seed_event_rows,
    (SELECT count(*) FROM ai_belt_algorithm_task WHERE task_no LIKE 'BAT%') AS old_bat_rows;


-- ---------- 步骤 3：清理旧的补数行（按前缀，可重复执行） ----------
DELETE FROM ai_belt_algorithm_task WHERE task_no LIKE 'BAT%';

-- 事件侧指针一并复位，避免指向已删除的任务
UPDATE ai_belt_event
SET algorithm_task_id = NULL
WHERE created_by = 'seed'
  AND algorithm_task_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM ai_belt_algorithm_task t WHERE t.id = ai_belt_event.algorithm_task_id
  );


-- ---------- 步骤 4：写入 30 条历史算法任务 ----------
-- id 固定为 6000000 + event_id（事件 id 为 2001~2030，落在
-- 6002001~6002030），与运行期雪花 id 不会相撞；重跑安全（步骤 3 已删）。
WITH seed_rows AS (
    SELECT
        ev.id                                     AS event_id,
        ev.event_no,
        ev.point_id,
        p.camera_id                               AS point_camera_id,
        ev.camera_code,
        ev.warehouse_id,
        ev.region_id,
        ev.org_id,
        ev.occurred_at,
        ev.conclusion_type,
        ev.person_count,
        /* 事件序号 i：04 种子把 i 编在 event_no 末 4 位（BC+日期+4位序号），
           不依赖 id 区间算术，04 重灌日期后依然拿得到正确序号 */
        right(ev.event_no, 4)::int                AS i,
        CASE right(ev.event_no, 4)::int
            WHEN 3  THEN 'FAILED'
            WHEN 10 THEN 'TIMEOUT'
            WHEN 17 THEN 'QUEUED'
            WHEN 24 THEN 'RUNNING'
            ELSE 'SUCCEEDED'
        END                                       AS execution_status
    FROM ai_belt_event ev
    JOIN ai_belt_monitor_point p ON p.id = ev.point_id
    WHERE ev.created_by = 'seed'
      -- ★ 未部署点位排除：没有摄像头就没有媒体输入，不得凭空出结论
      AND p.camera_id IS NOT NULL
),
calc AS (
    SELECT
        r.*,
        (r.execution_status = 'SUCCEEDED')                     AS is_ok,
        -- 结论与结论类型只有成功行才有；失败/进行中一律 NULL，不伪造
        CASE WHEN r.execution_status = 'SUCCEEDED' THEN 'ANOMALY' END            AS conclusion,
        -- 别名带 task_ 前缀：r.* 里已有事件自身的 conclusion_type，避免重名歧义
        CASE WHEN r.execution_status = 'SUCCEEDED' THEN r.conclusion_type END    AS task_conclusion_type,
        -- 置信度 0.55~0.98，(i*17)%44 保证 30 行取值分散且全落区间内
        CASE WHEN r.execution_status = 'SUCCEEDED'
             THEN ROUND((0.55 + ((r.i * 17) % 44) * 0.01)::numeric, 4) END       AS confidence,
        -- 耗时 18~420ms 区间；(i*13)%403 + 18
        CASE WHEN r.execution_status = 'SUCCEEDED'
             THEN (18 + (r.i * 13) % 403)::bigint END                            AS inference_duration_ms,
        -- QUEUED 没有开始时间；RUNNING 已开始未完成；
        -- 终态行开始时间取事件发生前 1 分钟（相对 occurred_at，不写死日期）
        CASE
            WHEN r.execution_status = 'QUEUED'   THEN NULL
            WHEN r.execution_status = 'RUNNING'  THEN NOW() - INTERVAL '90 minutes'
            ELSE r.occurred_at - INTERVAL '1 minutes'
        END                                                                        AS started_at,
        CASE WHEN r.execution_status = 'SUCCEEDED'
             THEN r.occurred_at - INTERVAL '1 minutes'
                  + make_interval(secs => (18 + (r.i * 13) % 403) / 1000.0)
        END                                                                        AS finished_at,
        CASE r.execution_status
            WHEN 'FAILED'  THEN 'MOCK_FAILURE'
            WHEN 'TIMEOUT' THEN 'MOCK_TIMEOUT'
        END                                                                        AS error_code,
        CASE r.execution_status
            WHEN 'FAILED'  THEN '模拟算法执行失败'
            WHEN 'TIMEOUT' THEN '模拟算法执行超时'
        END                                                                        AS error_message,
        CASE WHEN r.execution_status = 'QUEUED'
             THEN NOW() - INTERVAL '3 minutes'
             ELSE r.occurred_at - INTERVAL '1 minutes'
        END                                                                        AS created_at
    FROM seed_rows r
)
INSERT INTO ai_belt_algorithm_task (
    id, task_no, event_id, point_id, camera_id, camera_code,
    warehouse_id, region_id, org_id,
    task_type, media_type, media_refs, normal_reference_ref, roi,
    execution_status, conclusion, conclusion_type, confidence,
    model_name, model_version, algorithm_version, config_version,
    confidence_threshold, media_digest,
    started_at, finished_at, inference_duration_ms,
    detections, evidence_refs, raw_result_ref,
    error_code, error_message, retry_count, trace_id,
    is_simulated, created_by, created_at, updated_by, updated_at
)
SELECT
    6000000 + c.event_id,
    -- task_no：前缀 + 事件发生日期 + 事件序号，唯一（uk_bat_task_no）
    'BAT' || to_char(c.occurred_at, 'YYYYMMDD') || lpad(c.i::text, 4, '0'),
    c.event_id,
    c.point_id,
    c.point_camera_id,
    c.camera_code,
    c.warehouse_id,
    c.region_id,
    c.org_id,
    'BELT_PERSON_DETECT',
    'SINGLE_IMAGE',
    -- 受控内部引用，与 04 种子的证据键同源；禁止 http / 本地路径
    json_build_array('mock://belt/' || c.event_id || '/anomaly')::text,
    'mock://belt/' || c.event_id || '/normal',
    '[0.10,0.15,0.90,0.85]',
    c.execution_status,
    c.conclusion,
    c.task_conclusion_type,
    c.confidence,
    -- 模型标识与 Mock 适配器一致：这批历史任务本来就是模拟产物
    'mock-belt-person-detector',
    'mock-1.0.0',
    'mock-adapter-1.0.0',
    'belt-cfg-1.0.0',
    0.7500,
    'sha256-' || substr(md5(c.event_no), 1, 16),
    c.started_at,
    c.finished_at,
    c.inference_duration_ms,
    -- detections：目标框数量 = person_count，自洽；
    -- person_count 为 NULL 的行不出框，与「人数未给出」语义一致
    CASE WHEN c.is_ok AND c.person_count IS NOT NULL AND c.person_count > 0
         THEN (
             SELECT json_agg(json_build_object(
                 'category',  'person_in_belt_zone',
                 'confidence', c.confidence,
                 'bbox',      format('[%s,80,%s,420]', 120 + 60 * n, 460 + 60 * n),
                 'trackId',   'trk-seed-' || c.event_id || '-' || n
             ))
             FROM generate_series(1, c.person_count) AS n
         )::text
    END,
    CASE WHEN c.is_ok
         THEN json_build_array(json_build_object(
             'evidenceType', 'ANOMALY_FRAME',
             'objectKey',    'mock://belt/' || c.event_id || '/anomaly',
             'capturedAt',   to_char(c.occurred_at, 'YYYY-MM-DD HH24:MI:SS')
         ))::text
    END,
    'mock://task/belt-' || c.event_id || '/raw',
    c.error_code,
    c.error_message,
    0,
    'seed-trace-' || substr(md5(c.event_id::text || c.event_no), 1, 16),
    TRUE,
    'seed',
    c.created_at,
    'seed',
    c.created_at
FROM calc c
ON CONFLICT (id) DO NOTHING;


-- ---------- 步骤 5：回填事件算法列 ----------
-- 只写 algorithm_task_id / algorithm_status / algorithm_result / confidence。
-- 事件 conclusion_type 是 NOT NULL 的事件自身类型，不改写；
-- process_* 与 event_version 一律不触碰（算法取证 ≠ 人工处理）。
-- 特判行（FAILED/TIMEOUT/QUEUED/RUNNING）回填后 confidence 变回 NULL：
-- 与任务自身无结论的形态一致，正好保住 04 种子的 4 条「暂无」演示行。
UPDATE ai_belt_event e
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    algorithm_result  = t.conclusion,
    confidence        = t.confidence
FROM ai_belt_algorithm_task t
WHERE t.event_id = e.id
  AND t.task_no LIKE 'BAT%'
  AND t.created_by = 'seed'
  AND e.created_by = 'seed';


-- =====================================================================
-- 步骤 6：自检（执行后逐项目视结果）
-- =====================================================================

-- 1) 任务总数：期望 30（= created_by='seed' 且有摄像头的事件数）
SELECT
    (SELECT count(*) FROM ai_belt_algorithm_task WHERE task_no LIKE 'BAT%') AS bat_task_rows,
    (SELECT count(*) FROM ai_belt_algorithm_task WHERE is_simulated = FALSE AND task_no LIKE 'BAT%') AS not_simulated_rows;

-- 2) 状态分布：期望 SUCCEEDED 26 / FAILED 1 / QUEUED 1 / RUNNING 1 / TIMEOUT 1
SELECT execution_status, count(*) AS n
FROM ai_belt_algorithm_task
WHERE task_no LIKE 'BAT%'
GROUP BY execution_status
ORDER BY execution_status;

-- 3) 事件回填数：期望 30，且与任务状态完全一致（mismatch 期望 0）
SELECT count(*) AS events_backfilled
FROM ai_belt_event
WHERE created_by = 'seed' AND algorithm_task_id IS NOT NULL;

SELECT count(*) AS event_task_status_mismatch
FROM ai_belt_event e
JOIN ai_belt_algorithm_task t ON t.id = e.algorithm_task_id
WHERE e.created_by = 'seed'
  AND (e.algorithm_status IS DISTINCT FROM t.execution_status
       OR e.algorithm_result IS DISTINCT FROM t.conclusion
       OR e.confidence IS DISTINCT FROM t.confidence);

-- 4) 未部署点位不得有任何算法任务：期望 0 行
--    （没有媒体输入的点位出结论 = 伪造，与后端 CAMERA_NOT_BOUND 拒绝同口径）
SELECT count(*) AS tasks_on_undeployed_points
FROM ai_belt_algorithm_task t
JOIN ai_belt_monitor_point p ON p.id = t.point_id
WHERE p.camera_id IS NULL;

-- 5) detections 框数与 person_count 自洽：期望 0 行不一致
--    SUCCEEDED 且给出人数的行：框数必须等于人数；未给人数的行不得有框
SELECT count(*) AS detections_mismatch_rows
FROM ai_belt_algorithm_task t
JOIN ai_belt_event e ON e.id = t.event_id
WHERE t.task_no LIKE 'BAT%'
  AND (
        (t.execution_status = 'SUCCEEDED' AND e.person_count IS NOT NULL
         AND (t.detections IS NULL
              OR jsonb_array_length(t.detections::jsonb) <> e.person_count))
     OR (t.detections IS NOT NULL
         AND (e.person_count IS NULL OR e.person_count <> jsonb_array_length(t.detections::jsonb)))
  );

-- 6) 受控引用检查：media_refs / normal_reference_ref / raw_result_ref /
--    evidence_refs / detections 不得出现 http 或本地绝对路径，期望 0 行
SELECT count(*) AS forbidden_ref_rows
FROM ai_belt_algorithm_task
WHERE task_no LIKE 'BAT%'
  AND (   media_refs            ~* 'http://|https://|^/'
       OR normal_reference_ref  ~* 'http://|https://|^/'
       OR raw_result_ref        ~* 'http://|https://|^/'
       OR evidence_refs         ~* 'http://|https://'
       OR detections            ~* 'http://|https://');

-- 7) 置信度区间：SUCCEEDED 行必须落在 0.55~0.98，非 SUCCEEDED 行必须为空；期望 0
SELECT count(*) AS confidence_range_violation
FROM ai_belt_algorithm_task
WHERE task_no LIKE 'BAT%'
  AND (   (execution_status  = 'SUCCEEDED'
           AND (confidence IS NULL OR confidence < 0.55 OR confidence > 0.98))
       OR (execution_status <> 'SUCCEEDED' AND confidence IS NOT NULL));

-- 8) 失败/超时不得有 finished_at；进行中不得有 finished_at：期望 0
SELECT count(*) AS bad_finished_at_rows
FROM ai_belt_algorithm_task
WHERE task_no LIKE 'BAT%'
  AND execution_status IN ('FAILED', 'TIMEOUT', 'QUEUED', 'RUNNING')
  AND finished_at IS NOT NULL;

-- 9) 时间新鲜度：最近一条 SUCCEEDED 任务必须落在 7 天内（相对 now 生成的意义所在）
SELECT to_char(max(finished_at), 'YYYY-MM-DD HH24:MI:SS') AS latest_finished_at,
       max(finished_at) > NOW() - INTERVAL '7 days'       AS fresh
FROM ai_belt_algorithm_task
WHERE task_no LIKE 'BAT%';

-- 10) 事件「暂无置信度」演示行保留情况：期望 4（特判行 i=3/10/17/24）
SELECT count(*) AS null_confidence_event_rows
FROM ai_belt_event
WHERE created_by = 'seed' AND confidence IS NULL;
