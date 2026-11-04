-- =====================================================================
-- 粮面虫害智能识别功能 · 建表脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：pestDetection
-- 表前缀：ai_pest_
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
-- ⚠️ 重复执行防护必须显式列出全部表名，不得使用 LIKE 'ai_pest%'
--    之类的模式匹配（仓门异动那一版吃过这个亏）。
-- =====================================================================

-- ---------- 步骤 1：门禁校验（必须首先执行，单独运行） ----------
-- 预期返回：smart_storage_ai_extension
-- 若返回其他名称/为空/无法判断，立即断开并停止，不得继续。

SELECT current_database();


-- ---------- 步骤 2：确认目标对象尚不存在 ----------
-- 预期返回 0 行。若非 0 行，必须停止并报告。

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_pest_monitor_point',
      'ai_pest_event',
      'ai_pest_evidence',
      'ai_pest_process_record'
  )
ORDER BY table_name;


-- =====================================================================
-- 以下 DDL 仅在步骤 1、2 均通过后执行
-- =====================================================================

-- =====================================================================
-- T1  ai_pest_monitor_point  粮面监测点位配置
--
-- 对应原型图左侧「各仓虫情状态」网格的数据源。
-- 监测对象不是摄像头，而是「仓房 + 粮种 + 吨位」的粮面点位，
-- 一个点位绑定一路粮面摄像头（camera_id/camera_code）。
-- 设备连接状态 + 告警状态 + 心跳，页面展示状态由前端 computeDisplayStatus
-- 综合三者算出，本表不存「正常/告警」这种合并后的模糊字符串。
--
-- 安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_pest_monitor_point (
    id                BIGINT        NOT NULL,
    point_name        VARCHAR(128)  NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    warehouse_name    VARCHAR(128)  NOT NULL,
    grain_type        VARCHAR(32)   NOT NULL,
    grain_tonnage     NUMERIC(12,2) NOT NULL DEFAULT 0,
    region_id         VARCHAR(32)   NOT NULL,
    org_id            BIGINT,
    camera_id         VARCHAR(64),
    camera_code       VARCHAR(64),
    device_status     VARCHAR(16)   NOT NULL DEFAULT 'UNKNOWN',
    alarm_status      VARCHAR(16)   NOT NULL DEFAULT 'NORMAL',
    enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_no           INT           NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP,
    latest_event_at   TIMESTAMP,
    created_by        VARCHAR(64),
    created_at        TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    remark            VARCHAR(255),
    CONSTRAINT pk_ai_pest_monitor_point PRIMARY KEY (id),
    CONSTRAINT ck_pest_mp_tonnage CHECK (grain_tonnage >= 0)
);

COMMENT ON TABLE  ai_pest_monitor_point IS '粮面虫害监测点位配置（仓房+粮种+吨位，绑粮面摄像头）';
COMMENT ON COLUMN ai_pest_monitor_point.point_name    IS '点位名称，如「三号仓粮面」';
COMMENT ON COLUMN ai_pest_monitor_point.warehouse_id  IS '所属仓库，数据范围强制过滤维度，必须与主库组织树 type=2 节点同源';
COMMENT ON COLUMN ai_pest_monitor_point.grain_type    IS '粮种，如 小麦/玉米/稻谷';
COMMENT ON COLUMN ai_pest_monitor_point.grain_tonnage IS '存粮吨位，卡片标题「仓库 · 粮种吨位吨」拼接用';
COMMENT ON COLUMN ai_pest_monitor_point.camera_id     IS '绑定的粮面摄像头：视频平台侧标识，非敏感';
COMMENT ON COLUMN ai_pest_monitor_point.camera_code   IS '绑定的粮面摄像头：业务编码，如 CAM-003';
COMMENT ON COLUMN ai_pest_monitor_point.device_status IS '设备连接状态 ONLINE/OFFLINE/UNKNOWN';
COMMENT ON COLUMN ai_pest_monitor_point.alarm_status  IS '告警状态 NORMAL/ACTIVE/RECOVERED（ACTIVE=页面「告警」，与共享展示层判定一致）';
COMMENT ON COLUMN ai_pest_monitor_point.region_id     IS '行政区划码，用于数据范围过滤';
COMMENT ON COLUMN ai_pest_monitor_point.last_heartbeat_at IS '最近心跳；超过 90 分钟前端判为离线';
COMMENT ON COLUMN ai_pest_monitor_point.sort_no       IS '展示顺序';

CREATE INDEX IF NOT EXISTS idx_pmp_region ON ai_pest_monitor_point (region_id);
CREATE INDEX IF NOT EXISTS idx_pmp_wh     ON ai_pest_monitor_point (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_pmp_sort   ON ai_pest_monitor_point (sort_no);


-- =====================================================================
-- T2  ai_pest_event  粮面虫害采样检测事件
--
-- 对应原型图底部「虫害检测记录」表格，采样编号前缀 PT。
-- 三类状态相互独立：算法任务状态 / 算法结论 / 人工处理状态。
-- 人工处理只写 process_* 与 event_version，不得触碰 algorithm_*。
-- 密度判定：density > density_threshold 即密度超标；
-- 缺省阈值只来自后端 PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD，
-- 本表存的是每条采样当时生效的阈值快照，允许逐条不同。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_pest_event (
    id                  BIGINT        NOT NULL,
    event_no            VARCHAR(64)   NOT NULL,
    point_id            BIGINT        NOT NULL,
    camera_id           VARCHAR(64),
    camera_code         VARCHAR(64),
    sample_position     VARCHAR(128)  NOT NULL,
    warehouse_id        BIGINT        NOT NULL,
    warehouse_name      VARCHAR(128)  NOT NULL,
    grain_type          VARCHAR(32),
    region_id           VARCHAR(32)   NOT NULL,
    org_id              BIGINT,
    conclusion_type     VARCHAR(32)   NOT NULL,
    conclusion_type_name VARCHAR(64),
    insect_species      VARCHAR(64),
    density             NUMERIC(10,2) NOT NULL DEFAULT 0,
    density_threshold   NUMERIC(10,2) NOT NULL DEFAULT 0,
    event_desc          VARCHAR(512),
    occurred_at         TIMESTAMP     NOT NULL,
    recovered_at        TIMESTAMP,
    duration_seconds    BIGINT,
    algorithm_task_id   BIGINT,
    algorithm_status    VARCHAR(16),
    algorithm_result    VARCHAR(16),
    confidence          NUMERIC(5,4),
    process_status      VARCHAR(24)   NOT NULL DEFAULT 'PENDING',
    processed_by        VARCHAR(64),
    processed_at        TIMESTAMP,
    process_opinion     VARCHAR(512),
    event_version       INT           NOT NULL DEFAULT 0,
    is_simulated        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(64),
    created_at          TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    CONSTRAINT pk_ai_pest_event PRIMARY KEY (id),
    CONSTRAINT uk_pest_event_no UNIQUE (event_no),
    -- 密度与阈值均为「头/kg」，写负数没有业务含义，在库层直接挡住
    CONSTRAINT ck_pest_density CHECK (density >= 0),
    CONSTRAINT ck_pest_density_threshold CHECK (density_threshold >= 0),
    CONSTRAINT ck_pest_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

COMMENT ON TABLE  ai_pest_event IS '粮面虫害采样检测事件';
COMMENT ON COLUMN ai_pest_event.event_no            IS '采样编号，前缀 PT，如 PT202607290001';
COMMENT ON COLUMN ai_pest_event.point_id            IS '关联粮面监测点位，指向 ai_pest_monitor_point.id';
COMMENT ON COLUMN ai_pest_event.sample_position     IS '采样位置，如 东墙地上层 / 中墙下层 / 西北墙中层';
COMMENT ON COLUMN ai_pest_event.warehouse_id        IS '所属仓库，数据范围强制过滤维度';
COMMENT ON COLUMN ai_pest_event.conclusion_type     IS '检测结论 GRAIN_INSECT/DENSITY_HIGH/MOLD/OTHER';
COMMENT ON COLUMN ai_pest_event.insect_species      IS '虫种，如 玉米象/赤拟谷盗；无虫时为空';
COMMENT ON COLUMN ai_pest_event.density             IS '密度（头/kg），非负';
COMMENT ON COLUMN ai_pest_event.density_threshold   IS '判定阈值（头/kg），非负；density 超过它即判密度超标。缺省阈值来自后端常量，不写死在本脚本';
COMMENT ON COLUMN ai_pest_event.algorithm_status    IS '算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK';
COMMENT ON COLUMN ai_pest_event.algorithm_result    IS '算法结论 NORMAL/ANOMALY/UNCERTAIN；未产出时为 NULL，不得伪造为 NORMAL';
COMMENT ON COLUMN ai_pest_event.process_status      IS '人工处理状态 PENDING(未核实)/PROCESSING(处理中)/DONE(已处理)';
COMMENT ON COLUMN ai_pest_event.processed_by        IS '最近处理人，由后端按登录态写入，不接受前端传入';
COMMENT ON COLUMN ai_pest_event.event_version       IS '乐观锁版本号，提交处理时必须原样回传';
COMMENT ON COLUMN ai_pest_event.is_simulated        IS '是否模拟数据，必须显式标识，不得混入真实统计';

CREATE INDEX IF NOT EXISTS idx_pe_occurred ON ai_pest_event (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_pe_wh       ON ai_pest_event (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_pe_region   ON ai_pest_event (region_id);
CREATE INDEX IF NOT EXISTS idx_pe_status   ON ai_pest_event (process_status);
CREATE INDEX IF NOT EXISTS idx_pe_point    ON ai_pest_event (point_id);


-- =====================================================================
-- T3  ai_pest_evidence  检测证据
--
-- 只存受控对象存储引用（mock:// 或 s3://），不存任意 URL 或本地路径。
-- 短时地址由 MediaGateway 在详情接口签发。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_pest_evidence (
    id             BIGINT       NOT NULL,
    event_id       BIGINT       NOT NULL,
    evidence_type  VARCHAR(32)  NOT NULL,
    original_ref   VARCHAR(512) NOT NULL,
    annotated_ref  VARCHAR(512),
    thumbnail_ref  VARCHAR(512),
    captured_at    TIMESTAMP,
    camera_code    VARCHAR(64),
    content_hash   VARCHAR(128),
    sort_no        INT          NOT NULL DEFAULT 0,
    created_by     VARCHAR(64),
    created_at     TIMESTAMP,
    CONSTRAINT pk_ai_pest_evidence PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_pest_evidence IS '粮面虫害检测证据';
COMMENT ON COLUMN ai_pest_evidence.evidence_type  IS '证据类型 PEST_FRAME/NORMAL_REFERENCE/RECOVERY_IMAGE/EVENT_VIDEO';
COMMENT ON COLUMN ai_pest_evidence.original_ref   IS '受控内部媒体引用，禁止存放任意 URL 或本地绝对路径';
COMMENT ON COLUMN ai_pest_evidence.annotated_ref  IS '算法标注图引用，与 original_ref 分离存储，不得互相覆盖';

CREATE INDEX IF NOT EXISTS idx_pev_event ON ai_pest_evidence (event_id);


-- =====================================================================
-- T4  ai_pest_process_record  处理审计记录（追加式）
--
-- 只追加，永不 UPDATE / DELETE，保证「谁处理、为何这样处理」可追溯。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_pest_process_record (
    id              BIGINT       NOT NULL,
    event_id        BIGINT       NOT NULL,
    event_no        VARCHAR(64),
    status_before   VARCHAR(24),
    status_after    VARCHAR(24),
    process_result  VARCHAR(24),
    process_opinion VARCHAR(512),
    processed_by    VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMP    NOT NULL,
    source          VARCHAR(16)  NOT NULL DEFAULT 'WEB',
    created_at      TIMESTAMP,
    CONSTRAINT pk_ai_pest_process_record PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_pest_process_record IS '粮面虫害检测记录处理审计（追加式）';
COMMENT ON COLUMN ai_pest_process_record.status_before IS '变更前状态，用于还原处理链路';
COMMENT ON COLUMN ai_pest_process_record.process_opinion IS '处理意见；「查看即标记」的自动记录会明确标注，与真实人工结论区分';
COMMENT ON COLUMN ai_pest_process_record.source IS '操作来源 WEB/SYSTEM';

CREATE INDEX IF NOT EXISTS idx_ppr_event ON ai_pest_process_record (event_id);


-- =====================================================================
-- 授权给应用运行账号
-- zhlc 只有 DML 权限，建表后必须显式授权，否则应用读写报 permission denied。
-- 本期未建物理外键，逐表授权。
-- =====================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_pest_monitor_point TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_pest_event TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_pest_evidence TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_pest_process_record TO zhlc;


-- =====================================================================
-- 自检
-- =====================================================================
-- 1) 四张表全部存在：预期返回 4 行
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_pest_monitor_point',
      'ai_pest_event',
      'ai_pest_evidence',
      'ai_pest_process_record'
  )
ORDER BY table_name;

-- 2) 每张表都已授给 zhlc：预期 16 行（4 表 × 4 权限）
SELECT table_name, privilege_type
FROM information_schema.role_table_grants
WHERE grantee = 'zhlc'
  AND table_name IN (
      'ai_pest_monitor_point',
      'ai_pest_event',
      'ai_pest_evidence',
      'ai_pest_process_record'
  )
ORDER BY table_name, privilege_type;
