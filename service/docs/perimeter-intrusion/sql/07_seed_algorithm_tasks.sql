-- =====================================================================
-- 周界入侵识别功能 · 算法任务历史数据灌入（07）
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：perimeterIntrusion   任务表：ai_perimeter_algorithm_task（05 脚本建表）
-- task_type：PERIMETER_INTRUSION_DETECT（本功能语义名，不与其它功能共用）
--
-- 内容：为库里现有的每条 seed 周界事件（当前 30 条，≤40，按契约全量补齐）
--       各补 1 条历史算法任务，并回填事件行的
--       algorithm_task_id / algorithm_status / algorithm_result / confidence / intrusion_type。
--       「重新识别」产生的任务（task_no = MOCK-TASK-…）不在本脚本管辖范围内，
--       DELETE 只按本脚本自己的前缀 PDAT 圈定，不会误删。
--
-- 状态分布（契约 §2，约 85/5/5/5）：
--   SUCCEEDED 26（86.7%）/ FAILED 1 / TIMEOUT 1 / RUNNING 1 / QUEUED 1
--   后两类给「重新识别」按钮留出可观察的进行中态。
--
-- ⚠️ 时间必须相对 NOW() 生成，绝不写死日期：写死会让「最近检测」在几天后
--    变成陈旧数据，验收时看着像坏了。
--
-- ⚠️ 幂等：先按 task_no 前缀 DELETE 再 INSERT（id 固定 700000+rn，
--    与应用雪花 id 区间天然不冲突）；本脚本不 DROP 任何东西。
--
-- 授权：本步只写数据，不新建表；zhlc 对 ai_perimeter_algorithm_task 的
--       DML 权限已由 05 脚本 GRANT，无需重复授权。
--
-- ======================== 执行前强制门禁 ========================
--   1. 已获得用户对本次写入动作的明确授权；
--   2. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   3. 05_create_perimeter_algorithm_task.sql 已执行（任务表存在）；
--   4. 04_seed_perimeter_demo_data.sql 已执行（事件行存在）。
-- 任一条件为假，立即停止。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
SELECT current_database();

-- ---------- 步骤 2：前置对象自检 ----------
-- 预期各返回 1 行；为 0 行说明 01/04/05 没跑过，必须停止。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('ai_perimeter_event', 'ai_perimeter_algorithm_task')
ORDER BY table_name;

-- 事件规模确认：期望 30（若多于 40 需按契约收缩灌入范围，不要整表灌垃圾）
SELECT count(*) AS seed_event_rows FROM ai_perimeter_event WHERE created_by = 'seed';


-- ---------- 步骤 3：删除本脚本管辖的旧数据（只按自己的前缀） ----------
DELETE FROM ai_perimeter_algorithm_task WHERE task_no LIKE 'PDAT%';


-- =====================================================================
-- 步骤 4：插入 30 条历史任务
--
-- rn 按发生时间倒序编号（rn=1 最新）。异常态刻意压在 rn 27~30
-- （即最老的 4 条事件）上，新事件的详情页保持「成功+回填完整」的默认观感。
-- =====================================================================
INSERT INTO ai_perimeter_algorithm_task (
    id, task_no, event_id, point_id, camera_config_id, camera_id, camera_code,
    warehouse_id, region_id, org_id,
    task_type, media_type, media_refs, normal_reference_ref, roi,
    execution_status, conclusion, intrusion_type, confidence,
    model_name, model_version, algorithm_version, config_version, confidence_threshold,
    media_digest, started_at, finished_at, inference_duration_ms,
    detections, evidence_refs, raw_result_ref,
    error_code, error_message, retry_count, trace_id,
    is_simulated, created_by, created_at, updated_by, updated_at
)
SELECT
    -- id 固定 700000+rn，重复执行 DELETE 后按同值重插，天然幂等；
    -- 应用运行期写入的任务用雪花 id，两个区间不会相撞
    700000 + b.rn,
    'PDAT' || to_char(NOW(), 'YYYYMMDD') || lpad(b.rn::text, 4, '0'),
    b.event_id,
    b.point_id,
    b.camera_config_id,
    b.camera_id,
    b.camera_code,
    b.warehouse_id,
    b.region_id,
    b.org_id,

    'PERIMETER_INTRUSION_DETECT',
    'SINGLE_IMAGE',
    -- 受控内部引用：只允许 mock:// 形态，不出现 http/本地绝对路径（契约 §2、29.3）
    '[{"ref":"mock://perimeter/' || b.event_id || '/intrusion","type":"INTRUSION_FRAME"}]',
    'mock://perimeter/' || b.event_id || '/normal',
    '[120,80,460,420]',

    b.st,
    b.conclusion,
    b.intrusion_type_out,
    b.confidence,

    'mock-perimeter-intrusion-detector',
    'mock-1.0.0',
    'mock-adapter-1.0.0',
    'mock-config-v1',
    0.7500,
    'pd-digest-' || md5(b.event_id::text),

    b.started_at,
    -- 与契约一致：失败/超时/进行中都没有 finished_at，只有成功才有
    CASE WHEN b.st = 'SUCCEEDED'
         THEN b.started_at + make_interval(secs => b.duration_ms / 1000.0)
         ELSE NULL END,
    b.duration_ms,

    -- 检测框数量与周界事件自洽：成功任务按奇偶给 1~2 个人形框，
    -- 失败/超时给空数组，排队/进行中还没产出
    CASE
      WHEN b.st = 'SUCCEEDED' AND b.rn % 2 = 0 THEN
        '[{"category":"PERSON","bbox":[120,80,260,420],"confidence":0.93},'
        || '{"category":"PERSON","bbox":[520,110,650,430],"confidence":0.71}]'
      WHEN b.st = 'SUCCEEDED' THEN
        '[{"category":"PERSON","bbox":[140,95,305,398],"confidence":0.91}]'
      WHEN b.st IN ('FAILED', 'TIMEOUT') THEN '[]'
      ELSE NULL
    END,
    CASE WHEN b.st = 'SUCCEEDED' THEN
      '[{"evidenceType":"NORMAL_REFERENCE","objectKey":"mock://perimeter/'
      || b.event_id || '/normal"},'
      || '{"evidenceType":"INTRUSION_FRAME","objectKey":"mock://perimeter/'
      || b.event_id || '/intrusion"}]'
    ELSE NULL END,
    CASE WHEN b.st = 'SUCCEEDED'
         THEN 'mock://task/PDAT-' || b.rn || '/raw'
         ELSE NULL END,

    CASE b.st WHEN 'FAILED'  THEN 'SEED_INFERENCE_ERROR'
              WHEN 'TIMEOUT' THEN 'SEED_TIMEOUT'
              ELSE NULL END,
    CASE b.st WHEN 'FAILED'  THEN '模拟推理失败：目标特征提取异常（历史演示数据）'
              WHEN 'TIMEOUT' THEN '模拟算法执行超时（历史演示数据）'
              ELSE NULL END,
    0,
    'pd-trace-' || to_char(NOW(), 'YYYYMMDD') || '-' || lpad(b.rn::text, 4, '0'),

    -- 铁律 1：全部模拟结果，is_simulated 必须为 TRUE
    TRUE,
    'seed',
    NOW(),
    'seed',
    NOW()
FROM (
    SELECT
        s.*,
        -- 置信度 0.55~0.97，NUMERIC(5,4)，满足 ck_pmr_confidence（0~1）。
        -- 失败/超时/进行中还没产出置信度，留 NULL
        CASE WHEN s.st = 'SUCCEEDED'
             THEN (0.55 + ((s.rn * 17) % 43) / 100.0)::numeric(5,4)
             ELSE NULL END AS confidence,
        -- 时间相对 NOW()：成功/失败/超时的任务与对应事件同步（每 30 分钟一条），
        -- RUNNING/QUEUED 压在「刚刚」，验收时进行中态永远新鲜
        CASE
          WHEN s.st = 'QUEUED'  THEN NOW() - INTERVAL '25 seconds'
          WHEN s.st = 'RUNNING' THEN NOW() - INTERVAL '90 seconds'
          ELSE NOW() - make_interval(mins => ((s.rn - 1) * 30)::int)
                   - make_interval(secs => 40 + s.rn * 7)
        END AS started_at,
        CASE WHEN s.st IN ('SUCCEEDED', 'FAILED', 'TIMEOUT')
             THEN 18 + (s.rn * 31) % 403               -- 18~420ms
             ELSE NULL END AS duration_ms
    FROM (
        SELECT
            g.*,
            -- 状态分布：26 SUCCEEDED / 1 FAILED / 1 TIMEOUT / 1 RUNNING / 1 QUEUED
            CASE g.rn WHEN 27 THEN 'FAILED'
                      WHEN 28 THEN 'TIMEOUT'
                      WHEN 29 THEN 'RUNNING'
                      WHEN 30 THEN 'QUEUED'
                      ELSE 'SUCCEEDED' END AS st,
            CASE WHEN g.rn IN (27, 28, 29, 30) THEN NULL ELSE 'ANOMALY' END AS conclusion,
            -- 入侵类型只有出结论时才有；事件侧回填走 COALESCE，
            -- 不会把 ai_perimeter_event.intrusion_type（NOT NULL）覆盖成 NULL
            CASE WHEN g.rn IN (27, 28, 29, 30) THEN NULL ELSE g.intrusion_type END
                AS intrusion_type_out
        FROM (
            SELECT
                e.id              AS event_id,
                e.point_id,
                e.camera_config_id,
                e.camera_id,
                e.camera_code,
                e.warehouse_id,
                e.region_id,
                e.org_id,
                e.intrusion_type,
                row_number() OVER (ORDER BY e.occurred_at DESC) AS rn
            FROM ai_perimeter_event e
            -- 只认演示种子事件，与应用运行期写入的行划清界限
            WHERE e.created_by = 'seed'
        ) g
    ) s
) b;


-- =====================================================================
-- 步骤 5：回填事件行
--
-- 只写算法侧字段：algorithm_task_id / algorithm_status / algorithm_result /
-- confidence / intrusion_type。
-- ⚠️ 绝不触碰 process_status / processed_* —— 算法重跑不产生人工结论；
-- 也不递增 event_version，避免乐观锁被算法路径无谓搅动。
-- confidence / algorithm_result 直接覆盖：任务没产出时事件同步显示「暂无」，
-- 与服务层运行期回写口径一致；intrusion_type 是 NOT NULL，
-- 只有任务给出新结论才改写（COALESCE 兜底）。
-- =====================================================================
UPDATE ai_perimeter_event e
SET algorithm_task_id = t.id,
    algorithm_status  = t.execution_status,
    algorithm_result  = t.conclusion,
    confidence        = t.confidence,
    intrusion_type    = COALESCE(t.intrusion_type, e.intrusion_type),
    updated_by        = 'seed',
    updated_at        = NOW()
FROM ai_perimeter_algorithm_task t
WHERE t.event_id = e.id
  AND t.task_no LIKE 'PDAT%';


-- =====================================================================
-- 步骤 6：自检
-- =====================================================================
-- 1) 任务总数：期望 = seed 事件数（当前 30），每条事件恰好 1 条任务
SELECT count(*) AS seeded_tasks,
       count(DISTINCT event_id) AS covered_events
FROM ai_perimeter_algorithm_task
WHERE task_no LIKE 'PDAT%';

-- 2) 状态分布：期望 SUCCEEDED 26 / FAILED 1 / TIMEOUT 1 / RUNNING 1 / QUEUED 1
SELECT execution_status, count(*) AS n
FROM ai_perimeter_algorithm_task
WHERE task_no LIKE 'PDAT%'
GROUP BY execution_status
ORDER BY n DESC, execution_status;

-- 3) 事件回填数：期望 = 任务总数
SELECT count(*) AS backfilled_events
FROM ai_perimeter_event e
JOIN ai_perimeter_algorithm_task t ON t.id = e.algorithm_task_id
WHERE t.task_no LIKE 'PDAT%';

-- 4) is_simulated 铁律核查：期望 0（不允许出现 FALSE）
SELECT count(*) AS not_simulated_rows
FROM ai_perimeter_algorithm_task
WHERE task_no LIKE 'PDAT%' AND is_simulated IS NOT TRUE;

-- 5) 受控引用核查：期望 0（media_refs 里不允许出现 http/本地绝对路径）
SELECT count(*) AS bad_media_refs
FROM ai_perimeter_algorithm_task
WHERE task_no LIKE 'PDAT%'
  AND (media_refs ~ 'https?://' OR media_refs ~ '^/' OR media_refs ~ '\\\\');

-- 6) 任务与事件同点同仓自洽：期望 0
SELECT count(*) AS mismatched_tasks
FROM ai_perimeter_algorithm_task t
JOIN ai_perimeter_event e ON e.id = t.event_id
WHERE t.task_no LIKE 'PDAT%'
  AND (t.point_id <> e.point_id OR t.warehouse_id <> e.warehouse_id);

-- 7) 置信度约束核查：期望 0
SELECT count(*) AS bad_confidence
FROM ai_perimeter_algorithm_task
WHERE task_no LIKE 'PDAT%'
  AND confidence IS NOT NULL
  AND (confidence < 0.55 OR confidence > 0.98);
