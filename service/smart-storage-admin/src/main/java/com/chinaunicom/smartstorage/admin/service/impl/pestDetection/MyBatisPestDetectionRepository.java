package com.chinaunicom.smartstorage.admin.service.impl.pestDetection;

import cn.hutool.core.lang.Snowflake;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmWriteBackDTO;
import com.chinaunicom.smartstorage.admin.entity.pestDetection.AiPestAlgorithmTask;
import com.chinaunicom.smartstorage.admin.mapper.aivision.pestDetection.PestDetectionMapper;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.PestDetectionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的粮面虫害数据访问实现。
 *
 * <p>目标库：{@code smart_storage_ai_extension}。本类只负责数据访问，不做业务判断。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "ai-vision.datasource", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class MyBatisPestDetectionRepository implements PestDetectionRepository {

    @Resource
    private PestDetectionMapper mapper;

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
    public List<MonitorPointRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        return mapper.listMonitorPoint(unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<PestEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return mapper.listRecentEvent(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<PestEventRespDTO> listEventByCondition(AiVisionDataScope scope, PestEventQueryReqDTO query) {
        return mapper.listEventByCondition(applyScope(query, scope));
    }

    @Override
    public List<PestEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                     PestEventQueryReqDTO query, int limit) {
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
    private PestEventQueryReqDTO applyScope(PestEventQueryReqDTO query, AiVisionDataScope scope) {
        PestEventQueryReqDTO target = query == null ? new PestEventQueryReqDTO() : query;
        target.setScopeUnrestricted(unrestricted(scope));
        target.setScopeWarehouseIds(warehouseIds(scope));
        return target;
    }

    @Override
    public PestEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return null;
        }

        // 越权与不存在一律返回 null，不区分两种情况以避免泄露对象存在性
        PestEventDetailRespDTO detail =
            mapper.getEventDetailInScope(id, unrestricted(scope), warehouseIds(scope));
        if (detail == null) {
            log.warn("[PestDetection] 检测记录超出当前用户数据范围或不存在，已拒绝访问");
            return null;
        }

        // 证据：仅返回受控引用，换发短时地址由 Service 经 MediaGateway 处理
        List<PestEvidenceRespDTO> evidences = mapper.listEvidenceByEventId(id);
        detail.setEvidences(evidences == null ? new ArrayList<>() : evidences);

        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        // 处理审计随记录一起受数据范围保护。不能只依赖调用方先查过详情：
        // 接口签名已要求传入 scope，这里必须自己兑现，
        // 否则将来任何新调用点都会静默泄露审计内容。
        PestEventRespDTO event = mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (event == null) {
            return List.of();
        }
        List<ProcessRecordRespDTO> list = mapper.listProcessRecords(id);
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
                            String processedBy) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }
        Long id = parseId(eventId);
        if (id == null) {
            return 0;
        }

        // 写操作的范围校验下推到 SQL
        PestEventRespDTO existing = mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (existing == null) {
            log.warn("[PestDetection] 处理请求超出数据范围或记录不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        int affected = mapper.updateProcessWithVersion(
            id, eventVersion, processResult, processedBy, processOpinion, now);

        if (affected == 0) {
            log.info("[PestDetection] 处理未生效（版本冲突或状态不允许）eventId={} version={}", eventId, eventVersion);
            // 未更新状态就绝不写审计记录，否则审计与状态互相矛盾
            return 0;
        }

        // 追加式写入，不覆盖历史；与上面的更新同事务，失败一起回滚
        mapper.appendProcessRecord(
            snowflake.nextId(), id, existing.getEventNo(),
            statusBefore, processResult, processResult, processOpinion,
            processedBy, now, "WEB", now);

        return affected;
    }

    /* ==================== 算法任务（「重新识别」链路） ==================== */

    @Override
    public Long insertAlgorithmTask(AiPestAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(snowflake.nextId());
        }
        mapper.insertAlgorithmTask(task);
        return task.getId();
    }

    @Override
    public PestAlgorithmTaskRespDTO findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        PestAlgorithmTaskRespDTO task =
            mapper.getAlgorithmTaskByTaskNoInScope(taskNo, unrestricted(scope), warehouseIds(scope));
        if (task == null) {
            log.warn("[PestDetection] 算法任务超出当前用户数据范围或不存在，已拒绝访问");
        }
        return task;
    }

    @Override
    public List<PestAlgorithmTaskRespDTO> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        // 事件归属必须先过数据范围，越权事件即使有任务行也不返回
        PestEventRespDTO event = mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (event == null) {
            return List.of();
        }
        List<PestAlgorithmTaskRespDTO> list =
            mapper.listAlgorithmTasksByEventId(id, unrestricted(scope), warehouseIds(scope), Math.max(1, limit));
        return list == null ? List.of() : list;
    }

    /**
     * 保存网关结果 + 成功时回写事件，两笔写入同一事务。
     *
     * <p>事务管理器与 {@link #processEvent} 同一约定：只在实现类上声明，
     * 显式绑定本功能独立库的 {@code aiVisionTransactionManager}。</p>
     */
    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public void saveAlgorithmTaskResult(AiPestAlgorithmTask task, PestAlgorithmWriteBackDTO writeBack) {
        mapper.updateAlgorithmTaskResult(task);

        if (writeBack == null || writeBack.getEventId() == null) {
            return;
        }
        // 回写前按数据范围复核事件归属：任务行与事件行同仓，但仍自己兑现，
        // 不依赖调用方先查过（与 listProcessRecords 同一防御口径）。
        PestEventRespDTO event = mapper.getEventSummaryInScope(
            writeBack.getEventId(),
            writeBack.isScopeUnrestricted(),
            writeBack.getScopeWarehouseIds());
        if (event == null) {
            log.warn("[PestDetection] 算法回写目标事件超出数据范围或不存在，跳过事件更新 taskId={}",
                task.getTaskNo());
            return;
        }
        mapper.writeBackEventAlgorithmResult(writeBack, LocalDateTime.now());
    }

    /**
     * 记录标识转数值。非法格式返回 null 由调用方按「不存在」处理，
     * 不让 NumberFormatException 冒到接口层变成 500。
     */
    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("[PestDetection] 非法记录标识格式");
            return null;
        }
    }
}
