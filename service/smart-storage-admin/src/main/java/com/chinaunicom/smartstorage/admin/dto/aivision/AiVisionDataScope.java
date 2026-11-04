package com.chinaunicom.smartstorage.admin.dto.aivision;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 仓门异动数据范围对象。
 *
 * <p>依据 AI_HANDOFF.md 第 28 节「权限模型」与第 31 节边界场景：
 * 权限必须同时实施在后端接口、前端按钮可见性、视频与证据短时地址签发，
 * 以及<b>仓库、库区、仓房和摄像头数据范围</b>。</p>
 *
 * <p><b>本对象解决的 P0-6 问题</b>：此前所有查询向 Repository 传入
 * {@code null, null}（空组织/区域条件），历史、详情、处理与导出也未
 * 统一注入并校验当前用户的数据范围，存在跨组织读取与越权处理风险。</p>
 *
 * <p><b>范围维度以仓库为唯一强制条件（第 28 节）</b>：</p>
 * <ul>
 *   <li>{@code unrestricted = true}：全域数据权限，不做仓库过滤；</li>
 *   <li>{@code unrestricted = false} 且 {@code warehouseIds} 非空：
 *       仅可见 {@code warehouse_id IN warehouseIds} 的数据；</li>
 *   <li>{@code unrestricted = false} 且 {@code warehouseIds} 为空：<b>不可见任何数据</b>。
 *       即「未解析出授权仓库」按拒绝处理，而非按不限制处理。</li>
 * </ul>
 *
 * <p><b>为什么不把 orgId / regionId 也下推为 AND 条件</b>：
 * 仓库授权集合已经由 {@code orgId} 的组织子树推导得出（见
 * {@code AiVisionScopeResolver}），再叠加 {@code org_id} 等值条件
 * 属于重复限制；且 {@code ai_anomaly_event.org_id} 允许为空，
 * 等值比较会让合法用户查询恒返回 0 行，形成难以排查的空数据故障。</p>
 *
 * <p><b>使用规则</b>：</p>
 * <ul>
 *   <li>本对象<b>只能由后端从登录态构造</b>，不接受前端传入
 *       （与第 20.6 节「不信任前端传入处理人身份」一致）；</li>
 *   <li>仓门、视频、最近事件、历史、详情、处理与导出<b>全部</b>必须携带本对象；</li>
 *   <li>详情与写操作<b>必须</b>在查询条件中带仓库约束，
 *       不能只按事件 ID 查询；</li>
 * </ul>
 *
 * <p><b>类型约定</b>：{@code warehouseIds} 使用 {@code Long}，
 * 与 {@code ai_*.warehouse_id BIGINT} 对齐。若以字符串绑定，
 * PostgreSQL 会报 {@code bigint = character varying} 无匹配运算符。</p>
 *
 * <p><b>安全要求</b>：本对象不得包含密码、Token 或连接信息；
 * 不得写入日志正文（组织 ID 与仓库 ID 属敏感业务标识）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "仓门异动数据范围（仅后端构造，不接受前端传入）")
public class AiVisionDataScope {

    /** 行政区划码；仅用于判定全域权限与审计，不下推为 SQL 条件 */
    private String regionId;

    /** 组织机构标识；用于推导授权仓库集合，不下推为 SQL 条件 */
    private String orgId;

    /**
     * 授权仓库标识集合。
     *
     * <p>事件与仓门均挂在仓库维度上（第 27 节逻辑模型），
     * 因此仓库是本功能的<b>唯一强制过滤维度</b>。</p>
     */
    private List<Long> warehouseIds;

    /** 当前用户标识，仅用于审计，不参与数据过滤 */
    private String userId;

    /** 当前用户展示名，仅用于处理记录写入 */
    private String userName;

    /**
     * 是否具备全域数据权限。
     *
     * <p>true 时跳过数据范围过滤；false 时按 {@code warehouseIds} 过滤。</p>
     */
    private boolean unrestricted;

    /**
     * 是否为已认证用户。
     *
     * <p>读请求可按既定匿名策略处理；<b>写操作必须要求已认证</b>，
     * 无法解析认证用户时直接拒绝（第 29.4 节、P1-8）。</p>
     */
    private boolean authenticated;

    /**
     * 判断给定仓库是否在授权范围内。
     *
     * <p>语义为<b>失败即拒绝</b>：非全域且仓库集合为空时一律拒绝，
     * 目标仓库为空同样拒绝（无法证明归属的数据不放行）。</p>
     *
     * @param targetWarehouseId 目标仓库标识
     * @return true 表示允许访问
     */
    public boolean allowsWarehouse(Long targetWarehouseId) {
        if (unrestricted) {
            return true;
        }
        if (targetWarehouseId == null || warehouseIds == null || warehouseIds.isEmpty()) {
            return false;
        }
        return warehouseIds.contains(targetWarehouseId);
    }

    /**
     * 构造拒绝所有数据的范围（匿名或登录态解析失败时使用）。
     *
     * <p>匿名用户不具备全域权限，也不具备任何仓库的访问权；
     * 写操作必须通过 {@link #isAuthenticated()} 拒绝。</p>
     */
    public static AiVisionDataScope denyAll() {
        AiVisionDataScope scope = new AiVisionDataScope();
        scope.setAuthenticated(false);
        scope.setUnrestricted(false);
        scope.setWarehouseIds(List.of());
        return scope;
    }
}
