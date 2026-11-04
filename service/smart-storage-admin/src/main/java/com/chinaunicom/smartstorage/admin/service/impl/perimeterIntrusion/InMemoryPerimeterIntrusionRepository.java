package com.chinaunicom.smartstorage.admin.service.impl.perimeterIntrusion;

import com.chinaunicom.smartstorage.admin.constant.PerimeterIntrusionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion.AiPerimeterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.PerimeterIntrusionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.PerimeterIntrusionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 周界入侵数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳时间每次调用时基于当前时刻计算，原因见 {@link #heartbeat(int)}。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryPerimeterIntrusionRepository implements PerimeterIntrusionRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[][] WAREHOUSES = {
        {"9001", "一号粮库"}, {"9002", "二号粮库"}, {"9003", "三号粮库"}, {"9004", "散粮库"},
    };

    /** 四面防区，顺序与原型网格的分区展示一致 */
    private static final PerimeterIntrusionStatusEnum.ZoneEnum[] ZONES = {
        PerimeterIntrusionStatusEnum.ZoneEnum.EAST,
        PerimeterIntrusionStatusEnum.ZoneEnum.WEST,
        PerimeterIntrusionStatusEnum.ZoneEnum.SOUTH,
        PerimeterIntrusionStatusEnum.ZoneEnum.NORTH,
    };

    /** 每个防区三条墙段的名称后缀 */
    private static final String[] SEGMENT_SUFFIX = {"墙段01", "墙段02", "墙段03"};

    /** 防区汉字前缀，用于拼出「东防区-东墙段01」这类卡片标题 */
    private static final String[] ZONE_CN = {"东", "西", "南", "北"};

    private final List<PerimeterEventRespDTO> events = new CopyOnWriteArrayList<>();
    private final List<ProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /** 内存算法任务表，与真实库 ai_perimeter_algorithm_task 同形，供兜底模式跑「重新识别」。 */
    private final List<AiPerimeterAlgorithmTask> algorithmTasks = new CopyOnWriteArrayList<>();
    private final AtomicLong taskSeq = new AtomicLong(960_000L);

    public InMemoryPerimeterIntrusionRepository() {
        seed();
    }

    /** 生成 12 个防区段与 40 条入侵事件 */
    private void seed() {
        for (int i = 0; i < 40; i++) {
            int zoneIndex = i % ZONES.length;
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            PerimeterIntrusionStatusEnum.IntrusionTypeEnum type =
                PerimeterIntrusionStatusEnum.IntrusionTypeEnum.values()[i % 5];
            String processStatus = i % 4 == 0
                ? PerimeterIntrusionStatusEnum.ProcessStatusEnum.PENDING.name()
                : PerimeterIntrusionStatusEnum.ProcessStatusEnum.DONE.name();

            PerimeterEventRespDTO e = new PerimeterEventRespDTO();
            e.setEventId(String.valueOf(2000 + i));
            e.setEventNo("PD" + TS.format(LocalDateTime.now().minusHours(i)).replace("-", "").replace(":", "")
                .substring(0, 8) + String.format("%04d", i + 1));
            e.setOccurredAt(TS.format(LocalDateTime.now().minusMinutes(30L * i)));
            e.setWarehouseId(wh[0]);
            e.setWarehouseName(wh[1]);
            e.setPointId(String.valueOf(100 + i % 12));
            e.setPointName(pointNameOf(zoneIndex, i % 3));
            e.setZone(ZONES[zoneIndex].name());
            e.setZoneName(ZONES[zoneIndex].getLabel());
            e.setCameraConfigId(String.valueOf(100 + i % 12));
            e.setCameraCode(String.format("CAM-%03d", i % 12 + 1));
            e.setCameraPosition(ZONE_CN[zoneIndex] + "侧围墙中段");
            e.setIntrusionType(type.name());
            e.setIntrusionTypeName(type.getLabel());
            e.setAlgorithmStatus(PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            e.setAlgorithmResult(PerimeterIntrusionStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            e.setConfidence(i % 7 == 3 ? null : new BigDecimal("0.8500"));
            e.setProcessStatus(processStatus);
            e.setEventVersion(1);
            if (!PerimeterIntrusionStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                e.setProcessedBy("系统");
                e.setProcessedAt(TS.format(LocalDateTime.now().minusMinutes(10L * i)));
                e.setProcessOpinion(PerimeterIntrusionConstant.AUTO_VIEW_OPINION);
            }
            events.add(e);

            // 每条事件配一条历史算法任务，状态分布与 07_seed_algorithm_tasks.sql 一致：
            // 待核实记录里留出 进行中/失败/超时 样本，兜底模式下详情弹窗与「重新识别」都有内容。
            AiPerimeterAlgorithmTask task = seedAlgorithmTask(e, i, processStatus);
            algorithmTasks.add(task);
            e.setAlgorithmStatus(task.getExecutionStatus());
            e.setAlgorithmResult(task.getConclusion());
            e.setConfidence(task.getConfidence());
        }
    }

    /**
     * 内存兜底模式的历史算法任务。
     *
     * <p>状态分配（与真实库种子同口径）：待核实记录中第 1 条 QUEUED、第 2 条 RUNNING、
     * 第 3/4 条 FAILED、第 5 条 TIMEOUT，其余（含全部已处理记录）SUCCEEDED。
     * 未成功的事件置信度留空，用于验证页面「暂无」而不是 0%。</p>
     */
    private AiPerimeterAlgorithmTask seedAlgorithmTask(PerimeterEventRespDTO e, int index, String processStatus) {
        boolean pending = PerimeterIntrusionStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus);
        String status = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name();
        String conclusion = e.getConfidence() == null
            ? null : PerimeterIntrusionStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        String errorCode = null;
        String errorMessage = null;
        BigDecimal confidence = e.getConfidence();

        if (pending && index % 30 == 0) {
            status = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.QUEUED.name();
            conclusion = null;
            confidence = null;
        } else if (pending && index % 30 == 3) {
            status = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.RUNNING.name();
            conclusion = null;
            confidence = null;
        } else if (pending && (index % 30 == 6 || index % 30 == 9)) {
            status = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.FAILED.name();
            conclusion = null;
            confidence = null;
            errorCode = "FRAME_BLUR";
            errorMessage = "夜间画面噪点过高，检测框置信度不足";
        } else if (pending && index % 30 == 12) {
            status = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name();
            conclusion = null;
            confidence = null;
            errorCode = "INFER_TIMEOUT";
            errorMessage = "算法执行超时，未返回结论";
        }

        boolean succeeded = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);
        LocalDateTime occurred = parseTs(e.getOccurredAt());
        LocalDateTime startedAt = succeeded || errorCode != null
            ? occurred.plusSeconds(2) : LocalDateTime.now().minusMinutes(2 + index);
        long durationMs = 18L + (index * 97L) % 403L;
        LocalDateTime finishedAt = succeeded ? startedAt.plusNanos(durationMs * 1_000_000L) : null;

        AiPerimeterAlgorithmTask t = new AiPerimeterAlgorithmTask();
        t.setId(96000L + index);
        t.setTaskNo("PDAT" + TS.format(startedAt).replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
            + String.format("%04d", index + 1));
        t.setEventId(parseLongSafe(e.getEventId()));
        t.setPointId(parseLongSafe(e.getPointId()));
        t.setCameraConfigId(parseLongSafe(e.getCameraConfigId()));
        t.setCameraCode(e.getCameraCode());
        t.setWarehouseId(parseLongSafe(e.getWarehouseId()));
        t.setRegionId("000000");
        t.setTaskType(PerimeterIntrusionConstant.ALGORITHM_TASK_TYPE);
        t.setMediaType(PerimeterIntrusionConstant.ALGORITHM_MEDIA_TYPE);
        t.setMediaRefs("[\"mock://perimeter/" + e.getEventId() + "/intrusion\"]");
        t.setNormalReferenceRef("mock://perimeter/" + e.getEventId() + "/normal");
        t.setExecutionStatus(status);
        t.setConclusion(conclusion);
        t.setIntrusionType(succeeded ? e.getIntrusionType() : null);
        t.setConfidence(confidence);
        t.setModelName("perimeter-intrusion-detector");
        t.setModelVersion("v1.2.0");
        t.setAlgorithmVersion("algo-2026.09.1");
        t.setConfigVersion(PerimeterIntrusionConstant.ALGORITHM_CONFIG_VERSION);
        t.setConfidenceThreshold(PerimeterIntrusionConstant.ALGORITHM_CONFIDENCE_THRESHOLD);
        t.setStartedAt(startedAt);
        t.setFinishedAt(finishedAt);
        t.setInferenceDurationMs(succeeded || errorCode != null ? durationMs : null);
        t.setDetections(succeeded ? "[{\"category\":\"PERSON\",\"confidence\":"
            + (confidence == null ? "0.85" : confidence.toPlainString()) + ",\"bbox\":\"[120,80,260,420]\"}]" : null);
        t.setEvidenceRefs("[{\"evidenceType\":\"INTRUSION_FRAME\","
            + "\"objectKey\":\"mock://perimeter/" + e.getEventId() + "/intrusion\"}]");
        t.setRawResultRef("mock://task/" + t.getTaskNo() + "/raw");
        t.setErrorCode(errorCode);
        t.setErrorMessage(errorMessage);
        t.setRetryCount(0);
        t.setTraceId("seed-trace-pd-" + e.getEventId());
        t.setIsSimulated(Boolean.TRUE);
        t.setCreatedBy("seed-memory");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedBy("seed-memory");
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    private LocalDateTime parseTs(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, TS);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 卡片标题：形如「东防区-东墙段01」 */
    private String pointNameOf(int zoneIndex, int segmentIndex) {
        return ZONE_CN[zoneIndex] + "防区-" + ZONE_CN[zoneIndex] + SEGMENT_SUFFIX[segmentIndex];
    }

    /**
     * 防区段状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值
     * 判定心跳超时，若在这里固定时间戳，进程跑久了全部防区段会被误判为离线，
     * 正常/报警/未布防状态就永远演示不出来。</p>
     *
     * <p>状态分布刻意覆盖四种展示态：正常布防、入侵报警、未布防、离线。</p>
     */
    @Override
    public List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        List<MonitorPointStatusRespDTO> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int zoneIndex = i / 3;
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            if (!allows(scope, wh[0])) {
                continue;
            }
            MonitorPointStatusRespDTO p = new MonitorPointStatusRespDTO();
            p.setPointId(String.valueOf(100 + i));
            p.setPointCode(String.format("PD-%03d", i + 1));
            p.setPointName(pointNameOf(zoneIndex, i % 3));
            p.setZone(ZONES[zoneIndex].name());
            p.setZoneName(ZONES[zoneIndex].getLabel());
            p.setSegmentName(ZONE_CN[zoneIndex] + SEGMENT_SUFFIX[i % 3]);
            p.setWarehouseId(wh[0]);
            p.setWarehouseName(wh[1]);
            p.setCameraConfigId(String.valueOf(100 + i));
            p.setCameraCode(String.format("CAM-%03d", i + 1));
            p.setCameraName(ZONE_CN[zoneIndex] + "侧围墙中段");
            p.setPlatformCameraId("C" + String.format("%03d", i + 1));

            if (i == 1 || i == 8) {
                // 明确离线：设备离线且心跳也已陈旧
                p.setDeviceStatus(PerimeterIntrusionStatusEnum.DeviceStatusEnum.OFFLINE.name());
                p.setAlarmStatus(PerimeterIntrusionStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setDefenseStatus(PerimeterIntrusionStatusEnum.DefenseStatusEnum.ARMED.name());
                p.setLastHeartbeatAt(heartbeat(200));
            } else if (i == 11) {
                // 状态未知：既不是明确离线也不是在线，页面显示「未知」
                p.setDeviceStatus(PerimeterIntrusionStatusEnum.DeviceStatusEnum.UNKNOWN.name());
                p.setAlarmStatus(PerimeterIntrusionStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setDefenseStatus(PerimeterIntrusionStatusEnum.DefenseStatusEnum.ARMED.name());
                p.setLastHeartbeatAt(null);
            } else {
                p.setDeviceStatus(PerimeterIntrusionStatusEnum.DeviceStatusEnum.ONLINE.name());
                p.setAlarmStatus(i == 3 || i == 6
                    ? PerimeterIntrusionStatusEnum.AlarmStatusEnum.ACTIVE.name()
                    : PerimeterIntrusionStatusEnum.AlarmStatusEnum.NORMAL.name());
                // 第 5、9 段人工撤防：即使在线无告警，页面也必须显示「未布防」
                p.setDefenseStatus(i == 5 || i == 9
                    ? PerimeterIntrusionStatusEnum.DefenseStatusEnum.DISARMED.name()
                    : PerimeterIntrusionStatusEnum.DefenseStatusEnum.ARMED.name());
                p.setLastHeartbeatAt(heartbeat(1 + i % 5));
            }
            p.setSortNo(i + 1);
            list.add(p);
        }
        return list;
    }

    private String heartbeat(int minutesAgo) {
        return TS.format(LocalDateTime.now().minusMinutes(minutesAgo));
    }

    /** 数据范围判定：非全域且仓库不在授权集合内则不可见 */
    private boolean allows(AiVisionDataScope scope, String warehouseId) {
        if (scope != null && scope.isUnrestricted()) {
            return true;
        }
        if (scope == null || scope.getWarehouseIds() == null || scope.getWarehouseIds().isEmpty()) {
            return false;
        }
        try {
            return scope.getWarehouseIds().contains(Long.parseLong(warehouseId));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<PerimeterEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .sorted(Comparator.comparing(PerimeterEventRespDTO::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<PerimeterEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                            PerimeterEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<PerimeterEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                          PerimeterEventQueryReqDTO query, int limit) {
        List<PerimeterEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<PerimeterEventRespDTO> filter(AiVisionDataScope scope,
                                               PerimeterEventQueryReqDTO query) {
        PerimeterEventQueryReqDTO q = query == null ? new PerimeterEventQueryReqDTO() : query;
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .filter(e -> q.getEventNo() == null || q.getEventNo().isBlank()
                || e.getEventNo().contains(q.getEventNo()))
            .filter(e -> q.getWarehouseId() == null
                || String.valueOf(q.getWarehouseId()).equals(e.getWarehouseId()))
            .filter(e -> q.getZone() == null || q.getZone().isBlank()
                || q.getZone().equals(e.getZone()))
            .filter(e -> q.getCameraCode() == null || q.getCameraCode().isBlank()
                || e.getCameraCode().contains(q.getCameraCode()))
            .filter(e -> q.getIntrusionType() == null || q.getIntrusionType().isBlank()
                || q.getIntrusionType().equals(e.getIntrusionType()))
            .filter(e -> q.getProcessStatus() == null || q.getProcessStatus().isBlank()
                || q.getProcessStatus().equals(e.getProcessStatus()))
            .sorted(Comparator.comparing(PerimeterEventRespDTO::getOccurredAt).reversed())
            .toList();
    }

    @Override
    public PerimeterEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        PerimeterEventRespDTO base = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (base == null) {
            return null;
        }
        PerimeterEventDetailRespDTO detail = new PerimeterEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setEventDesc("周界围栏检测到人员翻越，越线行为持续超过阈值");
        detail.setDurationSeconds(120L);
        detail.setIsSimulated(true);

        PerimeterEvidenceRespDTO ev = new PerimeterEvidenceRespDTO();
        ev.setEvidenceId(eventId + "-1");
        ev.setEvidenceType(PerimeterIntrusionStatusEnum.EvidenceTypeEnum.INTRUSION_FRAME.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        ev.setOriginalUrl("mock://perimeter/" + eventId + "/intrusion");
        ev.setThumbnailUrl("mock://perimeter/" + eventId + "/thumb");
        ev.setCapturedAt(base.getOccurredAt());
        ev.setCameraCode(base.getCameraCode());
        ev.setSortNo(1);
        detail.setEvidences(new ArrayList<>(List.of(ev)));
        return detail;
    }

    @Override
    public PerimeterEventRespDTO getEventSummary(AiVisionDataScope scope, String eventId) {
        return events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        return records.stream().filter(r -> r.getRecordId().endsWith(eventId)).toList();
    }

    @Override
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String processResult, String processOpinion,
                            String processedBy) {
        PerimeterEventRespDTO target = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (target == null || eventVersion == null || !eventVersion.equals(target.getEventVersion())) {
            return 0;
        }
        target.setProcessStatus(processResult);
        target.setProcessedBy(processedBy);
        target.setProcessedAt(TS.format(LocalDateTime.now()));
        target.setProcessOpinion(processOpinion);
        target.setEventVersion(eventVersion + 1);

        ProcessRecordRespDTO record = new ProcessRecordRespDTO();
        record.setRecordId("R-" + eventId + "-" + records.size());
        record.setStatusBefore(statusBefore);
        record.setStatusAfter(processResult);
        record.setProcessResult(processResult);
        record.setProcessOpinion(processOpinion);
        record.setProcessedBy(processedBy);
        record.setProcessedAt(TS.format(LocalDateTime.now()));
        record.setSource("WEB");
        records.add(record);
        return 1;
    }

    /* ==================== 算法任务 / 重新识别（内存兜底） ==================== */

    @Override
    public Long insertAlgorithmTask(AiPerimeterAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        if (task.getId() == null) {
            task.setId(taskSeq.incrementAndGet());
        }
        algorithmTasks.add(task);
        return task.getId();
    }

    @Override
    public AiPerimeterAlgorithmTask getAlgorithmTaskByNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        return algorithmTasks.stream()
            .filter(t -> taskNo.equals(t.getTaskNo()))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<AiPerimeterAlgorithmTask> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit) {
        if (eventId == null || eventId.isBlank()) {
            return List.of();
        }
        return algorithmTasks.stream()
            .filter(t -> eventId.equals(String.valueOf(t.getEventId())))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .sorted(Comparator.comparing(
                (AiPerimeterAlgorithmTask t) -> t.getStartedAt() != null ? t.getStartedAt() : t.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(0, limit))
            .toList();
    }

    /** Service 持有并原地修改 find 返回的同一实例，列表已同步；此处只兜底保证行存在。 */
    @Override
    public int updateAlgorithmTaskResult(AiPerimeterAlgorithmTask task) {
        if (task == null || task.getTaskNo() == null) {
            return 0;
        }
        boolean exists = algorithmTasks.stream().anyMatch(t -> t.getTaskNo().equals(task.getTaskNo()));
        if (!exists) {
            algorithmTasks.add(task);
        }
        return 1;
    }

    /** 只写算法侧字段；process_status 与 event_version 不被动算法链路推进。 */
    @Override
    public int writeBackEventAlgorithmResult(Long eventId, Long taskId, String algorithmStatus,
                                             String algorithmResult, BigDecimal confidence,
                                             String intrusionType, String updatedBy,
                                             LocalDateTime updatedAt) {
        if (eventId == null) {
            return 0;
        }
        PerimeterEventRespDTO target = events.stream()
            .filter(e -> String.valueOf(eventId).equals(e.getEventId()))
            .findFirst()
            .orElse(null);
        if (target == null) {
            return 0;
        }
        target.setAlgorithmStatus(algorithmStatus);
        target.setAlgorithmResult(algorithmResult);
        if (confidence != null) {
            target.setConfidence(confidence);
        }
        if (intrusionType != null && !intrusionType.isBlank()) {
            target.setIntrusionType(intrusionType);
            try {
                target.setIntrusionTypeName(
                    PerimeterIntrusionStatusEnum.IntrusionTypeEnum.valueOf(intrusionType).getLabel());
            } catch (IllegalArgumentException ignored) {
                // 非法编码保留原名称
            }
        }
        return 1;
    }
}
