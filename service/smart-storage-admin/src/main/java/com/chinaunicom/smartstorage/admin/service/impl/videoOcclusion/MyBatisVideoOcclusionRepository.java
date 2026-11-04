package com.chinaunicom.smartstorage.admin.service.impl.videoOcclusion;

import cn.hutool.core.lang.Snowflake;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.videoOcclusion.AiOcclusionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.mapper.aivision.videoOcclusion.VideoOcclusionMapper;
import com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion.VideoOcclusionRepository;
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
 * 基于数据库的视频遮挡数据访问实现。
 *
 * <p>目标库：{@code smart_storage_ai_extension}。本类只负责数据访问，不做业务判断。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "ai-vision.datasource", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class MyBatisVideoOcclusionRepository implements VideoOcclusionRepository {

    @Resource
    private VideoOcclusionMapper mapper;

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
    public List<CameraStatusRespDTO> listCameraStatus(AiVisionDataScope scope) {
        return mapper.listCameraStatus(unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<OcclusionEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return mapper.listRecentEvent(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<OcclusionEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                            OcclusionEventQueryReqDTO query) {
        return mapper.listEventByCondition(applyScope(query, scope));
    }

    @Override
    public List<OcclusionEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                          OcclusionEventQueryReqDTO query, int limit) {
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
    private OcclusionEventQueryReqDTO applyScope(OcclusionEventQueryReqDTO query,
                                                 AiVisionDataScope scope) {
        OcclusionEventQueryReqDTO target = query == null ? new OcclusionEventQueryReqDTO() : query;
        target.setScopeUnrestricted(unrestricted(scope));
        target.setScopeWarehouseIds(warehouseIds(scope));
        return target;
    }

    @Override
    public OcclusionEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return null;
        }

        // 越权与不存在一律返回 null，不区分两种情况以避免泄露对象存在性
        OcclusionEventDetailRespDTO detail =
            mapper.getEventDetailInScope(id, unrestricted(scope), warehouseIds(scope));
        if (detail == null) {
            log.warn("[VideoOcclusion] 事件超出当前用户数据范围或不存在，已拒绝访问");
            return null;
        }

        // 证据：仅返回受控引用换发前的短时地址由 Service 经 MediaGateway 处理
        List<OcclusionEvidenceRespDTO> evidences = mapper.listEvidenceByEventId(id);
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
        OcclusionEventRespDTO event =
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
                            String processedBy) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }
        Long id = parseId(eventId);
        if (id == null) {
            return 0;
        }

        // 写操作的范围校验下推到 SQL
        OcclusionEventRespDTO existing =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (existing == null) {
            log.warn("[VideoOcclusion] 处理请求超出数据范围或事件不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        int affected = mapper.updateProcessWithVersion(
            id, eventVersion, processResult, processedBy, processOpinion, now);

        if (affected == 0) {
            log.info("[VideoOcclusion] 事件处理未生效（版本冲突或状态不允许）eventId={} version={}",
                eventId, eventVersion);
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
            log.warn("[VideoOcclusion] 非法事件标识格式");
            return null;
        }
    }

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    @Override
    public String insertAlgorithmTask(AiOcclusionAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        if (task.getId() == null) {
            task.setId(snowflake.nextId());
        }
        mapper.insertAlgorithmTask(task);
        return String.valueOf(task.getId());
    }

    @Override
    public OcclusionAlgorithmTaskRespDTO findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        // 越权与不存在一律 null，与事件侧同一口径，不泄露对象存在性
        return mapper.selectAlgorithmTaskByTaskNo(taskNo, unrestricted(scope), warehouseIds(scope));
    }

    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int updateAlgorithmTask(AiOcclusionAlgorithmTask task) {
        if (task == null || task.getId() == null) {
            return 0;
        }
        return mapper.updateAlgorithmTaskResult(task);
    }

    @Override
    public List<OcclusionAlgorithmTaskRespDTO> listAlgorithmTasksByEvent(AiVisionDataScope scope,
                                                                        String eventId, int limit) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        List<OcclusionAlgorithmTaskRespDTO> list =
            mapper.listAlgorithmTasksByEvent(id, unrestricted(scope), warehouseIds(scope), limit);
        return list == null ? List.of() : list;
    }

    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int writeBackEventAlgorithmResult(AiVisionDataScope scope, String eventId, Long taskId,
                                             String algorithmStatus, String algorithmResult,
                                             BigDecimal confidence, String updatedBy) {
        Long id = parseId(eventId);
        if (id == null || taskId == null) {
            return 0;
        }
        // 范围条件下推到 SQL（数据范围块 8/8）；不触碰 process_status / event_version
        return mapper.updateEventAlgorithmResult(id, taskId, algorithmStatus, algorithmResult,
            confidence, updatedBy, LocalDateTime.now(), unrestricted(scope), warehouseIds(scope));
    }
}
