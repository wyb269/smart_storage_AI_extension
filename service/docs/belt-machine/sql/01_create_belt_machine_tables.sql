-- =====================================================================
-- 皮带机下站人监测功能 · 建表脚本
--
-- 目标数据库：smart_storage_ai_extension（唯一允许操作的数据库）
-- 功能标识：beltMachine
-- 表前缀：ai_belt_
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
-- ⚠️ 重复执行防护必须显式列出全部表名，不得使用 LIKE 'ai_belt%'
--    之类的模式匹配。仓门异动那一版就吃过这个亏：6 张表前缀不统一，
--    LIKE 'ai_door%' 只命中 1 张，其余 5 张的防护完全失效。
--
-- ⚠️ 权限说明：应用运行账号 zhlc 只有 DML 权限，CREATE TABLE 会失败。
--    DDL 必须用特权账号执行，执行完再 GRANT 给 zhlc（步骤 6）。
--    漏了 GRANT 的现象是：接口 500 且日志报 permission denied for table。
--    视频遮挡那一版的 01 脚本就没写 GRANT，只有 05 写了，
--    本脚本对四张表逐一授权，不再重复那个缺口。
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
      'ai_belt_monitor_point',
      'ai_belt_event',
      'ai_belt_evidence',
      'ai_belt_process_record'
  )
ORDER BY table_name;


-- =====================================================================
-- 以下 DDL 仅在步骤 1、2 均通过后执行
-- =====================================================================

-- =====================================================================
-- T1  ai_belt_monitor_point  皮带机监测点位配置
--
-- 对应原型图左侧「皮带机情况概况」网格的数据源。
-- 与视频遮挡的 ai_occlusion_camera 同构：设备连接状态 + 告警状态 + 心跳，
-- 页面展示状态由前端 computeDisplayStatus 综合三者算出，
-- 本表不存「正常/报警/离线」这种合并后的模糊字符串。
--
-- ★ 与遮挡那张表的本质差异：本表主体是【皮带机】，摄像头是它的附属。
--   因此多了一组 camera_* 绑定列，且「未部署」态由
--   device_status='UNKNOWN' AND camera_id IS NULL 两个条件共同表达
--   （皮带机已登记台账但还没接摄像头）。
--
-- 安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_belt_monitor_point (
    id                BIGINT       NOT NULL,
    device_code       VARCHAR(64)  NOT NULL,
    device_name       VARCHAR(128) NOT NULL,
    area_name         VARCHAR(128) NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    warehouse_name    VARCHAR(128) NOT NULL,
    region_id         VARCHAR(32)  NOT NULL,
    org_id            BIGINT,
    camera_id         VARCHAR(64),
    camera_code       VARCHAR(64),
    camera_name       VARCHAR(128),
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
    CONSTRAINT pk_ai_belt_monitor_point PRIMARY KEY (id),
    CONSTRAINT uk_belt_device_code UNIQUE (device_code),
    -- 「未部署」的定义在库层就要成立：device_status='UNKNOWN' 单独出现
    -- 表示「绑了摄像头但设备态未知」（页面「未知」），
    -- 只有同时 camera_id IS NULL 才是「未部署」。
    -- 这条 CHECK 挡住「绑了摄像头却把 camera_id 写成空」的半吊子行。
    CONSTRAINT ck_belt_point_camera_pair CHECK (
        (camera_id IS NULL AND camera_code IS NULL)
        OR (camera_id IS NOT NULL AND camera_code IS NOT NULL)
    )
);

COMMENT ON TABLE  ai_belt_monitor_point IS '皮带机下站人监测点位配置（主体是皮带机，摄像头为其绑定项）';
COMMENT ON COLUMN ai_belt_monitor_point.device_code      IS '皮带机业务编码，如 BELT-001';
COMMENT ON COLUMN ai_belt_monitor_point.device_name      IS '皮带机名称，如「一号皮带机」，即左侧卡片标题';
COMMENT ON COLUMN ai_belt_monitor_point.area_name        IS '安装作业区名称，如「一号仓库作业区」';
COMMENT ON COLUMN ai_belt_monitor_point.warehouse_id     IS '所属仓库，数据范围强制过滤维度，必须与主库组织树 type=2 节点同源';
COMMENT ON COLUMN ai_belt_monitor_point.camera_id        IS '绑定摄像头的视频平台标识；IS NULL 表示未绑定摄像头';
COMMENT ON COLUMN ai_belt_monitor_point.camera_code      IS '绑定摄像头业务编码，右侧实时监控标题使用';
COMMENT ON COLUMN ai_belt_monitor_point.camera_name      IS '绑定摄像头位置名称，右侧实时监控标题使用';
COMMENT ON COLUMN ai_belt_monitor_point.device_status    IS '设备连接状态 ONLINE/OFFLINE/UNKNOWN';
COMMENT ON COLUMN ai_belt_monitor_point.alarm_status     IS '行为告警状态 NORMAL/ACTIVE/RECOVERED；共享前端 displayStatus 只认 ACTIVE 为有效告警，故不用 spec 里的 ALARM';
COMMENT ON COLUMN ai_belt_monitor_point.last_heartbeat_at IS '最近心跳；超过 90 分钟前端判为离线';
COMMENT ON COLUMN ai_belt_monitor_point.sort_no          IS '展示顺序';
COMMENT ON COLUMN ai_belt_monitor_point.enabled          IS 'FALSE 表示点位已停用，网格与取流一律不返回';

CREATE INDEX IF NOT EXISTS idx_belt_point_region ON ai_belt_monitor_point (region_id);
CREATE INDEX IF NOT EXISTS idx_belt_point_wh     ON ai_belt_monitor_point (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_belt_point_sort   ON ai_belt_monitor_point (sort_no);


-- =====================================================================
-- T2  ai_belt_event  皮带机下站人事件
--
-- 对应原型图底部「闲人历史记录」表格，事件编号前缀 BC。
-- 三类状态相互独立：算法任务状态 / 算法结论 / 人工处理状态。
-- 人工处理只写 process_* 与 event_version，不得触碰 algorithm_*。
--
-- ⚠️ device_name / camera_code 是【发生时刻快照】，不是外键关联列。
--    点位改名或换绑摄像头后，历史记录必须保持原样，
--    否则会出现「一号皮带机」和「二号皮带机」互相错挂的脏数据。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_belt_event (
    id                  BIGINT        NOT NULL,
    event_no            VARCHAR(64)   NOT NULL,
    point_id            BIGINT        NOT NULL,
    device_code         VARCHAR(64),
    device_name         VARCHAR(128)  NOT NULL,
    camera_id           VARCHAR(64),
    camera_code         VARCHAR(64),
    warehouse_id        BIGINT        NOT NULL,
    warehouse_name      VARCHAR(128)  NOT NULL,
    region_id           VARCHAR(32)   NOT NULL,
    org_id              BIGINT,
    conclusion_type     VARCHAR(32)   NOT NULL,
    conclusion_type_name VARCHAR(64),
    event_desc          VARCHAR(512),
    person_count        INT,
    occurred_at         TIMESTAMP     NOT NULL,
    recovered_at        TIMESTAMP,
    duration_seconds    BIGINT,
    algorithm_task_id   BIGINT,
    algorithm_status    VARCHAR(16),
    algorithm_result    VARCHAR(16),
    confidence          NUMERIC(5,4),
    process_status      VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    processed_by        VARCHAR(64),
    processed_at        TIMESTAMP,
    process_opinion     VARCHAR(512),
    event_version       INT           NOT NULL DEFAULT 0,
    is_simulated        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(64),
    created_at          TIMESTAMP,
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    CONSTRAINT pk_ai_belt_event PRIMARY KEY (id),
    CONSTRAINT uk_belt_event_no UNIQUE (event_no),
    -- 置信度是 0~1 的小数，写错单位（写成百分数）会静默污染阈值比较，
    -- 所以在库层直接挡住。
    CONSTRAINT ck_belt_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    -- 处理状态三值是本功能与视频遮挡（五值）的显式差异，
    -- 用 CHECK 钉死，防止有人把遮挡那版的 CLOSED 抄进来后
    -- 前端 PROCESS_STATUS_LABEL 查不到 key、页面渲染成空白。
    CONSTRAINT ck_belt_process_status CHECK (
        process_status IN ('PENDING', 'PROCESSING', 'DONE')
    ),
    CONSTRAINT ck_belt_conclusion_type CHECK (
        conclusion_type IN ('STANDING_UNDER', 'CROSSING', 'LOITERING', 'OTHER')
    )
);

COMMENT ON TABLE  ai_belt_event IS '皮带机下站人事件';
COMMENT ON COLUMN ai_belt_event.event_no             IS '事件编号，前缀 BC，如 BC202607290001';
COMMENT ON COLUMN ai_belt_event.point_id             IS '关联皮带机点位，指向 ai_belt_monitor_point.id（本期未建物理外键）';
COMMENT ON COLUMN ai_belt_event.device_name          IS '皮带机名称快照，列表「皮带机」列直接展示，不随点位改名而改写';
COMMENT ON COLUMN ai_belt_event.camera_code          IS '摄像头业务编码快照，列表「摄像头」列直接展示';
COMMENT ON COLUMN ai_belt_event.warehouse_id         IS '所属仓库，数据范围强制过滤维度';
COMMENT ON COLUMN ai_belt_event.conclusion_type      IS '结论类型 STANDING_UNDER/CROSSING/LOITERING/OTHER';
COMMENT ON COLUMN ai_belt_event.person_count         IS '画面内检出人数；算法未给出时为 NULL，不得伪造为 0';
COMMENT ON COLUMN ai_belt_event.algorithm_status     IS '算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK';
COMMENT ON COLUMN ai_belt_event.algorithm_result     IS '算法结论 NORMAL/ANOMALY/UNCERTAIN；未产出时为 NULL，不得伪造为 NORMAL';
COMMENT ON COLUMN ai_belt_event.process_status       IS '人工处理状态 PENDING 未核实 / PROCESSING 处理中 / DONE 已处理（三值，与遮挡那版不同）';
COMMENT ON COLUMN ai_belt_event.processed_by         IS '最近处理人，由后端按登录态写入，不接受前端传入';
COMMENT ON COLUMN ai_belt_event.event_version        IS '乐观锁版本号，提交处理时必须原样回传';
COMMENT ON COLUMN ai_belt_event.is_simulated         IS '是否模拟数据，必须显式标识，不得混入真实统计';

CREATE INDEX IF NOT EXISTS idx_belt_ev_occurred ON ai_belt_event (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_belt_ev_wh       ON ai_belt_event (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_belt_ev_region   ON ai_belt_event (region_id);
CREATE INDEX IF NOT EXISTS idx_belt_ev_status   ON ai_belt_event (process_status);
CREATE INDEX IF NOT EXISTS idx_belt_ev_point    ON ai_belt_event (point_id);


-- =====================================================================
-- T3  ai_belt_evidence  皮带机下站人证据
--
-- 只存受控对象存储引用（mock:// 或 s3://），不存任意 URL 或本地路径。
-- 短时地址由 MediaGateway 在详情接口签发。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_belt_evidence (
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
    CONSTRAINT pk_ai_belt_evidence PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_belt_evidence IS '皮带机下站人证据';
COMMENT ON COLUMN ai_belt_evidence.evidence_type  IS '证据类型 ANOMALY_FRAME/NORMAL_REFERENCE/ANNOTATED_FRAME/EVENT_VIDEO';
COMMENT ON COLUMN ai_belt_evidence.original_ref   IS '受控内部媒体引用，禁止存放任意 URL 或本地绝对路径';
COMMENT ON COLUMN ai_belt_evidence.annotated_ref  IS '算法标注图引用（把检出的人框画出来），与 original_ref 分离存储，不得互相覆盖';

CREATE INDEX IF NOT EXISTS idx_belt_evid_event ON ai_belt_evidence (event_id);


-- =====================================================================
-- T4  ai_belt_process_record  处理审计记录（追加式）
--
-- 只追加，永不 UPDATE / DELETE，保证「谁处理、为何这样处理」可追溯。
-- =====================================================================
CREATE TABLE IF NOT EXISTS ai_belt_process_record (
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
    CONSTRAINT pk_ai_belt_process_record PRIMARY KEY (id)
);

COMMENT ON TABLE  ai_belt_process_record IS '皮带机下站人事件处理审计记录（追加式）';
COMMENT ON COLUMN ai_belt_process_record.status_before  IS '变更前状态，用于还原处理链路';
COMMENT ON COLUMN ai_belt_process_record.process_opinion IS '处理意见；「查看即标记」的自动记录会明确标注，与真实人工结论区分';
COMMENT ON COLUMN ai_belt_process_record.source         IS '操作来源：WEB=真人处理，SYSTEM=查看即标记的自动写入。二者必须可区分，否则审计无法评估自动标记的影响面';

CREATE INDEX IF NOT EXISTS idx_belt_pr_event ON ai_belt_process_record (event_id);


-- ---------- 步骤 6：授权给应用运行账号 ----------
-- zhlc 只有 DML 权限，建表后必须逐表显式授权，否则应用读写报 permission denied。
-- 本期未建物理外键，只授本脚本新建的四张表。

GRANT SELECT, INSERT, UPDATE, DELETE ON ai_belt_monitor_point  TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_belt_event          TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_belt_evidence       TO zhlc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ai_belt_process_record TO zhlc;


-- ---------- 步骤 7：自检 ----------
-- 预期四张表全部存在，返回 4 行。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_belt_monitor_point',
      'ai_belt_event',
      'ai_belt_evidence',
      'ai_belt_process_record'
  )
ORDER BY table_name;

-- 预期列数：monitor_point 22 / event 32 / evidence 12 / process_record 11。
-- 少一列说明执行中断，多一列说明有人手工改过表，都要停下来核对。
SELECT table_name, count(*) AS column_count
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN (
      'ai_belt_monitor_point',
      'ai_belt_event',
      'ai_belt_evidence',
      'ai_belt_process_record'
  )
GROUP BY table_name
ORDER BY table_name;

-- 预期每张表 4 行 SELECT/INSERT/UPDATE/DELETE，grantee 均为 zhlc（共 16 行）。
-- 少于 16 行就是漏授了某张表，应用会在运行期才暴露成 500。
SELECT table_name, grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_name IN (
      'ai_belt_monitor_point',
      'ai_belt_event',
      'ai_belt_evidence',
      'ai_belt_process_record'
  )
  AND grantee = 'zhlc'
ORDER BY table_name, privilege_type;
