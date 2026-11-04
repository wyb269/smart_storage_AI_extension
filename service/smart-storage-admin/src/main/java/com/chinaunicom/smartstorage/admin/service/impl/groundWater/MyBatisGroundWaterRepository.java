package com.chinaunicom.smartstorage.admin.service.impl.groundWater;

import cn.hutool.core.lang.Snowflake;
import com.chinaunicom.smartstorage.admin.constant.GroundWaterConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.groundWater.AiWaterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.mapper.aivision.groundWater.GroundWaterMapper;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.GroundWaterRepository;
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
 * 基于数据库的地面积水数据访问实现。
 *
 * <p>目标库：{@code smart_storage_ai_extension}。本类只负责数据访问，不做业务判断。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "ai-vision.datasource", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class MyBatisGroundWaterRepository implements GroundWaterRepository {

    @Resource
    private GroundWaterMapper mapper;

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
    public List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        return mapper.listMonitorPoint(unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<WaterEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return mapper.listRecentEvent(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<WaterEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                        WaterEventQueryReqDTO query) {
        return mapper.listEventByCondition(applyScope(query, scope));
    }

    @Override
    public List<WaterEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                      WaterEventQueryReqDTO query, int limit) {
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
    private WaterEventQueryReqDTO applyScope(WaterEventQueryReqDTO query, AiVisionDataScope scope) {
        WaterEventQueryReqDTO target = query == null ? new WaterEventQueryReqDTO() : query;
        target.setScopeUnrestricted(unrestricted(scope));
        target.setScopeWarehouseIds(warehouseIds(scope));
        return target;
    }

    @Override
    public WaterEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return null;
        }

        // 越权与不存在一律返回 null，不区分两种情况以避免泄露对象存在性
        WaterEventDetailRespDTO detail =
            mapper.getEventDetailInScope(id, unrestricted(scope), warehouseIds(scope));
        if (detail == null) {
            log.warn("[GroundWater] 事件超出当前用户数据范围或不存在，已拒绝访问");
            return null;
        }

        // 证据：仅返回受控引用，换发短时地址由 Service 经 MediaGateway 处理
        List<WaterEvidenceRespDTO> evidences = mapper.listEvidenceByEventId(id);
        detail.setEvidences(evidences == null ? new ArrayList<>() : evidences);

        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        // 处理记录随事件一起受数据范围保护。不能只依赖调用方先查过详情：
        // 接口签名已要求传入 scope，这里必须自己兑现，
        // 否则将来任何新调用点都会静默泄露审计内容。
        WaterEventRespDTO event =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
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
                            String processedBy, String source) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }
        Long id = parseId(eventId);
        if (id == null) {
            return 0;
        }

        // 写操作的范围校验下推到 SQL
        WaterEventRespDTO existing =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (existing == null) {
            log.warn("[GroundWater] 处理请求超出数据范围或事件不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        int affected = mapper.updateProcessWithVersion(
            id, eventVersion, processResult, processedBy, processOpinion, now);

        if (affected == 0) {
            log.info("[GroundWater] 事件处理未生效（版本冲突或状态不允许）eventId={} version={}",
                eventId, eventVersion);
            // 未更新状态就绝不写审计记录，否则审计与状态互相矛盾
            return 0;
        }

        // 追加式写入，不覆盖历史；与上面的更新同事务，失败一起回滚
        mapper.appendProcessRecord(
            snowflake.nextId(), id, existing.getEventNo(),
            statusBefore, processResult, processResult, processOpinion,
            processedBy, now,
            source == null || source.isBlank() ? GroundWaterConstant.PROCESS_SOURCE_WEB : source,
            now);

        return affected;
    }

    /* ==================== 算法任务（「重新识别」链路） ==================== */

    @Override
    public AiWaterAlgorithmTask insertAlgorithmTask(AiWaterAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(snowflake.nextId());
        }
        mapper.insertAlgorithmTask(task);
        return task;
    }

    @Override
    public AiWaterAlgorithmTask getAlgorithmTaskByTaskNo(String taskNo, AiVisionDataScope scope) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        // 与事件详情同口径：越权与不存在都返回 null，不暴露对象存在性
        return mapper.getAlgorithmTaskByTaskNo(taskNo, unrestricted(scope), warehouseIds(scope));
    }

    /**
     * 任务产出落库 + 事件算法侧回填，同一条链路两条 UPDATE，必须同事务：
     * 否则中途失败会留下「任务已 SUCCEEDED 但事件还挂着旧状态」的错位行，
     * 页面两个区域自相矛盾，比整体失败更难排查。
     *
     * <p>事务管理器与 {@link #processEvent} 同一取舍：只在实现类上声明、
     * 显式绑定 {@code aiVisionTransactionManager}（仅独立库启用时存在）。</p>
     */
    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int saveAlgorithmTaskOutcome(AiWaterAlgorithmTask task, String eventId,
                                        boolean writeBackEvent, AiVisionDataScope scope,
                                        String updatedBy) {
        final LocalDateTime now = LocalDateTime.now();
        int affected = mapper.updateAlgorithmTaskOutcome(
            task, updatedBy, now, unrestricted(scope), warehouseIds(scope));

        if (affected != 1) {
            // 任务行没更新成功就绝不回填事件，避免状态与引用互相矛盾
            log.info("[GroundWater] 算法任务更新未生效（范围或并发）taskNo={}", task.getTaskNo());
            return 0;
        }

        if (writeBackEvent && eventId != null && !eventId.isBlank()) {
            Long id = parseId(eventId);
            if (id != null) {
                // 只回填算法侧字段；process_status / event_version 不在 UPDATE 列里
                mapper.writeBackEventAlgorithm(id, task.getId(),
                    task.getExecutionStatus(), task.getConclusion(),
                    task.getConfidence(), task.getWaterType(), updatedBy, now);
            }
        }
        return affected;
    }

    @Override
    public List<AiWaterAlgorithmTask> listAlgorithmTasksByEventId(AiVisionDataScope scope,
                                                                  String eventId, int limit) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        List<AiWaterAlgorithmTask> list =
            mapper.listAlgorithmTasksByEventId(id, unrestricted(scope), warehouseIds(scope), limit);
        return list == null ? List.of() : list;
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
            log.warn("[GroundWater] 非法事件标识格式");
            return null;
        }
    }
}
