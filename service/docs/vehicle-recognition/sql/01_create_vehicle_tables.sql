-- =====================================================================
-- 车辆智能识别功能 · 建表脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：vehicleRecognition
-- 表前缀：ai_vehicle_
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
-- ⚠️ 重复执行防护必须显式列出全部表名，不得使用 LIKE 'ai_vehicle%'
--    之类的模式匹配。仓门异动那一版就吃过这个亏：6 张表前缀不统一，
--    LIKE 'ai_door%' 只命中 1 张，其余 5 张的防护完全失效。
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
      'ai_vehicle_monitor_point',
      'ai_vehicle_event',
      'ai_vehicle_evidence',
      'ai_vehicle_process_record'
  )
ORDER BY table_name;


-- =====================================================================
-- 以下 DDL 仅在步骤 1、2 均通过后执行
-- =====================================================================

-- =====================================================================
-- T1  ai_vehicle_monitor_point  车辆识别点位配置
--
-- 车辆功能没有「设备状态网格」语义（左侧是今日识别车辆），但仍建点位表存
-- 识别点位（东大门入口/西大门出口…），每个点位绑定一路摄像头。选中一条识别
-- 记录后，右侧播放的就是该记录所属点位的摄像头，因此点位表承担「记录→摄像头」的桥梁。
--
-- 与遮挡的 ai_occlusion_camera 同构：设备连接状态 + 告警状态 + 心跳，
-- 页面展示状态由前端 computeDisplayStatus 综合三者算出。
--
-- 安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_vehicle_monitor_point (
    id                BIGINT       NOT NULL,
    point_code        VARCHAR(64)  NOT NULL,
    point_name        VARCHAR(128) NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    warehouse_name    VARCHAR(128) NOT NULL,
    region_id         VARCHAR(32)  NOT NULL,
    org_id            BIGINT,
    camera_code       VARCHAR(64),
    camera_name       VARCHAR(128),
    camera_id         VARCHAR(64),
    device_status     VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN',
    alarm_status      VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_no           INT          NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP,
    latest_event_at   TIMESTAMP,
    created_by        VARCHAR(64),
    created_at        TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    remark            VARCHAR(255),
    CONSTRAINT pk_ai_vehicle_monitor_point PRIMARY KEY (id),
    CONSTRAINT uk_vh_point_code UNIQUE (point_code)
);

COMMENT ON TABLE  ai_vehicle_monitor_point IS '车辆识别点位配置';
COMMENT ON COLUMN ai_vehicle_monitor_point.point_code      IS '点位业务编码，如 GATE-E-IN';
COMMENT ON COLUMN ai_vehicle_monitor_point.point_name      IS '点位名称，如「东大门入口」';
COMMENT ON COLUMN ai_vehicle_monitor_point.warehouse_id    IS '所属仓库，数据范围强制过滤维度，必须与主库组织树 type=2 节点同源';
COMMENT ON COLUMN ai_vehicle_monitor_point.camera_code     IS '绑定的摄像头业务编码，如 CAM-003';
COMMENT ON COLUMN ai_vehicle_monitor_point.camera_id       IS '视频平台侧标识，非敏感';
COMMENT ON COLUMN ai_vehicle_monitor_point.device_status   IS '设备连接状态 ONLINE/OFFLINE/UNKNOWN';
COMMENT ON COLUMN ai_vehicle_monitor_point.alarm_status    IS '点位告警状态 NORMAL/ACTIVE/RECOVERED';
COMMENT ON COLUMN ai_vehicle_monitor_point.region_id       IS '行政区划码，用于数据范围过滤';
COMMENT ON COLUMN ai_vehicle_monitor_point.last_heartbeat_at IS '最近心跳；超过 90 分钟前端判为离线';
COMMENT ON COLUMN ai_vehicle_monitor_point.sort_no         IS '展示顺序';

CREATE INDEX IF NOT EXISTS idx_vhpoint_region ON ai_vehicle_monitor_point (region_id);
CREATE INDEX IF NOT EXISTS idx_vhpoint_wh     ON ai_vehicle_monitor_point (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_vhpoint_sort   ON ai_vehicle_monitor_point (sort_no);


-- =====================================================================
-- T2  ai_vehicle_event  车辆识别记录
--
-- 一条记录 = 某识别点位在某时刻识别到的一辆车。左侧「今日识别车辆」与底部
-- 「出入事件记录」都由本表按不同条件投影。事件编号前缀 VC。
-- 算法任务状态与人工核实状态相互独立；核实只写 verify_* 与 event_version，
-- 不得触碰 algorithm_* 与识别结果列（车牌/车型/颜色/方向）。
--
-- ⚠️ PII：plate_no 属个人轨迹信息。接口原样返回（原型显示完整车牌），
--    但后端日志严禁打印完整车牌（见 VehicleRecognitionServiceImpl 掩码说明）。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_vehicle_event (
    id                BIGINT        NOT NULL,
    event_no          VARCHAR(64)   NOT NULL,
    plate_no          VARCHAR(32)   NOT NULL,
    vehicle_type      VARCHAR(64),
    vehicle_color     VARCHAR(32),
    direction         VARCHAR(8)    NOT NULL,
    point_id          BIGINT        NOT NULL,
    gate_name         VARCHAR(128)  NOT NULL,
    recognized_at     TIMESTAMP     NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    warehouse_name    VARCHAR(128)  NOT NULL,
    region_id         VARCHAR(32)   NOT NULL,
    org_id            BIGINT,
    camera_code       VARCHAR(64),
    camera_id         VARCHAR(64),
    event_desc        VARCHAR(512),
    algorithm_task_id BIGINT,
    algorithm_status  VARCHAR(16),
    confidence        NUMERIC(5,4),
    verify_status     VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    processed_by      VARCHAR(64),
    processed_at      TIMESTAMP,
    verify_opinion    VARCHAR(512),
    event_version     INT           NOT NULL DEFAULT 0,
    is_simulated      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(64),
    created_at        TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    CONSTRAINT pk_ai_vehicle_event PRIMARY KEY (id),
    CONSTRAINT uk_vh_event_no UNIQUE (event_no),
    CONSTRAINT ck_vh_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_vh_verify_status CHECK (verify_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT ck_vh_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

COMMENT ON TABLE  ai_vehicle_event IS '车辆识别记录';
COMMENT ON COLUMN ai_vehicle_event.event_no        IS '事件编号，前缀 VC，如 VC202607300001';
COMMENT ON COLUMN ai_vehicle_event.plate_no        IS '车牌号（PII，日志严禁打印完整值）';
COMMENT ON COLUMN ai_vehicle_event.vehicle_type    IS '车辆类型，如柴油货车/空车/客车车型/新能源轿车/未识别车型';
COMMENT ON COLUMN ai_vehicle_event.vehicle_color   IS '车身颜色';
COMMENT ON COLUMN ai_vehicle_event.direction       IS '行驶方向 IN 入库 / OUT 出库';
COMMENT ON COLUMN ai_vehicle_event.point_id        IS '所属识别点位，指向 ai_vehicle_monitor_point.id';
COMMENT ON COLUMN ai_vehicle_event.gate_name       IS '识别地点快照（点位名），列表直接展示，不随点位配置变更改写';
COMMENT ON COLUMN ai_vehicle_event.recognized_at   IS '识别时间，列表默认按此倒序；今日过滤以 recognized_at::date = CURRENT_DATE 为准';
COMMENT ON COLUMN ai_vehicle_event.warehouse_id    IS '所属仓库，数据范围强制过滤维度';
COMMENT ON COLUMN ai_vehicle_event.algorithm_status IS '算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK';
COMMENT ON COLUMN ai_vehicle_event.verify_status   IS '核实状态 PENDING 待核实 / VERIFIED 已核实 / REJECTED 误报（预留，本期无写入路径）';
COMMENT ON COLUMN ai_vehicle_event.processed_by    IS '最近核实人，由后端按登录态写入，不接受前端传入';
COMMENT ON COLUMN ai_vehicle_event.event_version   IS '乐观锁版本号，提交处理时必须原样回传';
COMMENT ON COLUMN ai_vehicle_event.is_simulated    IS '是否模拟数据，必须显式标识，不得混入真实统计';

CREATE INDEX IF NOT EXISTS idx_vh_ev_recognized ON ai_vehicle_event (recognized_at DESC);
CREATE INDEX IF NOT EXISTS idx_vh_ev_wh         ON ai_vehicle_event (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_vh_ev_region     ON ai_vehicle_event (region_id);
CREATE INDEX IF NOT EXISTS idx_vh_ev_verify     ON ai_vehicle_event (verify_status);
CREATE INDEX IF NOT EXISTS idx_vh_ev_point      ON ai_vehicle_event (point_id);


-- =====================================================================
-- T3  ai_vehicle_evidence  车辆识别证据
--
-- 只存受控对象存储引用（mock:// 或 s3://），不存任意 URL 或本地路径。
-- 短时地址由 MediaGateway 在详情接口签发。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_vehicle_evidence (
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
    CONSTRAINT pk_ai_vehicle_evidence PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_vehicle_evidence IS '车辆识别证据';
COMMENT ON COLUMN ai_vehicle_evidence.evidence_type  IS '证据类型 VEHICLE_IMAGE/PLATE_CROP/ANNOTATED';
COMMENT ON COLUMN ai_vehicle_evidence.original_ref   IS '受控内部媒体引用，禁止存放任意 URL 或本地绝对路径';
COMMENT ON COLUMN ai_vehicle_evidence.annotated_ref  IS '算法标注图引用，与 original_ref 分离存储，不得互相覆盖';

CREATE INDEX IF NOT EXISTS idx_vh_evid_event ON ai_vehicle_evidence (event_id);


-- =====================================================================
-- T4  ai_vehicle_process_record  核实审计记录（追加式）
--
-- 只追加，永不 UPDATE / DELETE，保证「谁核实、为何这样核实」可追溯。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_vehicle_process_record (
    id              BIGINT       NOT NULL,
    event_id        BIGINT       NOT NULL,
    event_no        VARCHAR(64),
    status_before   VARCHAR(16),
    status_after    VARCHAR(16),
    process_result  VARCHAR(16),
    process_opinion VARCHAR(512),
    processed_by    VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMP    NOT NULL,
    source          VARCHAR(16)  NOT NULL DEFAULT 'WEB',
    created_at      TIMESTAMP,
    CONSTRAINT pk_ai_vehicle_process_record PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_vehicle_process_record IS '车辆识别记录核实审计（追加式）';
COMMENT ON COLUMN ai_vehicle_process_record.status_before IS '变更前状态，用于还原核实链路';
COMMENT ON COLUMN ai_vehicle_process_record.process_opinion IS '处理意见；「查看即标记」的自动记录会明确标注，与真实人工结论区分';
COMMENT ON COLUMN ai_vehicle_process_record.source IS '操作来源 WEB/SYSTEM';

CREATE INDEX IF NOT EXISTS idx_vh_pr_event ON ai_vehicle_process_record (event_id);


-- ---------- 步骤 5：授权给应用运行账号 ----------
-- zhlc 只有 DML 权限，建表后必须显式授权，否则应用读写报 permission denied。
-- 本期未建物理外键，逐表授权。

GRANT SELECT, INSERT, UPDATE, DELETE ON ai_vehicle_monitor_point TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_vehicle_event TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_vehicle_evidence TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_vehicle_process_record TO zhlc;


-- ---------- 步骤 6：自检 ----------
-- 预期返回 4 行：ai_vehicle_evidence / ai_vehicle_event /
--               ai_vehicle_monitor_point / ai_vehicle_process_record
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_vehicle_monitor_point',
      'ai_vehicle_event',
      'ai_vehicle_evidence',
      'ai_vehicle_process_record'
  )
ORDER BY table_name;

-- 预期每张表 4 行授权（SELECT/INSERT/UPDATE/DELETE，grantee=zhlc），共 16 行
SELECT table_name, count(*) AS granted_privs
FROM information_schema.role_table_grants
WHERE grantee = 'zhlc'
  AND table_name IN (
      'ai_vehicle_monitor_point',
      'ai_vehicle_event',
      'ai_vehicle_evidence',
      'ai_vehicle_process_record'
  )
GROUP BY table_name
ORDER BY table_name;
