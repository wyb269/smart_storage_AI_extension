-- =====================================================================
-- 仓门异动监测 · 仓库 ID 对齐脚本
--
-- 【目标库】smart_storage_ai_extension（本功能独占库，允许写）
-- 【前置库】主库 base_organization_info（只读查询，不在本脚本内执行）
-- 【本交付未执行】原因见下方「当前阻塞」。
--
-- ---------------------------------------------------------------------
-- 对齐规则
-- ---------------------------------------------------------------------
-- ai_door_config / ai_algorithm_task / ai_anomaly_event 的 warehouse_id
-- 必须等于主库 base_organization_info 中某个 type=2（仓库）节点的 id。
--
-- 数据范围下推（DoorAnomalyScopeResolver）取的是
-- 「当前用户所属组织节点 + 其全部后代」里 type=2 的节点 id，
-- 再与 warehouse_id 比对。两边不同源时【不报错，只是查不到数据】：
-- 非全域用户看到空列表，全域用户（userType=1 或 regionId=0）仍看到全部。
-- 这是最容易被误判成「接口坏了」的一类故障。
--
-- 类型约束：warehouse_id 是 BIGINT，而 base_organization_info.id 是 VARCHAR。
-- 只有【纯数字】的组织 ID 才能落进这张表。非数字 ID 会被
-- DoorAnomalyScopeResolver.toWarehouseId() 跳过并计数告警。
-- 现网实测组织 ID 示例：0、69440756366229552、197976620592889895
-- —— 均为纯数字，可用；但 69440756366229552 已接近 bigint 上界的量级，
-- 新分配 ID 时请确认仍在 bigint 范围内。
--
-- ---------------------------------------------------------------------
-- 当前阻塞（2026-10-22 实测）
-- ---------------------------------------------------------------------
-- 通过后端自身接口（未直连主库）核查：
--   POST /rbac/organization/listOrganizationByConditionOnPage  -> total = 3
--   POST /rbac/organization/listOrganizationChildById/0        -> 3 行
-- 三行全部 type=1（公司）：0=总部公司、69440756366229552=齐齐哈尔、
-- 197976620592889895=123。type=2（仓库）节点数为【0】。
--
-- 所以现在没有任何真实仓库组织 ID 可以对齐。必须先由平台侧在主库
-- 建立仓库组织（type=2），本脚本才有输入。
-- 在此状态下，任何非全域用户都必然查不到仓门数据 —— 与 warehouse_id
-- 是否对齐无关，因为授权集合本身就是空集（失败即拒绝）。
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 第 1 步：先做映射临时表，把 <新仓库组织ID> 与种子库里的旧 ID 一一对上。
--         旧 ID 9001~9004 是建表种子数据里的占位值。
--         执行前请删除用不到的行。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS tmp_warehouse_id_mapping;
CREATE TEMP TABLE tmp_warehouse_id_mapping (
    old_id  BIGINT       NOT NULL,
    new_id  BIGINT       NOT NULL,
    org_name VARCHAR(128) NOT NULL
);

INSERT INTO tmp_warehouse_id_mapping (old_id, new_id, org_name) VALUES
    (9001, 0, '占位-请替换为真实 type=2 组织ID'),
    (9002, 0, '占位-请替换为真实 type=2 组织ID'),
    (9003, 0, '占位-请替换为真实 type=2 组织ID'),
    (9004, 0, '占位-请替换为真实 type=2 组织ID');

-- ---------------------------------------------------------------------
-- 第 2 步：护栏。new_id 为 0（未替换占位）或出现重复，直接中止，
--         避免把 warehouse_id 批量写成非法值。
-- ---------------------------------------------------------------------
DO $$
DECLARE
    bad_count  INTEGER;
    dup_count  INTEGER;
BEGIN
    SELECT count(*) INTO bad_count
      FROM tmp_warehouse_id_mapping
     WHERE new_id IS NULL OR new_id <= 0;
    IF bad_count > 0 THEN
        RAISE EXCEPTION '仍有 % 行 new_id 未替换为真实仓库组织ID，拒绝执行', bad_count;
    END IF;

    SELECT count(*) INTO dup_count
      FROM (SELECT new_id FROM tmp_warehouse_id_mapping GROUP BY new_id HAVING count(*) > 1) d;
    IF dup_count > 0 THEN
        RAISE EXCEPTION 'new_id 存在 % 组重复，多个旧仓库不能并入同一个组织ID', dup_count;
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 第 3 步：按外键依赖顺序更新。三张表必须在同一事务内完成，
--         否则会出现事件与仓门归属不一致的中间态。
-- ---------------------------------------------------------------------
UPDATE ai_anomaly_event e
   SET warehouse_id = m.new_id
  FROM tmp_warehouse_id_mapping m
 WHERE e.warehouse_id = m.old_id;

UPDATE ai_algorithm_task t
   SET warehouse_id = m.new_id
  FROM tmp_warehouse_id_mapping m
 WHERE t.warehouse_id = m.old_id;

UPDATE ai_door_config dc
   SET warehouse_id = m.new_id,
       warehouse_name = m.org_name
  FROM tmp_warehouse_id_mapping m
 WHERE dc.warehouse_id = m.old_id;

-- ai_anomaly_event 也冗余存了 warehouse_name，保持一致，
-- 否则列表与导出的仓库名称会与仓门配置不一致。
UPDATE ai_anomaly_event e
   SET warehouse_name = m.org_name
  FROM tmp_warehouse_id_mapping m
 WHERE e.warehouse_id = m.new_id;

COMMIT;

-- =====================================================================
-- 第 4 步：核对（在主库和仓门库分别执行）
--
-- 主库（只读）—— 列出可用于对齐的仓库组织：
--   select id, name, parent_id, levels, type
--   from base_organization_info
--   where type = 2
--   order by id;
--
-- 仓门库 —— 确认不再有遗留占位 ID：
--   select distinct warehouse_id from ai_door_config
--   union select distinct warehouse_id from ai_anomaly_event
--   union select distinct warehouse_id from ai_algorithm_task
--   order by 1;
--   -- 结果里不应再出现 9001~9004
--
-- 仓门库 —— 三张表归属一致性：
--   select count(*) from ai_anomaly_event e
--     left join ai_door_config d on d.id = e.door_id
--    where d.id is null or e.warehouse_id is distinct from d.warehouse_id;
--   -- 期望 0
--
-- 第 5 步：用一个【非全域】、所属组织子树内含 type=2 仓库的账号登录，
--         确认 /doorAnomaly/listEventByCondition 返回该仓库的事件。
--         这一步是本次交付唯一没能覆盖的实测项（缺第二个组织的账号）。
-- =====================================================================
