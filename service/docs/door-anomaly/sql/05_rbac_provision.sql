-- =====================================================================
-- 仓门异动监测 · 主库 RBAC 授权脚本
--
-- 【目标库】主库（base_menu_info / base_rights_info 等所在库），
--          不是 smart_storage_ai_extension。
-- 【执行方】平台管理员 / DBA。
-- 【本交付未执行】开发约束明确禁止本次交付触碰主库，
--          因此本脚本【从未运行过】，属于待执行项，不是已验证项。
--          执行前请在测试库演练并逐条核对 ID。
--
-- ---------------------------------------------------------------------
-- 为什么必须执行它
-- ---------------------------------------------------------------------
-- 本项目 Spring Security 的 authority 是 base_rights_info.id 的字符串形式
-- （见 TokenUserDetailsServiceImpl：new SimpleGrantedAuthority(right.getId())），
-- 没有权限码列。后端已按数字 ID 接入校验
-- （DoorAnomalyPermissionChecker + door-anomaly.permission.rights），
-- 但目标库里还没有对应权限行，所以 door-anomaly.permission.enabled 目前只能保持 false。
-- 执行本脚本、拿到实际 ID 后注入环境变量，功能权限才真正生效。
--
-- ---------------------------------------------------------------------
-- 重要限制：授权粒度是「菜单」，不是「权限」
-- ---------------------------------------------------------------------
-- 登录取权限的 SQL（RightsMapper.listRightsByRoleGroupId）是：
--   base_role_group_link -> base_role_info -> base_role_menu(menu_id)
--     -> base_menu_rights(menu_id, rights_id) -> base_rights_info
-- 它按 menu_id 取出该菜单【全部】权限，不区分角色。
-- 因此：把「查看」和「导出」挂在同一个菜单上，就【无法】只授予其中一个。
-- 若将来需要按角色拆分，必须把导出单独挂到一个 is_display=2（不显示）的
-- 子菜单行上，或者改造该查询。本脚本按「历史页 = 查看 + 导出」的口径编写。
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. 权限行（id 由业务侧手工分配，非自增；请先确认下列 ID 未被占用）
--    命名沿用现网风格：现网实测为「添加 / 修改 / 删除」这类短动作名。
-- ---------------------------------------------------------------------
INSERT INTO base_rights_info (id, name) VALUES
    (90101, '查看'),
    (90102, '导出'),
    (90103, '处理');
-- 若 901xx 段与现网冲突，改用其他空闲段，并同步修改本文件末尾的核对查询
-- 以及环境变量 AI_DOOR_RIGHT_VIEW / AI_DOOR_RIGHT_EXPORT / AI_DOOR_RIGHT_PROCESS。

-- ---------------------------------------------------------------------
-- 2. 菜单行
--    parent_id = 0 表示一级菜单；type = 1 为菜单节点。
--    open_url 必须与前端路由保持一致：
--      前端静态路由为 /aiVision/doorAnomaly/{monitor,history}
--      （manage/src/router/modules/aiVision.ts）
--    注意：一级菜单的 open_url 会被前端拼成 '/' + open_url 作为 path，
--    二级菜单会被拼成 parentUrl + '/' + open_url + '/index' 作为组件路径，
--    因此二级 open_url 的取值必须与 src/pages 下的目录结构对得上，
--    否则动态路由会解析不到组件。本页当前由静态路由直接注册可用，
--    菜单行仅用于侧边导航展示与权限挂载。
-- ---------------------------------------------------------------------
INSERT INTO base_menu_info (id, parent_id, name, open_url, icon, is_frame, order_num, is_display, type)
VALUES (9010, 0, '视觉AI监测', 'aiVision', 'video-camera-2', 2, 90, 1, 1);

INSERT INTO base_menu_info (id, parent_id, name, open_url, icon, is_frame, order_num, is_display, type)
VALUES (9011, 9010, '仓门异动监测', 'doorAnomaly/monitor', '', 2, 1, 1, 1),
       (9012, 9010, '仓门异动历史记录', 'doorAnomaly/history', '', 2, 2, 1, 1);

-- ---------------------------------------------------------------------
-- 3. 菜单 <-> 权限
-- ---------------------------------------------------------------------
INSERT INTO base_menu_rights (menu_id, rights_id) VALUES
    (9011, 90101),   -- 监测页：查看
    (9012, 90101),   -- 历史页：查看
    (9012, 90102);   -- 历史页：导出（与查看同菜单，按现网授权粒度不可再拆分）

-- 90103「处理」刻意【不绑定到任何菜单】：
-- 产品口径已取消人工处理入口（查看即标记已处理），
-- 后端 /processEvent 接口保留但默认无人可用。将来要恢复人工处理时，
-- 把它绑定到相应菜单即可。

-- ---------------------------------------------------------------------
-- 4. 角色 <-> 菜单（这一步才真正授权）
--    把下面的角色 ID 换成目标环境实际承担「粮库值班/监控」职责的角色。
--    查询可用角色： select id, name from base_role_info order by id;
-- ---------------------------------------------------------------------
-- INSERT INTO base_role_menu (role_id, menu_id) VALUES
--     (<角色ID>, 9010),
--     (<角色ID>, 9011),
--     (<角色ID>, 9012);

-- ---------------------------------------------------------------------
-- 5. 可选：若使用角色管理界面按「菜单+权限」细粒度勾选，
--    该界面读写的是 base_menu_rights_link(rights_id, menu_id, role_id)。
--    登录鉴权不读这张表，所以不填也不影响授权生效。
-- ---------------------------------------------------------------------

COMMIT;

-- =====================================================================
-- 执行后核对：应返回 3 行（90101 查看 / 90102 导出 / 90103 处理）
--   select brin.id, brin.name
--   from base_menu_rights bmr
--   join base_rights_info brin on brin.id = bmr.rights_id
--   where bmr.menu_id in (9011, 9012)
--   group by brin.id, brin.name order by brin.id;
--
-- 再把上面查到的实际 ID 注入环境变量并重启后端：
--   export AI_DOOR_PERMISSION_ENABLED='true'
--   export AI_DOOR_RIGHT_VIEW='90101'
--   export AI_DOOR_RIGHT_EXPORT='90102'
--   export AI_DOOR_RIGHT_PROCESS='90103'
--
-- 最后用一个【未】绑定上述菜单的普通账号验证：
--   调 /doorAnomaly/listDoorStatus 与 /doorAnomaly/exportEvent
--   均应返回 status.code=403「没有访问权限」，
--   后端日志出现「[DoorAnomaly] 越权拒绝 operation=... requiredRightId=...」。
-- =====================================================================
