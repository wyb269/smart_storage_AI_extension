package com.chinaunicom.smartstorage.admin.service.impl.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.constant.DoorAnomalyConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventProcessReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.DoorAnomalyStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.DoorAnomalyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版数据访问实现（兜底，仅在独立数据源未启用时生效）。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true
 * （未配置环境变量、显式关闭等）时由 Spring 注入，
 * 用于数据库未就绪时做前端联调。</p>
 *
 * <p><b>正常运行时</b>：由 {@link MyBatisDoorAnomalyRepository} 提供实现，
 * 直连 {@code smart_storage_ai_extension} 数据库，与主库完全隔离。</p>
 *
 * <p><b>本实现</b>：数据进程重启即丢失，<b>不得用于生产环境</b>。</p>
 *
 * @author 仓门异动功能
 */
@Slf4j
@org.springframework.stereotype.Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryDoorAnomalyRepository implements DoorAnomalyRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] DOOR_NAMES = {
        "一号粮库东门", "一号粮库西门", "二号粮库南门", "二号粮库北门",
        "三号粮库东门", "三号粮库西门", "散粮库南门", "散粮库北门",
    };

    /** 仓门状态表 */
    private final Map<String, DoorStatusRespDTO> doorStore = new ConcurrentHashMap<>();

    /** 事件主表 */
    private final Map<String, AnomalyEventRespDTO> eventStore = new ConcurrentHashMap<>();

    /** 事件详情表 */
    private final Map<String, EventDetailRespDTO> detailStore = new ConcurrentHashMap<>();

    /** 处理记录表，key = eventId */
    private final Map<String, List<ProcessRecordRespDTO>> processStore = new ConcurrentHashMap<>();

    /** 自增 ID */
    private final AtomicLong idGenerator = new AtomicLong(1000L);

    /** 算法任务表，key = taskNo（仅内存兜底，进程重启即丢，不得用于生产） */
    private final Map<String, AiAlgorithmTask> taskStore = new ConcurrentHashMap<>();

    public InMemoryDoorAnomalyRepository() {
        seed();
    }

    /**
     * 兜底演示数据：8 个仓门 + 12 条事件 + 每条事件一条历史算法任务。
     *
     * <p>没有这段种子时，内存模式下仓门页面整页为空，验收时看不出
     * 「算法任务」「重新识别」是否真的接通。真实库路径以 04 / 07 脚本为准。</p>
     */
    private void seed() {
        final LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 8; i++) {
            String warehouseId = String.valueOf(9001 + i % 4);
            String warehouseName = new String[]{"一号粮库", "二号粮库", "三号粮库", "散粮库"}[i % 4];
            DoorStatusRespDTO door = new DoorStatusRespDTO();
            door.setDoorId(String.valueOf(100 + i));
            door.setDoorName(DOOR_NAMES[i]);
            door.setWarehouseId(warehouseId);
            door.setWarehouseName(warehouseName);
            door.setCameraId("C" + String.format("%03d", i + 1));
            door.setCameraCode(String.format("CAM-%03d", i + 1));
            door.setDeviceStatus(i == 6
                ? DoorAnomalyStatusEnum.DeviceStatusEnum.OFFLINE.name()
                : DoorAnomalyStatusEnum.DeviceStatusEnum.ONLINE.name());
            door.setDoorPhysicalStatus(i % 3 == 0
                ? DoorAnomalyStatusEnum.DoorPhysicalStatusEnum.OPEN.name()
                : DoorAnomalyStatusEnum.DoorPhysicalStatusEnum.CLOSED.name());
            door.setAlarmStatus(i % 4 == 0
                ? DoorAnomalyStatusEnum.AlarmStatusEnum.ACTIVE.name()
                : DoorAnomalyStatusEnum.AlarmStatusEnum.NORMAL.name());
            door.setLastHeartbeatAt(TS.format(now.minusMinutes(i == 6 ? 200 : 1 + i % 3)));
            door.setLatestEventAt(null);
            door.setSortNo(i + 1);
            doorStore.put(door.getDoorId(), door);
        }

        for (int i = 0; i < 12; i++) {
            DoorStatusRespDTO door = doorStore.get(String.valueOf(100 + i % 8));
            DoorAnomalyStatusEnum.AnomalyTypeEnum type =
                DoorAnomalyStatusEnum.AnomalyTypeEnum.values()[i % 5];
            String processStatus = i % 3 == 0
                ? DoorAnomalyStatusEnum.ProcessStatusEnum.PENDING.name()
                : DoorAnomalyStatusEnum.ProcessStatusEnum.CLOSED.name();
            LocalDateTime occurred = now.minusMinutes(37L * i);

            EventDetailRespDTO d = new EventDetailRespDTO();
            d.setEventId(String.valueOf(3000 + i));
            d.setEventNo("DA" + TS.format(occurred).replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
                + String.format("%04d", i + 1));
            d.setOccurredAt(TS.format(occurred));
            d.setWarehouseId(door.getWarehouseId());
            d.setWarehouseName(door.getWarehouseName());
            d.setDoorId(door.getDoorId());
            d.setDoorName(door.getDoorName());
            d.setCameraId(door.getCameraId());
            d.setCameraCode(door.getCameraCode());
            d.setAnomalyType(type.name());
            d.setAnomalyTypeName(type.getDescription());
            d.setProcessStatus(processStatus);
            d.setEventVersion(1);
            if (!DoorAnomalyStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                d.setProcessedBy("系统");
                d.setProcessedAt(TS.format(occurred.plusMinutes(3)));
                d.setProcessOpinion(DoorAnomalyConstant.AUTO_VIEW_OPINION);
            }
            d.setEventDesc("仓门识别到「" + type.getDescription() + "」，已生成异常事件");
            d.setIsSimulated(true);

            AiAlgorithmTask task = seedAlgorithmTask(d, i, occurred);
            taskStore.put(task.getTaskNo(), task);
            d.setAlgorithmStatus(task.getExecutionStatus());
            d.setAlgorithmResult(task.getConclusion());
            d.setConfidence(task.getConfidence());
            d.setAlgorithmTaskNo(task.getTaskNo());
            d.setModelName(task.getModelName());
            d.setModelVersion(task.getModelVersion());
            d.setAlgorithmVersion(task.getAlgorithmVersion());
            d.setConfigVersion(task.getConfigVersion());
            d.setInferenceStartedAt(TS.format(task.getStartedAt()));
            d.setInferenceFinishedAt(task.getFinishedAt() == null ? null : TS.format(task.getFinishedAt()));
            d.setInferenceDurationMs(task.getInferenceDurationMs());

            EventEvidenceRespDTO ev = new EventEvidenceRespDTO();
            ev.setEvidenceId(d.getEventId() + "-1");
            ev.setEvidenceType("ANOMALY_FRAME");
            ev.setOriginalUrl("mock://door/" + d.getEventId() + "/anomaly");
            ev.setThumbnailUrl("mock://door/" + d.getEventId() + "/thumb");
            ev.setCapturedAt(d.getOccurredAt());
            ev.setCameraCode(d.getCameraCode());
            ev.setSortNo(1);
            d.setEvidences(new ArrayList<>(List.of(ev)));

            door.setLatestEventAt(d.getOccurredAt());
            loadEvents(List.of(d));
        }
    }

    /**
     * 与 07_seed_algorithm_tasks.sql 同口径：第 5 条 FAILED、第 9 条 TIMEOUT、
     * 第 12 条 RUNNING，其余 SUCCEEDED；未成功的任务没有结论与置信度。
     */
    private AiAlgorithmTask seedAlgorithmTask(EventDetailRespDTO d, int index, LocalDateTime occurred) {
        String status = DoorAnomalyStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name();
        String errorCode = null;
        String errorMessage = null;
        if (index == 4) {
            status = DoorAnomalyStatusEnum.AlgorithmStatusEnum.FAILED.name();
            errorCode = "FRAME_BLUR";
            errorMessage = "夜间画面噪点过高，门框轮廓置信度不足";
        } else if (index == 8) {
            status = DoorAnomalyStatusEnum.AlgorithmStatusEnum.TIMEOUT.name();
            errorCode = "INFER_TIMEOUT";
            errorMessage = "算法执行超时，未返回结论";
        } else if (index == 11) {
            status = DoorAnomalyStatusEnum.AlgorithmStatusEnum.RUNNING.name();
        }

        boolean succeeded = DoorAnomalyStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);
        boolean started = succeeded || errorCode != null;
        LocalDateTime startedAt = started ? occurred.plusSeconds(2) : LocalDateTime.now().minusMinutes(2);
        long durationMs = 21L + (index * 73L) % 356L;
        BigDecimal confidence = succeeded ? new BigDecimal("0.8700") : null;

        AiAlgorithmTask t = new AiAlgorithmTask();
        t.setId(93000L + index);
        t.setTaskNo("DAT" + TS.format(startedAt).replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
            + String.format("%04d", index + 1));
        t.setCandidateEventId(Long.parseLong(d.getEventId()));
        t.setDoorId(Long.parseLong(d.getDoorId()));
        t.setCameraId(d.getCameraId());
        t.setWarehouseId(Long.parseLong(d.getWarehouseId()));
        t.setTaskType(DoorAnomalyConstant.ALGORITHM_TASK_TYPE);
        t.setMediaType("SINGLE_IMAGE");
        t.setMediaRefs("[\"mock://door/" + d.getEventId() + "/anomaly\"]");
        t.setNormalReferenceRef("mock://door/" + d.getEventId() + "/normal");
        t.setRoi("{\"x\":0.15,\"y\":0.10,\"w\":0.70,\"h\":0.78}");
        t.setExecutionStatus(status);
        t.setConclusion(succeeded ? DoorAnomalyStatusEnum.AlgorithmResultEnum.ANOMALY.name() : null);
        t.setAnomalyType(succeeded ? d.getAnomalyType() : null);
        t.setConfidence(confidence);
        t.setModelName("mock-door-anomaly-detector");
        t.setModelVersion("mock-1.0.0");
        t.setAlgorithmVersion("mock-adapter-1.0.0");
        t.setConfigVersion(DoorAnomalyConstant.ALGORITHM_CONFIG_VERSION);
        t.setConfidenceThreshold(new BigDecimal("0.7500"));
        t.setMediaDigest("sha256:seedtask" + String.format("%04d", index + 1));
        t.setStartedAt(startedAt);
        t.setFinishedAt(succeeded ? startedAt.plusNanos(durationMs * 1_000_000L) : null);
        t.setInferenceDurationMs(started ? durationMs : null);
        t.setDetections(succeeded ? "[{\"category\":\"DOOR_STATE\",\"confidence\":0.87,"
            + "\"bbox\":\"[150,100,1100,780]\"}]" : null);
        t.setEvidenceRefs("[{\"evidenceType\":\"ANOMALY_FRAME\","
            + "\"objectKey\":\"mock://door/" + d.getEventId() + "/anomaly\"}]");
        t.setRawResultRef("mock://task/seed" + String.format("%04d", index + 1) + "/raw");
        t.setErrorCode(errorCode);
        t.setErrorMessage(errorMessage);
        t.setRetryCount("FRAME_BLUR".equals(errorCode) ? 1 : 0);
        t.setTraceId("seedtask-" + String.format("%04d", index + 1));
        t.setIsSimulated(Boolean.TRUE);
        t.setCreatedBy("seed-memory");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedBy("seed-memory");
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Override
    public List<DoorStatusRespDTO> listDoorStatus(AiVisionDataScope scope) {
        List<DoorStatusRespDTO> list = new ArrayList<>(doorStore.values());
        // P0-6：内存实现同样必须执行数据范围过滤，避免联调期误判为「已生效」
        list.removeIf(d -> !inScope(scope, d.getWarehouseId()));
        list.sort(Comparator.comparing(d -> d.getSortNo() == null ? 0 : d.getSortNo()));
        return list;
    }

    @Override
    public List<AnomalyEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        List<AnomalyEventRespDTO> list = new ArrayList<>(eventStore.values());
        list.removeIf(e -> !inScope(scope, e.getWarehouseId()));
        list.sort(Comparator.comparing(AnomalyEventRespDTO::getOccurredAt).reversed());
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @Override
    public List<AnomalyEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                          EventQueryReqDTO query) {
        return listEventForExport(scope, query, Integer.MAX_VALUE);
    }

    @Override
    public List<AnomalyEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                        EventQueryReqDTO query, int limit) {
        List<AnomalyEventRespDTO> list = new ArrayList<>(eventStore.values());

        // P0-6：数据范围过滤
        list.removeIf(e -> !inScope(scope, e.getWarehouseId()));

        // 条件过滤，对应 SQL 的 where 子句
        if (hasText(query.getEventNo())) {
            list.removeIf(e -> !e.getEventNo().contains(query.getEventNo()));
        }
        if (query.getWarehouseId() != null) {
            list.removeIf(e -> !query.getWarehouseId().equals(toNumericId(e.getWarehouseId())));
        }
        if (query.getDoorId() != null) {
            list.removeIf(e -> !query.getDoorId().equals(toNumericId(e.getDoorId())));
        }
        if (hasText(query.getCameraCode())) {
            list.removeIf(e -> !hasText(e.getCameraCode())
                || !e.getCameraCode().contains(query.getCameraCode()));
        }
        if (hasText(query.getAnomalyType())) {
            list.removeIf(e -> !e.getAnomalyType().equals(query.getAnomalyType()));
        }
        if (hasText(query.getProcessStatus())) {
            list.removeIf(e -> !e.getProcessStatus().equals(query.getProcessStatus()));
        }
        if (query.getStartTime() != null) {
            list.removeIf(e -> parseTime(e.getOccurredAt()) == null
                || parseTime(e.getOccurredAt()).isBefore(query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            list.removeIf(e -> parseTime(e.getOccurredAt()) == null
                || parseTime(e.getOccurredAt()).isAfter(query.getEndTime()));
        }

        // 默认按发生时间倒序（第18.3节）
        boolean desc = query.getOrderByOccurredDesc() == null || query.getOrderByOccurredDesc();
        list.sort(desc
            ? Comparator.comparing(AnomalyEventRespDTO::getOccurredAt).reversed()
            : Comparator.comparing(AnomalyEventRespDTO::getOccurredAt));

        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @Override
    public EventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        EventDetailRespDTO detail = detailStore.get(eventId);
        if (detail == null) {
            return null;
        }
        // P0-6：越权访问一律按「不存在」处理，不泄露对象是否存在
        if (!inScope(scope, detail.getWarehouseId())) {
            log.warn("[InMemoryRepo] 事件超出数据范围，已拒绝访问");
            return null;
        }
        // 必须返回副本：Service 会把证据的受控键就地替换成短时地址，
        // 直接交出库存对象会让每次查看都污染存储（见 copyDetail 注释）。
        return copyDetail(detail);
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        // P0-6：处理记录随事件一起受数据范围保护
        if (getEventDetail(scope, eventId) == null) {
            return List.of();
        }
        List<ProcessRecordRespDTO> list = processStore.get(eventId);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        // 按时间正序展示完整历史（第17.6节：追加式记录）
        return new ArrayList<>(list);
    }

    /* ==================== 内部工具 ==================== */

    /**
     * 判断仓库是否在授权范围内。
     *
     * <p>与 {@code AiAnomalyEventMapper.xml} 的仓库范围条件保持同一语义：
     * 非全域时<b>只放行</b>授权集合内的仓库，集合为空或标识无法解析为数值时
     * 一律拒绝。内存版若比 SQL 宽松，联调期就会把越权读当成正常现象。</p>
     */
    private boolean inScope(AiVisionDataScope scope, String warehouseId) {
        if (scope == null) {
            return false;
        }
        if (scope.isUnrestricted()) {
            return true;
        }
        return scope.allowsWarehouse(toNumericId(warehouseId));
    }

    /**
     * 把对外的字符串标识解析为库内的数值形态（{@code ai_*.warehouse_id} 等为 BIGINT）。
     *
     * @return 无法解析时返回 null；数据范围判定按拒绝处理，筛选判定按不相等处理
     */
    private Long toNumericId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 提交人工处理：乐观锁更新 + 追加审计记录。
     *
     * <p>内存实现不需要事务（进程内 Map 操作，重启即丢，本就不得用于生产），
     * 但必须保持与 MyBatis 实现相同的原子语义：<b>状态未更新就绝不追加记录</b>，
     * 否则联调期看到的审计行为与真实库不一致。</p>
     */
    @Override
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, EventProcessReqDTO req, String processedBy) {
        if (eventId == null || eventVersion == null) {
            return 0;
        }

        // P0-6：写操作同样校验数据范围，越权返回 0（按并发冲突处理，不写审计）
        if (getEventDetail(scope, eventId) == null) {
            log.warn("[InMemoryRepo] 处理请求超出数据范围或事件不存在，拒绝写入");
            return 0;
        }

        final LocalDateTime now = LocalDateTime.now();
        // 复现乐观锁：版本不匹配则不更新任何字段
        final int[] affected = {0};
        eventStore.computeIfPresent(eventId, (key, existing) -> {
            if (!eventVersion.equals(existing.getEventVersion())) {
                log.info("[InMemoryRepo] 事件版本冲突 eventId={} 期望={} 实际={}",
                    eventId, eventVersion, existing.getEventVersion());
                return existing;
            }

            AnomalyEventRespDTO updated = copy(existing);
            // 仅更新人工处理相关字段
            // 算法结论与算法任务状态保持不变（第23节禁止覆盖算法结果）
            updated.setProcessStatus(req.getProcessResult());
            updated.setProcessedBy(processedBy);
            updated.setProcessedAt(now.format(TS));
            updated.setProcessOpinion(req.getProcessOpinion());
            // 版本号自增
            updated.setEventVersion(existing.getEventVersion() + 1);
            affected[0] = 1;
            return updated;
        });

        if (affected[0] != 1) {
            return 0;
        }

        // 同步更新详情副本，保证详情接口返回最新状态
        detailStore.computeIfPresent(eventId, (key, detail) -> {
            EventDetailRespDTO d = copyDetail(detail);
            d.setProcessStatus(req.getProcessResult());
            d.setProcessedBy(processedBy);
            d.setProcessedAt(now.format(TS));
            d.setProcessOpinion(req.getProcessOpinion());
            d.setEventVersion(detail.getEventVersion() + 1);
            return d;
        });

        ProcessRecordRespDTO record = new ProcessRecordRespDTO();
        record.setRecordId("PRC-" + idGenerator.incrementAndGet());
        record.setStatusBefore(statusBefore);
        record.setStatusAfter(req.getProcessResult());
        record.setProcessResult(req.getProcessResult());
        record.setProcessOpinion(req.getProcessOpinion());
        record.setProcessedBy(processedBy);
        record.setProcessedAt(now.format(TS));
        record.setSource("WEB");

        // 追加写入，不覆盖历史（第17.6节）
        processStore.computeIfAbsent(eventId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
            .add(record);

        return affected[0];
    }

    /* ==================== 以下为开发期数据装载入口 ==================== */

    /**
     * 装载仓门数据。
     *
     * <p>供测试数据脚本或后续联调工具调用，
     * 生产环境应由数据库查询替代。</p>
     */
    public void loadDoors(List<DoorStatusRespDTO> doors) {
        doors.forEach(d -> doorStore.put(d.getDoorId(), d));
    }

    /**
     * 装载事件与详情数据。
     */
    public void loadEvents(List<EventDetailRespDTO> details) {
        details.forEach(d -> {
            AnomalyEventRespDTO summary = new AnomalyEventRespDTO();
            summary.setEventId(d.getEventId());
            summary.setEventNo(d.getEventNo());
            summary.setOccurredAt(d.getOccurredAt());
            summary.setWarehouseId(d.getWarehouseId());
            summary.setWarehouseName(d.getWarehouseName());
            summary.setDoorId(d.getDoorId());
            summary.setDoorName(d.getDoorName());
            summary.setCameraId(d.getCameraId());
            summary.setCameraCode(d.getCameraCode());
            summary.setAnomalyType(d.getAnomalyType());
            summary.setAnomalyTypeName(d.getAnomalyTypeName());
            summary.setAlgorithmStatus(d.getAlgorithmStatus());
            summary.setAlgorithmResult(d.getAlgorithmResult());
            summary.setConfidence(d.getConfidence());
            summary.setProcessStatus(d.getProcessStatus());
            summary.setEventVersion(d.getEventVersion());
            eventStore.put(d.getEventId(), summary);
            detailStore.put(d.getEventId(), d);
        });
    }

    /**
     * 清空全部数据，供测试重置。
     */
    public void clear() {
        doorStore.clear();
        eventStore.clear();
        detailStore.clear();
        processStore.clear();
        taskStore.clear();
        idGenerator.set(1000L);
    }

    /* ==================== 算法任务（契约 §1，内存兜底实现） ==================== */

    @Override
    public void insertAlgorithmTask(AiAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(idGenerator.incrementAndGet());
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
        // 铁律 §0.1：一律模拟
        task.setIsSimulated(Boolean.TRUE);
        // 存副本，避免调用方后续复用同一对象导致库存被就地改写
        taskStore.put(task.getTaskNo(), copyTask(task));
    }

    @Override
    public AiAlgorithmTask getAlgorithmTaskByTaskNo(String taskNo, AiVisionDataScope scope) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        AiAlgorithmTask task = taskStore.get(taskNo);
        if (task == null) {
            return null;
        }
        // 与 SQL 版同语义：非全域时只放行授权仓库内的任务
        if (scope != null && !scope.isUnrestricted()
                && !scope.allowsWarehouse(task.getWarehouseId())) {
            return null;
        }
        return copyTask(task);
    }

    @Override
    public List<AiAlgorithmTask> listAlgorithmTasksByEvent(String eventId,
                                                           AiVisionDataScope scope, int limit) {
        Long id = parseEventId(eventId);
        if (id == null) {
            return List.of();
        }
        List<AiAlgorithmTask> list = new ArrayList<>();
        for (AiAlgorithmTask t : taskStore.values()) {
            if (id.equals(t.getCandidateEventId())
                    && (scope == null || scope.isUnrestricted()
                        || scope.allowsWarehouse(t.getWarehouseId()))) {
                list.add(copyTask(t));
            }
        }
        // started_at 倒序，空值最后（对齐 SQL 的 NULLS LAST）
        list.sort(Comparator.comparing(AiAlgorithmTask::getStartedAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return list.size() > limit ? new ArrayList<>(list.subList(0, Math.max(limit, 0))) : list;
    }

    @Override
    public void updateAlgorithmTask(AiAlgorithmTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        if (task.getTaskNo() != null) {
            taskStore.put(task.getTaskNo(), copyTask(task));
        }
    }

    @Override
    public int writebackEventAlgorithm(AiVisionDataScope scope, String eventId, Long algorithmTaskId,
                                       String algorithmStatus, String algorithmResult,
                                       BigDecimal confidence, String anomalyType, String updatedBy) {
        if (eventId == null || eventId.isBlank()) {
            return 0;
        }
        // 复用带范围的详情读取；越权时按「不存在」返回 0
        if (getEventDetail(scope, eventId) == null) {
            return 0;
        }
        final LocalDateTime now = LocalDateTime.now();
        int[] affected = {0};
        eventStore.computeIfPresent(eventId, (key, existing) -> {
            AnomalyEventRespDTO updated = copy(existing);
            updated.setAlgorithmStatus(algorithmStatus);
            updated.setAlgorithmResult(algorithmResult);
            updated.setConfidence(confidence);
            if (anomalyType != null && !anomalyType.isBlank()) {
                updated.setAnomalyType(anomalyType);
            }
            affected[0] = 1;
            return updated;
        });
        if (affected[0] != 1) {
            return 0;
        }
        detailStore.computeIfPresent(eventId, (key, detail) -> {
            EventDetailRespDTO d = copyDetail(detail);
            d.setAlgorithmStatus(algorithmStatus);
            d.setAlgorithmResult(algorithmResult);
            d.setConfidence(confidence);
            if (anomalyType != null && !anomalyType.isBlank()) {
                d.setAnomalyType(anomalyType);
            }
            return d;
        });
        log.info("[InMemoryRepo] 重新识别回写事件算法维度 eventId={} status={}", eventId, algorithmStatus);
        return affected[0];
    }

    private Long parseEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(eventId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 算法任务副本。
     *
     * <p>Service 轮询时会就地推进同一对象的状态字段，若与库存共享引用，
     * 上一次读取会污染存储；这里逐字段浅拷贝（字段皆为不可变类型）。</p>
     */
    private static AiAlgorithmTask copyTask(AiAlgorithmTask src) {
        AiAlgorithmTask t = new AiAlgorithmTask();
        t.setId(src.getId());
        t.setTaskNo(src.getTaskNo());
        t.setCandidateEventId(src.getCandidateEventId());
        t.setDoorId(src.getDoorId());
        t.setCameraId(src.getCameraId());
        t.setWarehouseId(src.getWarehouseId());
        t.setTaskType(src.getTaskType());
        t.setMediaType(src.getMediaType());
        t.setMediaRefs(src.getMediaRefs());
        t.setNormalReferenceRef(src.getNormalReferenceRef());
        t.setRoi(src.getRoi());
        t.setExecutionStatus(src.getExecutionStatus());
        t.setConclusion(src.getConclusion());
        t.setAnomalyType(src.getAnomalyType());
        t.setConfidence(src.getConfidence());
        t.setModelName(src.getModelName());
        t.setModelVersion(src.getModelVersion());
        t.setAlgorithmVersion(src.getAlgorithmVersion());
        t.setConfigVersion(src.getConfigVersion());
        t.setConfidenceThreshold(src.getConfidenceThreshold());
        t.setMediaDigest(src.getMediaDigest());
        t.setStartedAt(src.getStartedAt());
        t.setFinishedAt(src.getFinishedAt());
        t.setInferenceDurationMs(src.getInferenceDurationMs());
        t.setDetections(src.getDetections());
        t.setEvidenceRefs(src.getEvidenceRefs());
        t.setRawResultRef(src.getRawResultRef());
        t.setErrorCode(src.getErrorCode());
        t.setErrorMessage(src.getErrorMessage());
        t.setRetryCount(src.getRetryCount());
        t.setTraceId(src.getTraceId());
        t.setIsSimulated(src.getIsSimulated());
        t.setCreatedBy(src.getCreatedBy());
        t.setCreatedAt(src.getCreatedAt());
        t.setUpdatedBy(src.getUpdatedBy());
        t.setUpdatedAt(src.getUpdatedAt());
        return t;
    }

    /* ==================== 内部工具 ==================== */

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private static AnomalyEventRespDTO copy(AnomalyEventRespDTO src) {
        AnomalyEventRespDTO t = new AnomalyEventRespDTO();
        t.setEventId(src.getEventId());
        t.setEventNo(src.getEventNo());
        t.setOccurredAt(src.getOccurredAt());
        t.setWarehouseId(src.getWarehouseId());
        t.setWarehouseName(src.getWarehouseName());
        t.setDoorId(src.getDoorId());
        t.setDoorName(src.getDoorName());
        t.setCameraId(src.getCameraId());
        t.setCameraCode(src.getCameraCode());
        t.setAnomalyType(src.getAnomalyType());
        t.setAnomalyTypeName(src.getAnomalyTypeName());
        t.setAlgorithmStatus(src.getAlgorithmStatus());
        t.setAlgorithmResult(src.getAlgorithmResult());
        t.setConfidence(src.getConfidence());
        t.setProcessStatus(src.getProcessStatus());
        t.setProcessedBy(src.getProcessedBy());
        t.setProcessedAt(src.getProcessedAt());
        t.setEventVersion(src.getEventVersion());
        return t;
    }

    private static EventDetailRespDTO copyDetail(EventDetailRespDTO src) {
        EventDetailRespDTO t = new EventDetailRespDTO();
        t.setEventId(src.getEventId());
        t.setEventNo(src.getEventNo());
        t.setOccurredAt(src.getOccurredAt());
        t.setWarehouseId(src.getWarehouseId());
        t.setWarehouseName(src.getWarehouseName());
        t.setDoorId(src.getDoorId());
        t.setDoorName(src.getDoorName());
        t.setCameraId(src.getCameraId());
        t.setCameraCode(src.getCameraCode());
        t.setAnomalyType(src.getAnomalyType());
        t.setAnomalyTypeName(src.getAnomalyTypeName());
        t.setEventDesc(src.getEventDesc());
        t.setAlgorithmTaskNo(src.getAlgorithmTaskNo());
        t.setModelName(src.getModelName());
        t.setModelVersion(src.getModelVersion());
        t.setAlgorithmVersion(src.getAlgorithmVersion());
        t.setConfigVersion(src.getConfigVersion());
        t.setInferenceStartedAt(src.getInferenceStartedAt());
        t.setInferenceFinishedAt(src.getInferenceFinishedAt());
        t.setInferenceDurationMs(src.getInferenceDurationMs());
        t.setAlgorithmStatus(src.getAlgorithmStatus());
        t.setAlgorithmResult(src.getAlgorithmResult());
        t.setConfidence(src.getConfidence());
        t.setProcessStatus(src.getProcessStatus());
        t.setProcessedBy(src.getProcessedBy());
        t.setProcessedAt(src.getProcessedAt());
        t.setIsSimulated(src.getIsSimulated());
        t.setEventVersion(src.getEventVersion());
        // 证据必须逐条深拷贝：Service 会把 originalUrl / annotatedUrl / thumbnailUrl
        // 就地换成短时签发地址。若与库存共享同一个对象，
        // MockMediaGateway 每次查看都会在上一次结果后再拼一个 ?Expires=（无限增长），
        // 而 S3MediaGateway 会把受控键覆盖成 https 预签名地址，
        // 之后 isControlledReference 不再认可它，证据图片永久失效。
        if (src.getEvidences() != null) {
            List<EventEvidenceRespDTO> copies = new ArrayList<>(src.getEvidences().size());
            for (EventEvidenceRespDTO evidence : src.getEvidences()) {
                copies.add(copyEvidence(evidence));
            }
            t.setEvidences(copies);
        }
        return t;
    }

    /**
     * 复制一条证据元数据。
     */
    private static EventEvidenceRespDTO copyEvidence(EventEvidenceRespDTO src) {
        if (src == null) {
            return null;
        }
        EventEvidenceRespDTO t = new EventEvidenceRespDTO();
        t.setEvidenceId(src.getEvidenceId());
        t.setEvidenceType(src.getEvidenceType());
        t.setOriginalUrl(src.getOriginalUrl());
        t.setAnnotatedUrl(src.getAnnotatedUrl());
        t.setThumbnailUrl(src.getThumbnailUrl());
        t.setCapturedAt(src.getCapturedAt());
        t.setCameraCode(src.getCameraCode());
        t.setContentHash(src.getContentHash());
        t.setSortNo(src.getSortNo());
        return t;
    }
}