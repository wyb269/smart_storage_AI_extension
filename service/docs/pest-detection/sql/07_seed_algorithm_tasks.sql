-- =====================================================================
-- 粮面虫害智能识别功能 · 算法任务历史数据种子（07）
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 前置条件：
--   1. 05_create_pest_algorithm_task.sql 已成功执行（表 ai_pest_algorithm_task 存在）；
--   2. 04_seed_pest_demo_data.sql 已灌入 30 条 ai_pest_event（created_by='seed'）。
--
-- 本脚本为每条已有检测事件补 1 条历史算法任务（task_type = PEST_DETECT），
-- 并回写事件的 algorithm_task_id / algorithm_status / algorithm_result /
-- confidence 与结论列（conclusion_type / insect_species / density）。
--
-- 状态分布（30 条，按契约 §2 约 85/5/5/5）：
--   SUCCEEDED 25 / FAILED 2（i=7,21）/ TIMEOUT 2（i=14,28）/ RUNNING 1（i=30）。
--   RUNNING 给页面「重新识别」按钮留出可观察的进行中态。
--
-- 自洽约定（虫害功能特有，验收重点）：
--   1. detections JSON 给出【虫种与计数】：GRAIN_INSECT/DENSITY_HIGH 有
--      species + count（1kg 采样，count 即密度头数）；MOLD 为 mold_patch 计数；
--      OTHER 为空数组（无法判断不伪造检出）。
--   2. DENSITY_HIGH 必须 density > density_threshold：
--      count = GREATEST(density, threshold+2)，回写事件时同样从任务 JSON 取数。
--   3. is_simulated 一律 TRUE：全部是模拟结果，禁止写 FALSE。
--   4. media_refs / evidence_refs / normal_reference_ref 只有受控内部引用
--      （mock:// 前缀），不出现任何 http / 本地绝对路径。
--
-- ⚠️ 时间必须相对 now() 生成：任务起止时间从事件 occurred_at（本身相对
--    now()）推导，禁止写死日期，否则「最近识别」几天后腐烂成陈旧数据。
--
-- 幂等：先按 task_no 前缀 PSTASK + created_by='seed-task' 删除旧种子再插入，
--       可重复执行；不 DROP TABLE，不影响应用运行期产生的 MOCK-TASK-* 行。
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 连接显式指定数据库名 smart_storage_ai_extension；
--   3. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   4. 目标对象已逐项确认。
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
-- 本步只写数据（zhlc 已有 DML 权限），无新建表，不需要 GRANT。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
SELECT current_database();

-- ---------- 步骤 2：前置状态确认 ----------
-- 期望：task_table_exists=1，seed_event_rows=30。
-- 事件为 0 说明 04 未灌，先停下补灌，不要凭空造任务。
SELECT
  (SELECT count(*) FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'ai_pest_algorithm_task')       AS task_table_exists,
  (SELECT count(*) FROM ai_pest_event
    WHERE created_by = 'seed')                          AS seed_event_rows;


-- ---------- 步骤 3：清理旧种子（可重复执行的关键，不动应用任务行） ----------
DELETE FROM ai_pest_algorithm_task
WHERE task_no LIKE 'PSTASK%'
   OR created_by = 'seed-task';


-- ---------- 步骤 4：每条事件落 1 条历史任务 ----------
-- 结论类型与事件一致：i=(旧编号-2000)，(i-1)%4 → GI/DH/MOLD/OTHER 轮转，
-- 直接取事件自身列，不重复排布，避免与 04 的轮转逻辑漂移。
INSERT INTO ai_pest_algorithm_task (
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
    510000000000 + g.i,
    -- 编号：前缀 + 事件采样日期 + 序号，重跑按前缀清理
    'PSTASK' || to_char(e.occurred_at, 'YYYYMMDD') || lpad(g.i::text, 4, '0'),
    e.id, e.point_id, e.camera_id, e.camera_code,
    e.warehouse_id, e.region_id, e.org_id,

    'PEST_DETECT',                                        -- task_type（本功能语义名）
    'SINGLE_IMAGE',                                       -- 采样单帧
    -- 受控内部引用：与 04 种子的证据键完全同源，禁止 http/本地路径
    '["mock://pest/' || e.id || '/frame","mock://pest/' || e.id || '/normal"]',
    'mock://pest/' || e.id || '/normal',
    '[0,0,1920,1080]',

    g.execution_status,
    CASE g.execution_status
      WHEN 'SUCCEEDED' THEN CASE e.conclusion_type WHEN 'OTHER' THEN 'UNCERTAIN' ELSE 'ANOMALY' END
      ELSE NULL                                           -- 未成功的任务没有结论
    END,
    CASE WHEN g.execution_status = 'SUCCEEDED' THEN e.conclusion_type ELSE NULL END,
    CASE WHEN g.execution_status = 'SUCCEEDED'
         THEN g.task_confidence                           -- 0.55~0.98；i%7=3 与事件同步留空（验证「暂无」）
         ELSE NULL
    END,

    'mock-pest-detector',
    'mock-1.0.0',
    'mock-adapter-1.0.0',
    'mock-config-1',
    0.7500,
    'sha256:seedtask' || lpad(g.i::text, 4, '0'),

    -- 时间全部相对 now()：终态任务贴着事件采样时间；
    -- RUNNING 刻意 2 分钟前开始，页面能观察到「识别中」
    CASE WHEN g.execution_status = 'RUNNING'
         THEN NOW() - INTERVAL '2 minutes'
         ELSE e.occurred_at + INTERVAL '30 seconds'
    END,
    CASE WHEN g.execution_status = 'SUCCEEDED'
         THEN e.occurred_at + INTERVAL '30 seconds'
                  + make_interval(secs => (18 + (g.i * 53) % 403) / 1000.0)
         ELSE NULL            -- 失败/超时/进行中一律没有 finished_at（契约 §2）
    END,
    CASE WHEN g.execution_status = 'RUNNING' THEN NULL
         ELSE 18 + (g.i * 53) % 403          -- 18~420ms
    END,

    -- detections：虫种与计数自洽（1kg 采样，count 即密度头数）
    CASE
      WHEN g.execution_status <> 'SUCCEEDED' THEN NULL
      WHEN e.conclusion_type = 'OTHER'       THEN '[]'    -- 无法判断不伪造检出
      WHEN e.conclusion_type = 'MOLD'        THEN
        '[{"category":"mold_patch","count":' || d.detect_count ||
        ',"confidence":0.8800,"bbox":"[300,150,900,600]"}]'
      ELSE
        '[{"category":"grain_insect","species":"' || COALESCE(e.insect_species, '玉米象') ||
        '","count":' || d.detect_count ||
        ',"confidence":' || to_char(COALESCE(g.task_confidence, 0.9000), 'FM0.0000') ||
        ',"bbox":"[120,80,460,420]"}]'
    END,
    '[{"evidenceType":"PEST_FRAME","objectKey":"mock://pest/' || e.id || '/frame"}]',
    'mock://task/seed' || lpad(g.i::text, 4, '0') || '/raw',

    CASE g.execution_status WHEN 'FAILED'  THEN 'ALGORITHM_ERROR'
                            WHEN 'TIMEOUT' THEN 'ALGORITHM_TIMEOUT'
                            ELSE NULL END,
    CASE g.execution_status WHEN 'FAILED'  THEN '模拟算法执行失败（历史样例，非真实故障）'
                            WHEN 'TIMEOUT' THEN '模拟算法执行超时（历史样例，非真实故障）'
                            ELSE NULL END,
    CASE g.execution_status WHEN 'FAILED' THEN 2 ELSE 0 END,
    'seedtask-' || lpad(g.i::text, 4, '0'),

    TRUE,                                                 -- is_simulated：一律 TRUE，铁律 1
    'seed-task', NOW(), 'seed-task', NOW()
FROM (
    SELECT
        i,
        -- 状态分布：25 SUCCEEDED / 2 FAILED / 2 TIMEOUT / 1 RUNNING
        CASE
          WHEN i IN (7, 21)  THEN 'FAILED'
          WHEN i IN (14, 28) THEN 'TIMEOUT'
          WHEN i = 30        THEN 'RUNNING'
          ELSE 'SUCCEEDED'
        END AS execution_status,
        -- 置信度 0.55~0.98 平滑散布；与事件既有「置信度留空」样例（i%7=3）对齐
        CASE WHEN i % 7 = 3 THEN NULL
             ELSE round(0.55 + ((i * 137) % 44) / 100.0, 4)
        END AS task_confidence
    FROM generate_series(1, 30) AS i
) g
JOIN ai_pest_event e ON e.id = 2000 + g.i AND e.created_by = 'seed'
CROSS JOIN LATERAL (
    SELECT
        -- 计数自洽：DENSITY_HIGH 必须 > 阈值（取 max(现密度, 阈值+2)，现密度 7 即 7）；
        -- 其余（含密度为 0 的历史怪行）至少检出 1 个，杜绝「有虫情结论但 0 计数」
        CASE WHEN e.conclusion_type = 'DENSITY_HIGH'
             THEN GREATEST(e.density::int, (e.density_threshold::int) + 2)
             ELSE GREATEST(e.density::int, 1)
        END AS detect_count
) d;


-- ---------- 步骤 5：回写事件（任务行是事实来源，事件指回任务） ----------
-- 5.1 公共回写：任务指针 + 执行状态 + 置信度（与任务行逐列一致）
UPDATE ai_pest_event e
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    confidence        = t.confidence,
    updated_at        = NOW()
FROM ai_pest_algorithm_task t
WHERE t.event_id = e.id
  AND t.created_by = 'seed-task';

-- 5.2 成功任务额外回写结论列：算法结果 + 采样结论 + 虫种/密度（从任务 detections 取数，
--     保证「页面密度」与「算法计数」是同一个数字）。
--     ⚠️ 刻意不动 process_status / event_version：算法历史不干扰人工处理状态。
UPDATE ai_pest_event e
SET algorithm_result       = t.conclusion,
    conclusion_type        = t.conclusion_type,
    conclusion_type_name   = CASE t.conclusion_type
                               WHEN 'GRAIN_INSECT' THEN '粮面害虫'
                               WHEN 'DENSITY_HIGH' THEN '密度超标'
                               WHEN 'MOLD'         THEN '霉变'
                               WHEN 'OTHER'        THEN '其他或未知'
                             END,
    insect_species         = CASE WHEN t.conclusion_type IN ('GRAIN_INSECT', 'DENSITY_HIGH')
                                  THEN COALESCE(e.insect_species, '玉米象')
                                  ELSE e.insect_species
                             END,
    density                = CASE WHEN t.conclusion_type = 'OTHER' THEN e.density
                                  ELSE (t.detections::jsonb -> 0 ->> 'count')::numeric(5,2)
                             END,
    updated_at             = NOW()
FROM ai_pest_algorithm_task t
WHERE t.event_id = e.id
  AND t.created_by = 'seed-task'
  AND t.execution_status = 'SUCCEEDED';


-- ---------- 步骤 6：自检 ----------
-- 6.1 任务总数：期望 30
SELECT count(*) AS seed_task_rows
FROM ai_pest_algorithm_task
WHERE created_by = 'seed-task';

-- 6.2 状态分布：期望 SUCCEEDED 25 / FAILED 2 / TIMEOUT 2 / RUNNING 1
SELECT execution_status, count(*) AS n
FROM ai_pest_algorithm_task
WHERE created_by = 'seed-task'
GROUP BY execution_status
ORDER BY execution_status;

-- 6.3 事件回填数：期望 30（algorithm_task_id 全部指向任务行）
SELECT count(*) AS events_backfilled
FROM ai_pest_event e
JOIN ai_pest_algorithm_task t ON t.id = e.algorithm_task_id
WHERE t.created_by = 'seed-task';

-- 6.4 铁律自检：模拟标识必须全 TRUE，期望 0
SELECT count(*) AS not_simulated_rows
FROM ai_pest_algorithm_task
WHERE created_by = 'seed-task'
  AND is_simulated IS NOT TRUE;

-- 6.5 密度自洽：成功的 DENSITY_HIGH 任务必须 density > 阈值，期望 0 违规
SELECT count(*) AS density_high_violations
FROM ai_pest_event e
JOIN ai_pest_algorithm_task t ON t.id = e.algorithm_task_id
WHERE t.created_by = 'seed-task'
  AND t.execution_status = 'SUCCEEDED'
  AND e.conclusion_type = 'DENSITY_HIGH'
  AND e.density <= e.density_threshold;

-- 6.6 媒体引用卫生：不允许出现 http / 本地绝对路径，期望 0
SELECT count(*) AS bad_media_refs
FROM ai_pest_algorithm_task
WHERE created_by = 'seed-task'
  AND (media_refs LIKE '%http%' OR media_refs LIKE '/%' OR media_refs LIKE '%\%');

-- 6.7 虫种/计数自洽：成功且检出>0 的任务 detections 必须带 count，期望 0 违规
SELECT count(*) AS missing_species_count
FROM ai_pest_algorithm_task t
JOIN ai_pest_event e ON e.id = t.event_id
WHERE t.created_by = 'seed-task'
  AND t.execution_status = 'SUCCEEDED'
  AND e.conclusion_type IN ('GRAIN_INSECT', 'DENSITY_HIGH')
  AND (t.detections IS NULL OR t.detections NOT LIKE '%count%');
