-- =====================================================================
-- 视频遮挡检测功能 · 建表脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：videoOcclusion
-- 表前缀：ai_occlusion_
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
-- ⚠️ 重复执行防护必须显式列出全部表名，不得使用 LIKE 'ai_occlusion%'
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
      'ai_occlusion_camera',
      'ai_occlusion_event',
      'ai_occlusion_evidence',
      'ai_occlusion_process_record'
  )
ORDER BY table_name;


-- =====================================================================
-- 以下 DDL 仅在步骤 1、2 均通过后执行
-- =====================================================================

-- =====================================================================
-- T1  ai_occlusion_camera  遮挡监测摄像头配置
--
-- 对应原型图左侧「摄像头状态」网格的数据源。
-- 与仓门异动的 ai_door_config 同构：设备连接状态 + 告警状态 + 心跳，
-- 页面展示状态由前端 computeDisplayStatus 综合三者算出，
-- 本表不存「正常/遮挡/离线」这种合并后的模糊字符串。
--
-- 安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_occlusion_camera (
    id                BIGINT       NOT NULL,
    camera_code       VARCHAR(64)  NOT NULL,
    camera_name       VARCHAR(128) NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    warehouse_name    VARCHAR(128) NOT NULL,
    region_id         VARCHAR(32)  NOT NULL,
    org_id            BIGINT,
    camera_id         VARCHAR(64),
    device_status     VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN',
    occlusion_status  VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_no           INT          NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP,
    latest_event_at   TIMESTAMP,
    created_by        VARCHAR(64),
    created_at        TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    remark            VARCHAR(255),
    CONSTRAINT pk_ai_occlusion_camera PRIMARY KEY (id),
    CONSTRAINT uk_oc_camera_code UNIQUE (camera_code)
);

COMMENT ON TABLE  ai_occlusion_camera IS '视频遮挡监测摄像头配置';
COMMENT ON COLUMN ai_occlusion_camera.camera_code      IS '摄像头业务编码，页面显示为 CAM-001 这类';
COMMENT ON COLUMN ai_occlusion_camera.camera_name      IS '安装位置名称，如「一号仓库东门」';
COMMENT ON COLUMN ai_occlusion_camera.warehouse_id     IS '所属仓库，数据范围强制过滤维度，必须与主库组织树 type=2 节点同源';
COMMENT ON COLUMN ai_occlusion_camera.device_status    IS '设备连接状态 ONLINE/OFFLINE/UNKNOWN';
COMMENT ON COLUMN ai_occlusion_camera.occlusion_status IS '遮挡告警状态 NORMAL/ACTIVE/RECOVERED';
COMMENT ON COLUMN ai_occlusion_camera.region_id        IS '行政区划码，用于数据范围过滤';
COMMENT ON COLUMN ai_occlusion_camera.last_heartbeat_at IS '最近心跳；超过 90 分钟前端判为离线';
COMMENT ON COLUMN ai_occlusion_camera.sort_no          IS '展示顺序';

CREATE INDEX IF NOT EXISTS idx_occam_region ON ai_occlusion_camera (region_id);
CREATE INDEX IF NOT EXISTS idx_occam_wh     ON ai_occlusion_camera (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_occam_sort   ON ai_occlusion_camera (sort_no);


-- =====================================================================
-- T2  ai_occlusion_event  遮挡事件
--
-- 对应原型图底部「遮挡历史记录」表格，事件编号前缀 VD。
-- 三类状态相互独立：算法任务状态 / 算法结论 / 人工处理状态。
-- 人工处理只写 process_* 与 event_version，不得触碰 algorithm_*。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_occlusion_event (
    id                BIGINT        NOT NULL,
    event_no          VARCHAR(64)   NOT NULL,
    camera_config_id  BIGINT        NOT NULL,
    camera_id         VARCHAR(64),
    camera_code       VARCHAR(64),
    camera_position   VARCHAR(128)  NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    warehouse_name    VARCHAR(128)  NOT NULL,
    region_id         VARCHAR(32)   NOT NULL,
    org_id            BIGINT,
    occlusion_type    VARCHAR(32)   NOT NULL,
    occlusion_type_name VARCHAR(64),
    event_desc        VARCHAR(512),
    occurred_at       TIMESTAMP     NOT NULL,
    recovered_at      TIMESTAMP,
    duration_seconds  BIGINT,
    algorithm_task_id BIGINT,
    algorithm_status  VARCHAR(16),
    algorithm_result  VARCHAR(16),
    confidence        NUMERIC(5,4),
    process_status    VARCHAR(24)   NOT NULL DEFAULT 'PENDING',
    processed_by      VARCHAR(64),
    processed_at      TIMESTAMP,
    process_opinion   VARCHAR(512),
    event_version     INT           NOT NULL DEFAULT 0,
    is_simulated      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(64),
    created_at        TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    CONSTRAINT pk_ai_occlusion_event PRIMARY KEY (id),
    CONSTRAINT uk_oc_event_no UNIQUE (event_no),
    CONSTRAINT ck_oc_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

COMMENT ON TABLE  ai_occlusion_event IS '视频遮挡事件';
COMMENT ON COLUMN ai_occlusion_event.event_no           IS '事件编号，前缀 VD，如 VD202607290001';
COMMENT ON COLUMN ai_occlusion_event.camera_position    IS '摄像头位置快照，列表直接展示，不随配置变更而改写';
COMMENT ON COLUMN ai_occlusion_event.warehouse_id       IS '所属仓库，数据范围强制过滤维度';
COMMENT ON COLUMN ai_occlusion_event.occlusion_type     IS '遮挡类型 FULL/PARTIAL/PAINT/LENS_COVER/OTHER';
COMMENT ON COLUMN ai_occlusion_event.algorithm_status   IS '算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK';
COMMENT ON COLUMN ai_occlusion_event.algorithm_result   IS '算法结论 NORMAL/ANOMALY/UNCERTAIN；未产出时为 NULL，不得伪造为 NORMAL';
COMMENT ON COLUMN ai_occlusion_event.process_status     IS '人工处理状态 PENDING/PROCESSING/VERIFIED_TRUE/VERIFIED_FALSE/CLOSED；页面只展示未处理/已处理两态';
COMMENT ON COLUMN ai_occlusion_event.processed_by       IS '最近处理人，由后端按登录态写入，不接受前端传入';
COMMENT ON COLUMN ai_occlusion_event.event_version      IS '乐观锁版本号，提交处理时必须原样回传';
COMMENT ON COLUMN ai_occlusion_event.is_simulated       IS '是否模拟数据，必须显式标识，不得混入真实统计';

CREATE INDEX IF NOT EXISTS idx_oc_ev_occurred ON ai_occlusion_event (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_oc_ev_wh       ON ai_occlusion_event (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_oc_ev_region   ON ai_occlusion_event (region_id);
CREATE INDEX IF NOT EXISTS idx_oc_ev_status   ON ai_occlusion_event (process_status);
CREATE INDEX IF NOT EXISTS idx_oc_ev_camera   ON ai_occlusion_event (camera_config_id);


-- =====================================================================
-- T3  ai_occlusion_evidence  遮挡证据
--
-- 只存受控对象存储引用（mock:// 或 s3://），不存任意 URL 或本地路径。
-- 短时地址由 MediaGateway 在详情接口签发。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_occlusion_evidence (
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
    CONSTRAINT pk_ai_occlusion_evidence PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_occlusion_evidence IS '视频遮挡证据';
COMMENT ON COLUMN ai_occlusion_evidence.evidence_type  IS '证据类型 OCCLUDED_FRAME/NORMAL_REFERENCE/RECOVERY_IMAGE/EVENT_VIDEO';
COMMENT ON COLUMN ai_occlusion_evidence.original_ref   IS '受控内部媒体引用，禁止存放任意 URL 或本地绝对路径';
COMMENT ON COLUMN ai_occlusion_evidence.annotated_ref  IS '算法标注图引用，与 original_ref 分离存储，不得互相覆盖';

CREATE INDEX IF NOT EXISTS idx_oc_evid_event ON ai_occlusion_evidence (event_id);


-- =====================================================================
-- T4  ai_occlusion_process_record  处理审计记录（追加式）
--
-- 只追加，永不 UPDATE / DELETE，保证「谁处理、为何这样处理」可追溯。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_occlusion_process_record (
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
    CONSTRAINT pk_ai_occlusion_process_record PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_occlusion_process_record IS '视频遮挡事件处理审计记录（追加式）';
COMMENT ON COLUMN ai_occlusion_process_record.status_before IS '变更前状态，用于还原处理链路';
COMMENT ON COLUMN ai_occlusion_process_record.process_opinion IS '处理意见；「查看即标记」的自动记录会明确标注，与真实人工结论区分';
COMMENT ON COLUMN ai_occlusion_process_record.source IS '操作来源 WEB/SYSTEM';

CREATE INDEX IF NOT EXISTS idx_oc_pr_event ON ai_occlusion_process_record (event_id);


-- =====================================================================
-- 授权给应用运行账号
--
-- zhlc 只有 DML 权限，建完表不授权的话，接口会在运行期报
-- permission denied for table（表现为 500，而不是启动失败）。
-- 本段是补漏：这几张表当初是在库里单独 GRANT 的，脚本里没写，
-- 换环境重放 01 就会缺权限。
-- =====================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_occlusion_camera         TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_occlusion_event          TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_occlusion_evidence       TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_occlusion_process_record TO zhlc;

-- 自检：预期 4 张表 × 4 种权限 = 16 行
SELECT table_name, privilege_type
FROM information_schema.role_table_grants
WHERE grantee = 'zhlc'
  AND table_name IN (
      'ai_occlusion_camera',
      'ai_occlusion_event',
      'ai_occlusion_evidence',
      'ai_occlusion_process_record'
  )
ORDER BY table_name, privilege_type;
