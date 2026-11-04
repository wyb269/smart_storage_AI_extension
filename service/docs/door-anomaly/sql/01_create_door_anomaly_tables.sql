-- =====================================================================
-- 仓门异动功能 · 建表脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 依据：AI_HANDOFF.md 第 39.2 / 39.4 / 39.6 节
-- 方案：docs/door-anomaly-db-plan.md（已确认）
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
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
-- 若返回其他名称/为空/无法判断，立即断开并停止，不得继续。

SELECT current_database();


-- ---------- 步骤 2：确认目标对象尚不存在 ----------
-- 预期返回 0 行。若非 0 行，必须停止并报告。
--
-- 注意：本功能 6 张表的前缀并不统一（ai_door_ / ai_camera_ / ai_algorithm_
-- / ai_anomaly_ / ai_event_）。此前使用 LIKE 'ai_door%' 只会命中
-- ai_door_config 一张表，导致「重复执行防护」对其余 5 张表完全失效。
-- 因此改为显式列出 6 个表名，不使用模式匹配。

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_door_config',
      'ai_camera_mapping',
      'ai_algorithm_task',
      'ai_anomaly_event',
      'ai_event_evidence',
      'ai_event_process_record'
  )
ORDER BY table_name;


-- =====================================================================
-- 以下 DDL 仅在步骤 1、2 均通过后执行
-- =====================================================================

-- =====================================================================
-- T1  ai_door_config  仓门配置（逻辑模型 27.1）
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_door_config (
    id                  BIGINT       NOT NULL,
    door_code           VARCHAR(64)  NOT NULL,
    door_name           VARCHAR(128) NOT NULL,
    warehouse_id        BIGINT       NOT NULL,
    warehouse_name      VARCHAR(128) NOT NULL,
    region_id           VARCHAR(32)  NOT NULL,
    org_id              BIGINT,
    camera_id           VARCHAR(64),
    camera_code         VARCHAR(64),
    device_status       VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN',
    door_physical_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    alarm_status        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_no             INT          NOT NULL DEFAULT 0,
    last_heartbeat_at   TIMESTAMP,
    latest_event_at     TIMESTAMP,
    created_by          VARCHAR(64),
    created_at          TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    remark              VARCHAR(255),
    CONSTRAINT pk_ai_door_config PRIMARY KEY (id),
    CONSTRAINT uk_dc_code UNIQUE (door_code)
);

COMMENT ON TABLE  ai_door_config IS '仓门配置';
COMMENT ON COLUMN ai_door_config.door_code            IS '仓门业务编码';
COMMENT ON COLUMN ai_door_config.device_status        IS '设备连接状态 ONLINE/OFFLINE/UNKNOWN';
COMMENT ON COLUMN ai_door_config.door_physical_status IS '仓门物理状态 OPEN/CLOSED/UNKNOWN';
COMMENT ON COLUMN ai_door_config.alarm_status         IS '告警状态 NORMAL/ACTIVE/RECOVERED';
COMMENT ON COLUMN ai_door_config.region_id            IS '行政区划码，用于数据范围过滤';
COMMENT ON COLUMN ai_door_config.sort_no              IS '展示顺序';

CREATE INDEX IF NOT EXISTS idx_dc_region ON ai_door_config (region_id);
CREATE INDEX IF NOT EXISTS idx_dc_sort   ON ai_door_config (sort_no);


-- =====================================================================
-- T2  ai_camera_mapping  摄像头映射（逻辑模型 27.2）
--
-- 安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。
-- 视频平台凭据由视频/媒体网关侧持有，本表仅存非敏感标识。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_camera_mapping (
    id                  BIGINT       NOT NULL,
    door_id             BIGINT       NOT NULL,
    camera_id           VARCHAR(64)  NOT NULL,
    camera_code         VARCHAR(64)  NOT NULL,
    camera_name         VARCHAR(128),
    device_code         VARCHAR(64),
    channel_no          VARCHAR(32),
    supported_protocols VARCHAR(128),
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(64),
    created_at          TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    CONSTRAINT pk_ai_camera_mapping PRIMARY KEY (id),
    CONSTRAINT uk_cm_door_camera UNIQUE (door_id, camera_id)
);

COMMENT ON TABLE  ai_camera_mapping IS '摄像头映射（不含任何凭据）';
COMMENT ON COLUMN ai_camera_mapping.device_code IS '视频平台设备号，非敏感标识';
COMMENT ON COLUMN ai_camera_mapping.channel_no  IS '视频平台通道号';

CREATE INDEX IF NOT EXISTS idx_cm_door   ON ai_camera_mapping (door_id);
CREATE INDEX IF NOT EXISTS idx_cm_camera ON ai_camera_mapping (camera_id);


-- =====================================================================
-- T4  ai_algorithm_task  算法任务与结果（逻辑模型 27.4）
--
-- 注意：先于 T3 创建，因为 T3 有 algorithm_task_id 幂等约束逻辑关联。
-- 本期不建物理外键，故创建顺序无强制要求。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_algorithm_task (
    id                      BIGINT        NOT NULL,
    task_no                 VARCHAR(64)   NOT NULL,
    candidate_event_id      BIGINT,
    door_id                 BIGINT        NOT NULL,
    camera_id               VARCHAR(64),
    warehouse_id            BIGINT        NOT NULL,
    task_type               VARCHAR(32)   NOT NULL,
    media_type              VARCHAR(32),
    media_refs              TEXT,
    normal_reference_ref    VARCHAR(512),
    roi                     VARCHAR(128),
    execution_status        VARCHAR(16)   NOT NULL,
    conclusion              VARCHAR(16),
    anomaly_type            VARCHAR(32),
    confidence              NUMERIC(5,4),
    model_name              VARCHAR(128),
    model_version           VARCHAR(64),
    algorithm_version       VARCHAR(64),
    config_version          VARCHAR(64),
    confidence_threshold    NUMERIC(5,4),
    media_digest            VARCHAR(128),
    started_at              TIMESTAMP,
    finished_at             TIMESTAMP,
    inference_duration_ms   BIGINT,
    detections              TEXT,
    evidence_refs           TEXT,
    raw_result_ref          VARCHAR(512),
    error_code              VARCHAR(64),
    error_message           VARCHAR(512),
    retry_count             INT           NOT NULL DEFAULT 0,
    trace_id                VARCHAR(64),
    is_simulated            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by              VARCHAR(64),
    created_at              TIMESTAMP,
    updated_by              VARCHAR(64),
    updated_at              TIMESTAMP,
    CONSTRAINT pk_ai_algorithm_task PRIMARY KEY (id),
    CONSTRAINT uk_at_task_no UNIQUE (task_no),
    CONSTRAINT ck_at_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

COMMENT ON TABLE  ai_algorithm_task IS '算法任务与结果';
COMMENT ON COLUMN ai_algorithm_task.task_type        IS '任务类型，仓门异动识别固定 DOOR_ANOMALY_DETECT';
COMMENT ON COLUMN ai_algorithm_task.execution_status IS 'QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK';
COMMENT ON COLUMN ai_algorithm_task.conclusion       IS '算法结论 NORMAL/ANOMALY/UNCERTAIN';
COMMENT ON COLUMN ai_algorithm_task.media_refs       IS '受控内部媒体引用JSON，禁止存放任意URL或本地绝对路径';
COMMENT ON COLUMN ai_algorithm_task.raw_result_ref   IS '原始算法输出受控引用，前端永不直接解析';
COMMENT ON COLUMN ai_algorithm_task.is_simulated     IS '是否模拟数据，必须显式标识';

CREATE INDEX IF NOT EXISTS idx_at_exec  ON ai_algorithm_task (execution_status, created_at);
CREATE INDEX IF NOT EXISTS idx_at_trace ON ai_algorithm_task (trace_id);


-- =====================================================================
-- T3  ai_anomaly_event  异动事件主表（逻辑模型 27.3）
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_anomaly_event (
    id                BIGINT        NOT NULL,
    event_no          VARCHAR(64)   NOT NULL,
    door_id           BIGINT        NOT NULL,
    door_name         VARCHAR(128)  NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    warehouse_name    VARCHAR(128)  NOT NULL,
    camera_id         VARCHAR(64),
    camera_code       VARCHAR(64),
    region_id         VARCHAR(32)   NOT NULL,
    org_id            BIGINT,
    anomaly_type      VARCHAR(32)   NOT NULL,
    anomaly_type_name VARCHAR(64),
    event_desc        VARCHAR(512),
    occurred_at       TIMESTAMP     NOT NULL,
    recovered_at      TIMESTAMP,
    algorithm_task_id BIGINT,
    algorithm_status  VARCHAR(16)   NOT NULL,
    algorithm_result  VARCHAR(16),
    confidence        NUMERIC(5,4),
    process_status    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    processed_by      VARCHAR(64),
    processed_at      TIMESTAMP,
    process_opinion   VARCHAR(1000),
    event_version     INT           NOT NULL DEFAULT 0,
    is_simulated      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(64),
    created_at        TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    CONSTRAINT pk_ai_anomaly_event PRIMARY KEY (id),
    CONSTRAINT uk_ae_event_no UNIQUE (event_no),
    CONSTRAINT uk_ae_task_id_sim UNIQUE (algorithm_task_id, is_simulated),
    CONSTRAINT ck_ae_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

COMMENT ON TABLE  ai_anomaly_event IS '异动事件';
COMMENT ON COLUMN ai_anomaly_event.algorithm_status IS '算法任务状态，与算法结论、人工处理状态三者相互独立';
COMMENT ON COLUMN ai_anomaly_event.algorithm_result IS '算法结论 NORMAL/ANOMALY/UNCERTAIN，人工不可改写。算法未成功产出结论时为空，不得伪造为 NORMAL';
COMMENT ON COLUMN ai_anomaly_event.process_status   IS '人工处理状态，不得用布尔值存储';
COMMENT ON COLUMN ai_anomaly_event.event_version     IS '乐观锁版本号，提交处理时回传，不匹配则拒绝覆盖';
COMMENT ON COLUMN ai_anomaly_event.is_simulated      IS '是否模拟数据，默认TRUE为安全默认';

-- 默认列表按发生时间倒序（第18.3节）
CREATE INDEX IF NOT EXISTS idx_ae_occurred_at  ON ai_anomaly_event (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ae_process_time ON ai_anomaly_event (process_status, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ae_door_time    ON ai_anomaly_event (door_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ae_wh_time      ON ai_anomaly_event (warehouse_id, occurred_at DESC);
-- 支撑行政区数据范围过滤（@RegionSecurity）
CREATE INDEX IF NOT EXISTS idx_ae_region_time  ON ai_anomaly_event (region_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ae_type_time    ON ai_anomaly_event (anomaly_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ae_alg_status   ON ai_anomaly_event (algorithm_status);
CREATE INDEX IF NOT EXISTS idx_ae_alg_result   ON ai_anomaly_event (algorithm_result);


-- =====================================================================
-- T5  ai_event_evidence  事件证据（逻辑模型 27.5）
--
-- 第17.4节：原始资源与算法标注资源必须分离存储，不得互相覆盖。
-- 第39.6节：媒体文件不入库，仅存对象存储引用与元数据。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_event_evidence (
    id                   BIGINT       NOT NULL,
    event_id             BIGINT       NOT NULL,
    evidence_type        VARCHAR(32)  NOT NULL,
    object_key           VARCHAR(512) NOT NULL,
    annotated_object_key VARCHAR(512),
    thumbnail_object_key VARCHAR(512),
    captured_at          TIMESTAMP    NOT NULL,
    camera_code          VARCHAR(64),
    content_hash         VARCHAR(128),
    sort_no              INT          NOT NULL DEFAULT 0,
    created_by           VARCHAR(64),
    created_at           TIMESTAMP,
    CONSTRAINT pk_ai_event_evidence PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_event_evidence IS '事件证据元数据（媒体不入库）';
COMMENT ON COLUMN ai_event_evidence.object_key           IS '原始资源对象存储键';
COMMENT ON COLUMN ai_event_evidence.annotated_object_key IS '算法标注图对象键，与原始资源分离存储';
COMMENT ON COLUMN ai_event_evidence.captured_at          IS '采集时间，每张证据必须展示';

CREATE INDEX IF NOT EXISTS idx_ee_event ON ai_event_evidence (event_id, sort_no);


-- =====================================================================
-- T6  ai_event_process_record  处理记录（逻辑模型 27.6）
--
-- 第17.6节：追加式记录，不得只覆盖最后一次处理意见。
-- 第29.4节：处理人由后端按登录态写入，不可由前端传入。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_event_process_record (
    id               BIGINT        NOT NULL,
    event_id         BIGINT        NOT NULL,
    event_no         VARCHAR(64)   NOT NULL,
    status_before    VARCHAR(32)   NOT NULL,
    status_after     VARCHAR(32)   NOT NULL,
    process_result   VARCHAR(32)   NOT NULL,
    process_opinion  VARCHAR(1000) NOT NULL,
    processed_by     VARCHAR(64)   NOT NULL,
    processed_at     TIMESTAMP     NOT NULL,
    source           VARCHAR(32)   NOT NULL DEFAULT 'WEB',
    created_at       TIMESTAMP     NOT NULL,
    CONSTRAINT pk_ai_event_process_record PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_event_process_record IS '事件处理记录（追加式，不可变）';
COMMENT ON COLUMN ai_event_process_record.process_result  IS 'VERIFIED_TRUE 真实异常 / VERIFIED_FALSE 误报';
COMMENT ON COLUMN ai_event_process_record.processed_by    IS '处理人，后端按登录态写入';
COMMENT ON COLUMN ai_event_process_record.process_opinion IS '处理意见，按普通文本存储与输出';

CREATE INDEX IF NOT EXISTS idx_epr_event ON ai_event_process_record (event_id, processed_at DESC);
CREATE INDEX IF NOT EXISTS idx_epr_no    ON ai_event_process_record (event_no);


-- =====================================================================
-- 执行后自检
-- =====================================================================
-- 必须返回下列 6 行；少于 6 行说明有表未创建成功，不得继续下一步。
-- 此前使用 LIKE 'ai_door%' 只会返回 1 行，与「预期返回 6 张表」的注释矛盾，
-- 自检实际从未通过过断言。

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_door_config',
      'ai_camera_mapping',
      'ai_algorithm_task',
      'ai_anomaly_event',
      'ai_event_evidence',
      'ai_event_process_record'
  )
ORDER BY table_name;
