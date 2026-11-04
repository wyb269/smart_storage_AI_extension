-- =====================================================================
-- 粮面虫害智能识别功能 · 算法任务表建表脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：pestDetection
-- 表前缀：ai_pest_
--
-- 本脚本是「预留算法接缝」的落库部分：
--   接缝代码见 service/inter/pestDetection/algorithm/PestDetectionAlgorithmGateway.java
--            service/impl/pestDetection/algorithm/MockPestDetectionAlgorithmGateway.java
--   与视频遮挡 AlgorithmGateway 同构。当前仅有 Mock 适配器，
--   接真实算法时新增一个适配器实现即可，本表结构不需要改。
--
-- ======================== 执行前强制门禁 ========================
-- 本脚本执行前必须完成，且不得跳过：
--   1. 已获得用户对本次 DDL 执行动作的明确授权；
--   2. 连接显式指定数据库名 smart_storage_ai_extension；
--   3. 已执行 SELECT current_database(); 且返回值严格等于
--      smart_storage_ai_extension；
--   4. 目标对象已逐项确认。
--
-- 布尔式：can_execute = user_authorized
--                    AND (current_database = smart_storage_ai_extension)
--                    AND target_objects_confirmed
--
-- 任一条件为假，立即停止。禁止对其他数据库执行本脚本。
--
-- ⚠️ 权限说明：应用运行账号 zhlc 只有 DML 权限，CREATE TABLE 会失败。
--    DDL 必须用特权账号执行，执行完再 GRANT 给 zhlc（步骤 5）。
--    漏了 GRANT 的现象是：接口 500 且日志报 permission denied for table。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
-- 若返回其他名称/为空/无法判断，立即断开并停止，不得继续。

SELECT current_database();


-- ---------- 步骤 2：确认目标对象尚不存在 ----------
-- 预期返回 0 行。若非 0 行，必须停止并报告，不得直接 DROP。

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'ai_pest_algorithm_task';


-- =====================================================================
-- 以下 DDL 仅在步骤 1、2 均通过后执行
-- =====================================================================

-- =====================================================================
-- T5  ai_pest_algorithm_task  算法任务与结果
--
-- 与 ai_pest_event 是 1:N（一次检测任务可能不落事件），
-- 事件侧通过 ai_pest_event.algorithm_task_id 反向指过来。
--
-- 形状镜像 ai_occlusion_algorithm_task（39 列），主体维度换为粮面点位：
-- camera_config_id -> point_id；occlusion_type -> conclusion_type。
--
-- id 无序列、无默认值：由应用侧统一发号（雪花），与其余 ai_pest_*
-- 表保持一致，避免多数据源下序列不同步。
--
-- is_simulated 默认 TRUE：Mock 适配器产出的每条记录都必须显式标记，
-- 真实算法接入后由适配器写 FALSE，禁止混入真实生产统计。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_pest_algorithm_task (
    id                    BIGINT        NOT NULL,
    task_no               VARCHAR(64)   NOT NULL,
    event_id              BIGINT,
    point_id              BIGINT        NOT NULL,
    camera_id             VARCHAR(64),
    camera_code           VARCHAR(64),
    warehouse_id          BIGINT        NOT NULL,
    region_id             VARCHAR(64),
    org_id                BIGINT,

    task_type             VARCHAR(32)   NOT NULL,
    media_type            VARCHAR(32),
    media_refs            TEXT,
    normal_reference_ref  VARCHAR(512),
    roi                   VARCHAR(128),

    execution_status      VARCHAR(16)   NOT NULL,
    conclusion            VARCHAR(16),
    conclusion_type       VARCHAR(32),
    confidence            NUMERIC(5,4),

    model_name            VARCHAR(128),
    model_version         VARCHAR(64),
    algorithm_version     VARCHAR(64),
    config_version        VARCHAR(64),
    confidence_threshold  NUMERIC(5,4),
    media_digest          VARCHAR(128),

    started_at            TIMESTAMP,
    finished_at           TIMESTAMP,
    inference_duration_ms BIGINT,

    detections            TEXT,
    evidence_refs         TEXT,
    raw_result_ref        VARCHAR(512),

    error_code            VARCHAR(64),
    error_message         VARCHAR(512),
    retry_count           INT           NOT NULL DEFAULT 0,
    trace_id              VARCHAR(64),

    is_simulated          BOOLEAN       NOT NULL DEFAULT TRUE,

    created_by            VARCHAR(64),
    created_at            TIMESTAMP,
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,

    CONSTRAINT pk_ai_pest_algorithm_task PRIMARY KEY (id),
    CONSTRAINT uk_pst_task_no UNIQUE (task_no),
    -- 置信度是 0~1 的小数，写错单位（写成百分数）会静默污染阈值比较，
    -- 所以在库层直接挡住。
    CONSTRAINT ck_pst_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

COMMENT ON TABLE  ai_pest_algorithm_task IS '粮面虫害算法任务与结果';
COMMENT ON COLUMN ai_pest_algorithm_task.event_id           IS '产出了哪条检测事件；未落事件时为空';
COMMENT ON COLUMN ai_pest_algorithm_task.point_id           IS '指向 ai_pest_monitor_point.id，本表的功能内主体（对应遮挡表的 camera_config_id）';
COMMENT ON COLUMN ai_pest_algorithm_task.camera_id          IS '设备侧摄像头标识，与 ai_pest_event.camera_id 同源';
COMMENT ON COLUMN ai_pest_algorithm_task.region_id          IS '数据范围下推用；查询本表必须带范围条件';
COMMENT ON COLUMN ai_pest_algorithm_task.task_type          IS '任务类型，粮面虫害识别固定 PEST_DETECT';
COMMENT ON COLUMN ai_pest_algorithm_task.media_refs         IS '受控内部媒体引用JSON，禁止存放任意URL或本地绝对路径';
COMMENT ON COLUMN ai_pest_algorithm_task.execution_status   IS 'QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK';
COMMENT ON COLUMN ai_pest_algorithm_task.conclusion         IS '算法结论 NORMAL/ANOMALY/UNCERTAIN';
COMMENT ON COLUMN ai_pest_algorithm_task.conclusion_type    IS '采样结论类型，取值同 ai_pest_event.conclusion_type';
COMMENT ON COLUMN ai_pest_algorithm_task.raw_result_ref     IS '原始算法输出受控引用，前端永不直接解析';
COMMENT ON COLUMN ai_pest_algorithm_task.is_simulated       IS '模拟结果必须为 TRUE，不得混入真实生产统计';

-- 按点位查最近任务是主查询路径；事件反查是详情弹窗路径。
CREATE INDEX IF NOT EXISTS idx_pst_point ON ai_pest_algorithm_task (point_id, started_at);
CREATE INDEX IF NOT EXISTS idx_pst_event  ON ai_pest_algorithm_task (event_id);


-- ---------- 步骤 5：授权给应用运行账号 ----------
-- zhlc 只有 DML 权限，建表后必须显式授权，否则应用读写报 permission denied。
-- 本期未建物理外键，只授本表。

GRANT SELECT, INSERT, UPDATE, DELETE ON ai_pest_algorithm_task TO zhlc;


-- ---------- 步骤 6：自检 ----------
-- 预期：39 列。少一列说明执行中断，多一列说明有人手工改过表，都要停下来核对。

SELECT count(*) AS column_count
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'ai_pest_algorithm_task';

-- 预期返回 4 行：pk_ai_pest_algorithm_task / uk_pst_task_no
--               / idx_pst_point / idx_pst_event
SELECT indexname
FROM pg_indexes
WHERE tablename = 'ai_pest_algorithm_task'
ORDER BY indexname;

-- 预期返回 4 行：SELECT / INSERT / UPDATE / DELETE，grantee 均为 zhlc
SELECT grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_name = 'ai_pest_algorithm_task'
ORDER BY privilege_type;
