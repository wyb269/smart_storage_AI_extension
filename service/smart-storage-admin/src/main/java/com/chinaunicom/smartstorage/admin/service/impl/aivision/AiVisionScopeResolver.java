package com.chinaunicom.smartstorage.admin.service.impl.aivision;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.OrganizationMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 仓门异动数据范围解析器。
 *
 * <p><b>解决的问题（原报告 ⑥-3）</b>：此前 {@code warehouseIds} 从未被填充，
 * 导致 {@code AND warehouse_id IN (...)} 条件在 SQL 中整体失效，
 * 数据范围防护退化为「不拦截」。本类补齐授权来源。</p>
 *
 * <p><b>授权来源</b>：{@code base_organization_info} 的组织树。
 * 该表 {@code type} 字段定义为 {@code 1=公司、2=仓库}，因此
 * <b>用户所属组织节点及其全部后代中 {@code type=2} 的节点</b>，
 * 即为该用户的授权仓库集合。仓库用户得到自身节点，
 * 公司用户得到其下辖全部仓库。</p>
 *
 * <p><b>跨库边界（第 39.2、39.3 节）</b>：组织树属于<b>主库</b>
 * （{@code smart-storage-site}），异动数据属于独立库
 * （{@code smart_storage_ai_extension}），两者<b>禁止联表</b>。
 * 因此仓库集合必须在 Java 侧经主库 {@link OrganizationMapper} 解析，
 * 再以<b>参数</b>形式传入仓门库 SQL。本类不访问 {@code ai_*} 表。</p>
 *
 * <p><b>失败即拒绝</b>：登录态解析失败、{@code orgId} 为空、组织树查询异常、
 * 或子树内不存在 {@code type=2} 节点时，返回<b>空仓库集合</b>，
 * 由数据访问层转成恒假条件。绝不因解析失败而放宽为全量可见。</p>
 *
 * <p><b>缓存</b>：前端以 10~15 秒轮询仓门与事件，每次都跑递归 CTE
 * 会给主库带来无谓压力，因此按 {@code orgId} 做 60 秒短 TTL 缓存
 * （复用既有 hutool 依赖，不新增依赖）。授权变更后最多 60 秒生效。</p>
 *
 * <p><b>日志</b>：组织 ID、仓库 ID 属敏感业务标识，
 * 本类只打印数量，不打印标识值。</p>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Component
public class AiVisionScopeResolver {

    /** base_organization_info.type：2 = 仓库 */
    private static final int ORG_TYPE_WAREHOUSE = 2;

    /** base_user_info.userType：1 = 超级管理员 */
    private static final int USER_TYPE_SUPER_ADMIN = 1;

    /** 行政区划码 0 表示全域，语义与 BaseController.getValidRegionId 一致 */
    private static final String GLOBAL_REGION = "0";

    /** 授权仓库缓存时长，覆盖前端 10~15 秒轮询窗口 */
    private static final long GRANT_CACHE_MILLIS = 60_000L;

    @Resource
    private JwtTokenComponent jwtTokenComponent;

    @Resource
    private OrganizationMapper organizationMapper;

    /** key = 组织 ID，value = 该组织的授权仓库标识集合 */
    private final TimedCache<String, List<Long>> grantCache =
        CacheUtil.newTimedCache(GRANT_CACHE_MILLIS);

    /**
     * 依据当前登录态构造数据范围。
     *
     * <p>无法解析登录态时返回<b>拒绝全部数据</b>的匿名范围；
     * 读路径据此返回空集合，写路径由
     * {@code requireAuthenticatedScope} 直接拒绝。</p>
     */
    public AiVisionDataScope resolve() {
        User user;
        try {
            user = jwtTokenComponent.getUser();
        } catch (Exception e) {
            // 不打印异常堆栈或 token 内容，避免敏感信息进入日志
            log.warn("[DoorAnomaly] 无法解析登录态，按无数据权限处理");
            return AiVisionDataScope.denyAll();
        }
        if (user == null) {
            log.warn("[DoorAnomaly] 登录态为空，按无数据权限处理");
            return AiVisionDataScope.denyAll();
        }

        AiVisionDataScope scope = new AiVisionDataScope();
        scope.setAuthenticated(true);
        scope.setUserId(user.getId());
        scope.setUserName(user.getName());
        scope.setOrgId(user.getOrgId());
        scope.setRegionId(user.getRegionId());

        if (isGlobalAccess(user)) {
            scope.setUnrestricted(true);
            return scope;
        }
        scope.setUnrestricted(false);
        scope.setWarehouseIds(resolveWarehouseIds(user.getOrgId()));
        return scope;
    }

    /**
     * 判定全域数据权限。
     *
     * <p>两个信号任一命中即为全域：超级管理员（{@code userType=1}）、
     * 区划码为 {@code 0}。此前该判定被硬编码为 false，
     * 导致合法的全域管理员也拿不到数据。</p>
     */
    private boolean isGlobalAccess(User user) {
        if (user.getUserType() != null && user.getUserType() == USER_TYPE_SUPER_ADMIN) {
            return true;
        }
        return GLOBAL_REGION.equals(user.getRegionId());
    }

    /**
     * 解析指定组织及其后代中所有仓库节点的标识。
     *
     * <p>{@code listOrganizationChildById} 的递归基准条件为
     * {@code WHERE id = #{id}}，返回结果<b>包含入参节点自身</b>，
     * 因此仓库直属用户也能拿到自己的仓库。</p>
     */
    private List<Long> resolveWarehouseIds(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            log.warn("[DoorAnomaly] 用户未绑定组织，无仓库授权");
            return List.of();
        }

        List<Long> cached = grantCache.get(orgId);
        if (cached != null) {
            return cached;
        }

        List<Long> warehouseIds = new ArrayList<>();
        int unparsable = 0;
        try {
            List<Organization> subtree = queryOrgSubtreeUnpaged(orgId);
            if (subtree != null) {
                for (Organization org : subtree) {
                    if (org == null || !Integer.valueOf(ORG_TYPE_WAREHOUSE).equals(org.getType())) {
                        continue;
                    }
                    Long warehouseId = toWarehouseId(org.getId());
                    if (warehouseId == null) {
                        unparsable++;
                    } else if (!warehouseIds.contains(warehouseId)) {
                        warehouseIds.add(warehouseId);
                    }
                }
            }
        } catch (Exception e) {
            // 不打印 SQL 与堆栈细节，避免主库结构信息进入日志
            log.warn("[DoorAnomaly] 组织树解析失败，按无仓库授权处理");
            grantCache.put(orgId, List.of());
            return List.of();
        }

        if (unparsable > 0) {
            log.warn("[DoorAnomaly] 仓库组织标识无法映射为数值主键，已跳过 count={}", unparsable);
        }
        if (warehouseIds.isEmpty()) {
            log.warn("[DoorAnomaly] 组织子树内不存在仓库节点，该用户可见数据为空");
        }

        List<Long> immutable = List.copyOf(warehouseIds);
        grantCache.put(orgId, immutable);
        return immutable;
    }

    /**
     * 查询组织子树，并保证本次查询<b>不消费</b> PageHelper 的分页上下文。
     *
     * <p><b>为什么必须这么做</b>：PageHelper 把分页参数存在 ThreadLocal 里，
     * 由「之后第一条被拦截的 SQL」消费。而 {@code @QueryByPage} 切面
     * （{@code common/page/PageAcpect.java:67-71}）是在
     * {@code startPage()} <b>之后</b>才 {@code proceed()} 进入业务的。
     * 本方法恰好处在这两者之间，若不加保护就会出现两个互相叠加的故障：</p>
     * <ol>
     *   <li>组织树查询被当成待分页查询，结果按 {@code pageSize} 截断
     *       （历史页常传 10），<b>授权仓库集合被削短</b>，
     *       且这份残缺结果还会被缓存 60 秒；</li>
     *   <li>真正的事件历史查询此时已无分页上下文，
     *       <b>不带 LIMIT 全量返回</b>，分页总数退化为列表长度。</li>
     * </ol>
     *
     * <p>表现为「仓库多于 pageSize 的用户偶发看不到部分仓库、
     * 历史页翻页无效」，且只在缓存未命中时出现，极易被误判成偶发前端问题。</p>
     */
    private List<Organization> queryOrgSubtreeUnpaged(String orgId) {
        final Page<?> pending = PageMethod.getLocalPage();
        if (pending == null) {
            return organizationMapper.listOrganizationChildById(orgId);
        }

        // 暂存并清空，让组织树查询走无分页路径，随后原样交还给历史查询
        final int pageNum = pending.getPageNum();
        final int pageSize = pending.getPageSize();
        final boolean count = pending.isCount();
        PageMethod.clearPage();
        try {
            return organizationMapper.listOrganizationChildById(orgId);
        } finally {
            PageMethod.startPage(pageNum, pageSize, count);
        }
    }

    /**
     * 把主库的组织标识映射为 {@code ai_*.warehouse_id}（BIGINT）。
     *
     * <p>主库 {@code base_organization_info.id} 为 VARCHAR，
     * 异动库仓库外键为 BIGINT。非数值标识无法与该列比较，
     * 返回 null 由调用方计数并跳过，而不是静默放进 IN 列表
     * 触发 {@code bigint = varchar} 的运算符错误。</p>
     */
    private Long toWarehouseId(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(organizationId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
