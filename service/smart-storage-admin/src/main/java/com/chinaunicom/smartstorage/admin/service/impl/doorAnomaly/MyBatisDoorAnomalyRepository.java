package com.chinaunicom.smartstorage.admin.service.impl.doorAnomaly;

import cn.hutool.core.lang.Snowflake;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventProcessReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiAlgorithmTask;
import com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly.AiAlgorithmTaskMapper;
import com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly.AiAnomalyEventMapper;
import com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly.AiEventEvidenceMapper;
import com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly.AiEventProcessRecordMapper;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.DoorAnomalyRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的仓门异动数据访问实现。
 *
 * <p>目标库：{@code smart_storage_ai_extension}。</p>
 *
 * <p><b>分层要求（第 23 节）</b>：Controller → Service → Mapper，
 * 本类只负责数据访问，不做业务判断。</p>
 *
 * <p><b>并发控制（第 20.6 节）</b>：</p>
 * <ol>
 *   <li>人工处理使用 SQL 层乐观锁 {@code WHERE event_version = ?}，
 *       版本不匹配时更新 0 行，此处返回 0 交由 Service 判定。</li>
 *   <li>更新与追加处理记录在同一事务中，保证一致性。</li>
 * </ol>
 *
 * @author 仓门异动功能
 */
@Slf4j
@org.springframework.stereotype.Repository
@ConditionalOnProperty(prefix = "ai-vision.datasource", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class MyBatisDoorAnomalyRepository implements DoorAnomalyRepository {

    @Resource
    private AiAnomalyEventMapper eventMapper;

    @Resource
    private AiEventEvidenceMapper evidenceMapper;

    @Resource
    private AiEventProcessRecordMapper recordMapper;

    /** 算法任务 Mapper（仅数据源启用时随本类一同注册） */
    @Resource
    private AiAlgorithmTaskMapper algorithmTaskMapper;

    /** 雪花 ID 生成器，复用项目已有的 hutool 依赖，不新增任何依赖 */
    private Snowflake snowflake;

    /** 雪花 ID 机器号，来自配置（缺失时使用默认值） */
    @Value("${snowflake.worker-id:1}")
    private long workerId;

    @PostConstruct
    public void initSnowflake() {
        this.snowflake = new Snowflake(workerId, 1L);
    }

    /* ==================== 查询 ==================== */

    @Override
    public List<DoorStatusRespDTO> listDoorStatus(AiVisionDataScope scope) {
        // P0-6：数据范围下推到 SQL，避免全表返回后再过滤
        return eventMapper.listDoorStatus(unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<AnomalyEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return eventMapper.listRecentEvent(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<AnomalyEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                          EventQueryReqDTO query) {
        EventQueryReqDTO scoped = applyScope(query, scope);
        return eventMapper.listEventByCondition(scoped);
    }

    @Override
    public List<AnomalyEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                        EventQueryReqDTO query, int limit) {
        EventQueryReqDTO scoped = applyScope(query, scope);
        return eventMapper.listEventForExport(scoped, limit);
    }

    /** 全域标记；scope 缺失按「无权限」处理，不得当作全域 */
    private boolean unrestricted(AiVisionDataScope scope) {
        return scope != null && scope.isUnrestricted();
    }

    /** 授权仓库集合；scope 缺失返回空集合，由 Mapper 转成恒假条件 */
    private List<Long> warehouseIds(AiVisionDataScope scope) {
        if (scope == null || scope.getWarehouseIds() == null) {
            return List.of();
        }
        return scope.getWarehouseIds();
    }

    /**
     * 把数据范围写入查询条件。
     *
     * <p>P0-6：使 SQL 的 WHERE 必然带上仓库约束。
     * 调用方显式指定的 {@code warehouseId} 业务筛选条件<b>不被覆盖</b>，
     * 范围条件以 AND 叠加，因此伪造仓库标识最多缩小结果，不会放大。</p>
     */
    private EventQueryReqDTO applyScope(EventQueryReqDTO query, AiVisionDataScope scope) {
        EventQueryReqDTO target = query == null ? new EventQueryReqDTO() : query;
        target.setScopeUnrestricted(unrestricted(scope));
        target.setScopeWarehouseIds(warehouseIds(scope));
        return target;
    }

    @Override
    public EventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        Long id;
        try {
            id = Long.parseLong(eventId);
        } catch (NumberFormatException e) {
            log.warn("[DoorAnomalyRepo] 非法事件标识格式");
            return null;
        }

        // P0-6：仓库范围直接在 SQL 中生效，越权与不存在一律返回 null，
        // 不向外泄露对象是否存在（第31节）。
        // 详情字段由 getEventDetailInScope 一次性带出（含算法任务关联），
        // 不再手工逐字段拷贝——原先那段拷贝正是「仅详情字段恒为 null」的根因。
        EventDetailRespDTO detail =
            eventMapper.getEventDetailInScope(id, unrestricted(scope), warehouseIds(scope));
        if (detail == null) {
            log.warn("[DoorAnomalyRepo] 事件超出当前用户数据范围或不存在，已拒绝访问");
            return null;
        }

        // 证据：仅返回对象存储键，短时地址由 Service 经 MediaGateway 签发
        List<EventEvidenceRespDTO> evidences = evidenceMapper.listEvidenceByEventId(id);
        detail.setEvidences(evidences == null ? new ArrayList<>() : evidences);

        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        try {
            Long id = Long.parseLong(eventId);
            // 处理记录随事件一起受数据范围保护（第 28、29.4 节）。
            // 不能只依赖调用方先查过详情：接口签名已要求传入 scope，
            // 这里必须自己兑现，否则将来任何新调用点都会静默泄露审计内容。
            AnomalyEventRespDTO event =
                eventMapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
            if (event == null) {
                return List.of();
            }
            List<ProcessRecordRespDTO> list = recordMapper.listProcessRecords(id);
            return list == null ? List.of() : list;
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    /* ==================== 人工处理 ==================== */

    /**
     * 提交人工处理：乐观锁更新 + 追加审计记录，二者处于<b>同一事务</b>（P1-7）。
     *
     * <p>事务管理器显式写本功能独立库的 {@code aiVisionTransactionManager}：
     * 只有在这里（而不是 Service 上）声明才安全——该 Bean 仅在
     * {@code ai-vision.datasource.enabled=true} 时存在，本类同样带该条件注解，
     * 二者生命周期一致。若在 Service 上按名声明，内存兜底模式下会因
     * Bean 缺失而在首次调用抛 {@code NoSuchBeanDefinitionException}。</p>
     */
    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, EventProcessReqDTO req, String processedBy) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }
        Long id;
        try {
            id = Long.parseLong(eventId);
        } catch (NumberFormatException e) {
            return 0;
        }

        // P0-6：写操作的范围校验下推到 SQL
        AnomalyEventRespDTO existing =
            eventMapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (existing == null) {
            log.warn("[DoorAnomalyRepo] 处理请求超出数据范围或事件不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        int affected = eventMapper.updateProcessWithVersion(
            id, eventVersion, req.getProcessResult(), processedBy,
            req.getProcessOpinion(), now);

        if (affected == 0) {
            log.info("[DoorAnomalyRepo] 事件处理未生效（版本冲突或状态不允许）eventId={} version={}",
                eventId, eventVersion);
            // 未更新状态就绝不写审计记录，否则审计与状态互相矛盾
            return 0;
        }

        // 追加式写入，不覆盖历史（第 17.6 节）；与上面的更新同事务，失败一起回滚
        recordMapper.appendProcessRecord(
            snowflake.nextId(),
            id,
            existing.getEventNo(),
            statusBefore,
            req.getProcessResult(),
            req.getProcessResult(),
            req.getProcessOpinion(),
            processedBy,
            now,
            "WEB",
            now);

        return affected;
    }

    /* ==================== 算法任务（契约 §1） ==================== */

    @Override
    public void insertAlgorithmTask(AiAlgorithmTask task) {
        // 主键由应用侧发号（雪花），与其余 aivision 表一致，避免多数据源序列不同步
        if (task.getId() == null) {
            task.setId(snowflake.nextId());
        }
        final LocalDateTime now = LocalDateTime.now();
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(now);
        }
        if (task.getUpdatedAt() == null) {
            task.setUpdatedAt(now);
        }
        if (task.getRetryCount() == null) {
            task.setRetryCount(0);
        }
        // 铁律 §0.1：算法结果一律模拟，is_simulated 恒为 TRUE
        task.setIsSimulated(Boolean.TRUE);
        algorithmTaskMapper.insertTask(task);
    }

    @Override
    public AiAlgorithmTask getAlgorithmTaskByTaskNo(String taskNo, AiVisionDataScope scope) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        return algorithmTaskMapper.selectByTaskNoInScope(taskNo, unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<AiAlgorithmTask> listAlgorithmTasksByEvent(String eventId,
                                                           AiVisionDataScope scope, int limit) {
        Long id = parseEventId(eventId);
        if (id == null) {
            return List.of();
        }
        List<AiAlgorithmTask> list =
            algorithmTaskMapper.selectByEventIdInScope(id, unrestricted(scope), warehouseIds(scope),
                Math.max(limit, 0));
        return list == null ? List.of() : list;
    }

    @Override
    public void updateAlgorithmTask(AiAlgorithmTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        algorithmTaskMapper.updateTaskResult(task);
    }

    @Override
    public int writebackEventAlgorithm(AiVisionDataScope scope, String eventId, Long algorithmTaskId,
                                       String algorithmStatus, String algorithmResult,
                                       BigDecimal confidence, String anomalyType, String updatedBy) {
        Long id = parseEventId(eventId);
        if (id == null) {
            return 0;
        }
        return eventMapper.updateAlgorithmFields(
            id, algorithmTaskId, algorithmStatus, algorithmResult,
            confidence, anomalyType, updatedBy, LocalDateTime.now(),
            unrestricted(scope), warehouseIds(scope));
    }

    /**
     * 事件标识解析为库内 BIGINT；非数值按「不存在」处理，返回 null。
     */
    private Long parseEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(eventId.trim());
        } catch (NumberFormatException e) {
            log.warn("[DoorAnomalyRepo] 非法事件标识格式，忽略算法任务操作");
            return null;
        }
    }
}