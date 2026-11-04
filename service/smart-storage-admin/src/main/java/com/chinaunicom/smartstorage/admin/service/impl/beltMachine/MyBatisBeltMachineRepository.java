package com.chinaunicom.smartstorage.admin.service.impl.beltMachine;

import cn.hutool.core.lang.Snowflake;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.beltMachine.BeltMachineAlgorithmTask;
import com.chinaunicom.smartstorage.admin.mapper.aivision.beltMachine.BeltMachineMapper;
import com.chinaunicom.smartstorage.admin.service.inter.beltMachine.BeltMachineRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的皮带机下站人数据访问实现。
 *
 * <p>目标库：{@code smart_storage_ai_extension}。本类只负责数据访问，不做业务判断。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "ai-vision.datasource", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class MyBatisBeltMachineRepository implements BeltMachineRepository {

    @Resource
    private BeltMachineMapper mapper;

    /** 雪花 ID 生成器，复用项目已有的 hutool 依赖 */
    private Snowflake snowflake;

    @Value("${snowflake.worker-id:1}")
    private long workerId;

    @PostConstruct
    public void initSnowflake() {
        this.snowflake = new Snowflake(workerId, 1L);
    }

    /* ==================== 查询 ==================== */

    @Override
    public List<BeltMachineMonitorPointRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        return mapper.listMonitorPoint(unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<BeltMachineEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return mapper.listRecentEvent(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<BeltMachineEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                              BeltMachineEventQueryReqDTO query) {
        return mapper.listEventByCondition(applyScope(query, scope));
    }

    @Override
    public List<BeltMachineEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                            BeltMachineEventQueryReqDTO query, int limit) {
        return mapper.listEventForExport(applyScope(query, scope), limit);
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
     * <p>调用方显式指定的 {@code warehouseId} 业务筛选条件不被覆盖，
     * 范围条件以 AND 叠加，因此伪造仓库标识最多缩小结果，不会放大。</p>
     */
    private BeltMachineEventQueryReqDTO applyScope(BeltMachineEventQueryReqDTO query,
                                                   AiVisionDataScope scope) {
        BeltMachineEventQueryReqDTO target = query == null
            ? new BeltMachineEventQueryReqDTO() : query;
        target.setScopeUnrestricted(unrestricted(scope));
        target.setScopeWarehouseIds(warehouseIds(scope));
        return target;
    }

    @Override
    public BeltMachineEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return null;
        }

        // 越权与不存在一律返回 null，不区分两种情况以避免泄露对象存在性
        BeltMachineEventDetailRespDTO detail =
            mapper.getEventDetailInScope(id, unrestricted(scope), warehouseIds(scope));
        if (detail == null) {
            log.warn("[BeltMachine] 事件超出当前用户数据范围或不存在，已拒绝访问");
            return null;
        }

        // 证据：仅返回受控引用，换发短时地址由 Service 经 MediaGateway 处理
        List<BeltMachineEvidenceRespDTO> evidences = mapper.listEvidenceByEventId(id);
        detail.setEvidences(evidences == null ? new ArrayList<>() : evidences);

        return detail;
    }

    @Override
    public List<BeltMachineProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        // 处理记录随事件一起受数据范围保护。不能只依赖调用方先查过详情：
        // 接口签名已要求传入 scope，这里必须自己兑现，
        // 否则将来任何新调用点都会静默泄露审计内容。
        BeltMachineEventRespDTO event =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (event == null) {
            return List.of();
        }
        List<BeltMachineProcessRecordRespDTO> list = mapper.listProcessRecords(id);
        return list == null ? List.of() : list;
    }

    /* ==================== 人工处理 ==================== */

    /**
     * 提交人工处理：乐观锁更新 + 追加审计记录，二者处于同一事务。
     *
     * <p>事务管理器显式写共享独立库的 {@code aiVisionTransactionManager}：
     * 只有在这里（而不是 Service 上）声明才安全——该 Bean 仅在
     * {@code ai-vision.datasource.enabled=true} 时存在，本类同样带该条件注解，
     * 二者生命周期一致。</p>
     */
    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String processResult, String processOpinion,
                            String processedBy, String source) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }
        Long id = parseId(eventId);
        if (id == null) {
            return 0;
        }

        // 写操作的范围校验下推到 SQL
        BeltMachineEventRespDTO existing =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (existing == null) {
            log.warn("[BeltMachine] 处理请求超出数据范围或事件不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        int affected = mapper.updateProcessWithVersion(
            id, eventVersion, processResult, processedBy, processOpinion, now);

        if (affected == 0) {
            log.info("[BeltMachine] 事件处理未生效（版本冲突或状态不允许）eventId={} version={}",
                eventId, eventVersion);
            // 未更新状态就绝不写审计记录，否则审计与状态互相矛盾
            return 0;
        }

        // 追加式写入，不覆盖历史；与上面的更新同事务，失败一起回滚
        mapper.appendProcessRecord(
            snowflake.nextId(), id, existing.getEventNo(),
            statusBefore, processResult, processResult, processOpinion,
            processedBy, now, source, now);

        return affected;
    }

    /**
     * 事件标识转数值。非法格式返回 null 由调用方按「不存在」处理，
     * 不让 NumberFormatException 冒到接口层变成 500。
     */
    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("[BeltMachine] 非法事件标识格式");
            return null;
        }
    }

    /* ==================== 算法任务（重新识别链路） ==================== */

    @Override
    public long insertAlgorithmTask(BeltMachineAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(snowflake.nextId());
        }
        final LocalDateTime now = LocalDateTime.now();
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(now);
        }
        task.setUpdatedAt(now);
        mapper.insertAlgorithmTask(task);
        return task.getId();
    }

    @Override
    public BeltMachineAlgorithmTask getAlgorithmTaskByNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        return mapper.getAlgorithmTaskByNoInScope(taskNo, unrestricted(scope), warehouseIds(scope));
    }

    /**
     * 更新任务执行结果。
     *
     * <p>调用方（Service 轮询链路）必须先经 {@link #getAlgorithmTaskByNo}
     * 拿到带范围内的行，这里只按 id 定点更新。</p>
     */
    @Override
    public int updateAlgorithmTask(BeltMachineAlgorithmTask task) {
        if (task == null || task.getId() == null) {
            return 0;
        }
        task.setUpdatedAt(LocalDateTime.now());
        return mapper.updateAlgorithmTaskResult(task);
    }

    @Override
    public List<BeltMachineAlgorithmTask> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        // 与处理记录同一约定：列表接口自己兑现范围校验，不依赖调用方先查过详情
        BeltMachineEventRespDTO event =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (event == null) {
            return List.of();
        }
        List<BeltMachineAlgorithmTask> list = mapper.listAlgorithmTasksByEventId(id, Math.max(1, limit));
        return list == null ? List.of() : list;
    }

    @Override
    public int writeBackEventAlgorithm(AiVisionDataScope scope, String eventId, Long taskId,
                                       String algorithmStatus, String algorithmResult,
                                       String conclusionType, BigDecimal confidence, String updatedBy) {
        Long id = parseId(eventId);
        if (id == null || taskId == null) {
            return 0;
        }
        return mapper.updateEventAlgorithmWriteback(id, taskId, algorithmStatus, algorithmResult,
            conclusionType, confidence, unrestricted(scope), warehouseIds(scope),
            updatedBy, LocalDateTime.now());
    }
}
