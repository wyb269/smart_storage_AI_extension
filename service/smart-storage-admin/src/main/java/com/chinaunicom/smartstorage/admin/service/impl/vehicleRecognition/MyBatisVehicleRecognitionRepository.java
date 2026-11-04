package com.chinaunicom.smartstorage.admin.service.impl.vehicleRecognition;

import cn.hutool.core.lang.Snowflake;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.mapper.aivision.vehicleRecognition.VehicleRecognitionMapper;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.VehicleRecognitionRepository;
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
 * 基于数据库的车辆识别数据访问实现。
 *
 * <p>目标库：{@code smart_storage_ai_extension}。本类只负责数据访问，不做业务判断。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "ai-vision.datasource", name = "enabled",
    havingValue = "true", matchIfMissing = false)
public class MyBatisVehicleRecognitionRepository implements VehicleRecognitionRepository {

    @Resource
    private VehicleRecognitionMapper mapper;

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
    public List<TodayVehicleRespDTO> listTodayVehicle(AiVisionDataScope scope, int limit) {
        return mapper.listTodayVehicle(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<MonitorPointRespDTO> listPoints(AiVisionDataScope scope) {
        return mapper.listPoints(unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public List<VehicleEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return mapper.listRecentEvent(unrestricted(scope), warehouseIds(scope), limit);
    }

    @Override
    public List<VehicleEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                          VehicleEventQueryReqDTO query) {
        return mapper.listEventByCondition(applyScope(query, scope));
    }

    @Override
    public List<VehicleEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                        VehicleEventQueryReqDTO query, int limit) {
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
    private VehicleEventQueryReqDTO applyScope(VehicleEventQueryReqDTO query,
                                               AiVisionDataScope scope) {
        VehicleEventQueryReqDTO target = query == null ? new VehicleEventQueryReqDTO() : query;
        target.setScopeUnrestricted(unrestricted(scope));
        target.setScopeWarehouseIds(warehouseIds(scope));
        return target;
    }

    @Override
    public VehicleEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return null;
        }

        // 越权与不存在一律返回 null，不区分两种情况以避免泄露对象存在性
        VehicleEventDetailRespDTO detail =
            mapper.getEventDetailInScope(id, unrestricted(scope), warehouseIds(scope));
        if (detail == null) {
            // 严禁在此打印车牌（PII）；只打印越权/不存在这一事实
            log.warn("[VehicleRecognition] 识别记录超出当前用户数据范围或不存在，已拒绝访问");
            return null;
        }

        // 证据：仅返回受控引用换发前的短时地址由 Service 经 MediaGateway 处理
        List<VehicleEvidenceRespDTO> evidences = mapper.listEvidenceByEventId(id);
        detail.setEvidences(evidences == null ? new ArrayList<>() : evidences);

        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        // 核实记录随记录一起受数据范围保护。不能只依赖调用方先查过详情：
        // 接口签名已要求传入 scope，这里必须自己兑现，
        // 否则将来任何新调用点都会静默泄露审计内容。
        VehicleEventRespDTO event =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (event == null) {
            return List.of();
        }
        List<ProcessRecordRespDTO> list = mapper.listProcessRecords(id);
        return list == null ? List.of() : list;
    }

    /* ==================== 人工核实 ==================== */

    /**
     * 提交人工核实：乐观锁更新 + 追加审计记录，二者处于同一事务。
     *
     * <p>事务管理器显式写共享独立库的 {@code aiVisionTransactionManager}：
     * 只有在这里（而不是 Service 上）声明才安全——该 Bean 仅在
     * {@code ai-vision.datasource.enabled=true} 时存在，本类同样带该条件注解，
     * 二者生命周期一致。</p>
     */
    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String verifyResult, String verifyOpinion,
                            String processedBy) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }
        Long id = parseId(eventId);
        if (id == null) {
            return 0;
        }

        // 写操作的范围校验下推到 SQL
        VehicleEventRespDTO existing =
            mapper.getEventSummaryInScope(id, unrestricted(scope), warehouseIds(scope));
        if (existing == null) {
            log.warn("[VehicleRecognition] 核实请求超出数据范围或记录不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        int affected = mapper.updateVerifyWithVersion(
            id, eventVersion, verifyResult, processedBy, verifyOpinion, now);

        if (affected == 0) {
            log.info("[VehicleRecognition] 核实未生效（版本冲突或状态不允许）eventId={} version={}",
                eventId, eventVersion);
            // 未更新状态就绝不写审计记录，否则审计与状态互相矛盾
            return 0;
        }

        // 追加式写入，不覆盖历史；与上面的更新同事务，失败一起回滚
        mapper.appendProcessRecord(
            snowflake.nextId(), id, existing.getEventNo(),
            statusBefore, verifyResult, verifyResult, verifyOpinion,
            processedBy, now, "WEB", now);

        return affected;
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
            log.warn("[VehicleRecognition] 非法识别记录标识格式");
            return null;
        }
    }

    /* ==================== 算法任务 ==================== */

    /**
     * 算法任务归属骨架：所有归属列都由这条已校验范围的读取给出。
     *
     * <p>Service 拿到的骨架里 warehouse_id / point_id 等一律来自库里的记录，
     * 前端伪造不了别人的仓库；这也是 insert 语句本身不再带范围条件的前提。</p>
     */
    @Override
    public VehicleRecognitionAlgorithmTask findAlgorithmTaskContext(AiVisionDataScope scope, String eventId) {
        Long id = parseId(eventId);
        if (id == null) {
            return null;
        }
        return mapper.selectAlgorithmTaskContext(id, unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public Long insertAlgorithmTask(VehicleRecognitionAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        if (task.getId() == null) {
            task.setId(snowflake.nextId());
        }
        mapper.insertAlgorithmTask(task);
        return task.getId();
    }

    @Override
    public int updateAlgorithmTask(AiVisionDataScope scope, VehicleRecognitionAlgorithmTask task) {
        if (task == null || task.getTaskNo() == null || task.getTaskNo().isBlank()) {
            return 0;
        }
        return mapper.updateAlgorithmTask(task, unrestricted(scope), warehouseIds(scope));
    }

    @Override
    public VehicleRecognitionAlgorithmTask findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        VehicleRecognitionAlgorithmTask task =
            mapper.selectAlgorithmTaskByTaskNo(taskNo, unrestricted(scope), warehouseIds(scope));
        if (task == null) {
            // 越权与不存在一律按「查不到」处理，避免用错误码枚举出他人任务是否存在
            log.warn("[VehicleRecognition] 算法任务超出当前用户数据范围或不存在，已拒绝访问");
        }
        return task;
    }

    @Override
    public List<VehicleRecognitionAlgorithmTask> listAlgorithmTasksByEvent(AiVisionDataScope scope,
                                                                          String eventId, int limit) {
        Long id = parseId(eventId);
        if (id == null) {
            return List.of();
        }
        List<VehicleRecognitionAlgorithmTask> list =
            mapper.selectAlgorithmTasksByEvent(id, unrestricted(scope), warehouseIds(scope), limit);
        return list == null ? List.of() : list;
    }

    /**
     * 回写事件的算法字段。
     *
     * <p>与 {@code processEvent} 分开写、互不重叠：人工核实只写 verify_*，
     * 算法只写 algorithm_*。两条链路各自 UPDATE 自己的列，才能允许并发发生。</p>
     *
     * <p>本方法<b>刻意不加乐观锁</b>：重跑不产生人工结论，也不希望因为
     * 有人正在核实这条记录而让算法回写整单失败。</p>
     */
    @Override
    @Transactional(transactionManager = "aiVisionTransactionManager", rollbackFor = Exception.class)
    public int writeBackEventAlgorithmResult(AiVisionDataScope scope, String eventId, Long taskId,
                                             String executionStatus, BigDecimal confidence,
                                             String updatedBy, LocalDateTime updatedAt) {
        Long id = parseId(eventId);
        if (id == null) {
            return 0;
        }
        return mapper.writeBackEventAlgorithm(id, taskId, executionStatus, confidence,
            updatedBy, updatedAt, unrestricted(scope), warehouseIds(scope));
    }
}
