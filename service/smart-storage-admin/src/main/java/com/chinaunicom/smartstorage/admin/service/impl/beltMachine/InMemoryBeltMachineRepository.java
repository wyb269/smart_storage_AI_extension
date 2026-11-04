package com.chinaunicom.smartstorage.admin.service.impl.beltMachine;

import com.chinaunicom.smartstorage.admin.constant.BeltMachineConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.beltMachine.BeltMachineAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.BeltMachineStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.beltMachine.BeltMachineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 皮带机下站人数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳时间每次调用时基于当前时刻计算，原因见 {@link #heartbeat(int)}。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryBeltMachineRepository implements BeltMachineRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[][] WAREHOUSES = {
        {"9001", "一号粮库"}, {"9002", "二号粮库"}, {"9003", "三号粮库"}, {"9004", "散粮库"},
    };

    /** 卡片标题：一号皮带机 / 二号皮带机 …，与原型图左侧网格一致 */
    private static final String[] CN_NUM = {
        "一号", "二号", "三号", "四号", "五号", "六号",
        "七号", "八号", "九号", "十号", "十一号", "十二号",
    };

    /** 皮带机所在作业区，同时作为右侧实时监控标题的摄像头位置 */
    private static final String[] AREAS = {
        "一号仓库作业区", "二号仓库作业区", "三号仓库作业区", "罩棚作业区",
        "工作塔一层", "卸粮坑", "主通廊东段", "主通廊西段",
        "铁路专用线", "汽车接收棚", "烘干塔进料口", "浅圆仓B区",
    };

    private final List<BeltMachineEventRespDTO> events = new CopyOnWriteArrayList<>();
    private final List<BeltMachineProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /**
     * 算法任务内存存储。
     *
     * <p>与 MyBatis 实现同语义：任务对象按引用存放，Service 轮询链路
     * 先经带范围的 {@link #getAlgorithmTaskByNo} 取回行、改写后再调
     * {@link #updateAlgorithmTask}，这里落库退化为「确认行存在」。</p>
     */
    private final List<BeltMachineAlgorithmTask> algorithmTasks = new CopyOnWriteArrayList<>();

    /** 已补过历史任务的事件标识，避免重复生成 */
    private final java.util.Set<String> seededTaskEvents = ConcurrentHashMap.newKeySet();

    private final java.util.concurrent.atomic.AtomicLong taskSeq =
        new java.util.concurrent.atomic.AtomicLong(900000L);

    public InMemoryBeltMachineRepository() {
        seed();
    }

    /** 生成 40 条皮带机下站人事件（点位见 {@link #listMonitorPoint}） */
    private void seed() {
        BeltMachineStatusEnum.ConclusionTypeEnum[] types =
            BeltMachineStatusEnum.ConclusionTypeEnum.values();
        for (int i = 0; i < 40; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            BeltMachineStatusEnum.ConclusionTypeEnum type = types[i % types.length];
            // 三值处理状态：每 4 条留 1 条未核实，每 7 条留 1 条处理中
            String processStatus;
            if (i % 4 == 0) {
                processStatus = BeltMachineStatusEnum.ProcessStatusEnum.PENDING.name();
            } else if (i % 7 == 2) {
                processStatus = BeltMachineStatusEnum.ProcessStatusEnum.PROCESSING.name();
            } else {
                processStatus = BeltMachineStatusEnum.ProcessStatusEnum.DONE.name();
            }

            BeltMachineEventRespDTO e = new BeltMachineEventRespDTO();
            e.setEventId(String.valueOf(2000 + i));
            e.setEventNo(BeltMachineConstant.EVENT_NO_PREFIX
                + TS.format(LocalDateTime.now().minusHours(i)).replace("-", "").replace(":", "")
                    .substring(0, 8)
                + String.format("%04d", i + 1));
            e.setOccurredAt(TS.format(LocalDateTime.now().minusMinutes(30L * i)));
            e.setWarehouseId(wh[0]);
            e.setWarehouseName(wh[1]);
            e.setPointId(String.valueOf(101 + i % 12));
            e.setDeviceCode(String.format("BELT-%03d", i % 12 + 1));
            e.setDeviceName(CN_NUM[i % 12] + "皮带机");
            e.setCameraCode(String.format("CAM-%03d", i % 12 + 1));
            e.setConclusionType(type.name());
            e.setConclusionTypeName(type.getLabel());
            e.setPersonCount(i % 6 == 3 ? null : 1 + i % 3);
            e.setAlgorithmStatus(BeltMachineStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            e.setAlgorithmResult(BeltMachineStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            e.setConfidence(i % 7 == 3 ? null : new BigDecimal("0.8500"));
            e.setProcessStatus(processStatus);
            e.setEventVersion(1);
            if (!BeltMachineStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                e.setProcessedBy("系统");
                e.setProcessedAt(TS.format(LocalDateTime.now().minusMinutes(10L * i)));
                e.setProcessOpinion(BeltMachineConstant.AUTO_VIEW_OPINION);
            }
            events.add(e);
        }
    }

    /**
     * 皮带机点位状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值
     * 判定心跳超时，若在这里固定时间戳，进程跑久了全部点位会被误判为离线，
     * 正常/报警状态就永远演示不出来。</p>
     *
     * <p>状态分布覆盖原型图全部四态：正常 / 报警 / 离线 / 未部署。
     * 其中「未部署」= {@code deviceStatus=UNKNOWN} 且 {@code cameraId} 为空。</p>
     */
    @Override
    public List<BeltMachineMonitorPointRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        List<BeltMachineMonitorPointRespDTO> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            if (!allows(scope, wh[0])) {
                continue;
            }
            BeltMachineMonitorPointRespDTO p = new BeltMachineMonitorPointRespDTO();
            // 点位标识与 04_seed 脚本的 100 + i（i 从 1 起）对齐，
            // 便于切到真库后逐行对照
            p.setPointId(String.valueOf(101 + i));
            p.setDeviceCode(String.format("BELT-%03d", i + 1));
            p.setDeviceName(CN_NUM[i] + "皮带机");
            p.setAreaName(AREAS[i]);
            p.setWarehouseId(wh[0]);
            p.setWarehouseName(wh[1]);

            // 第 10、12 台未部署：没绑摄像头，也从未接入
            boolean notDeployed = (i == 9 || i == 11);
            if (notDeployed) {
                p.setCameraId(null);
                p.setCameraCode(null);
                p.setCameraName(null);
                p.setDeviceStatus(BeltMachineStatusEnum.DeviceStatusEnum.UNKNOWN.name());
                p.setAlarmStatus(BeltMachineStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setLastHeartbeatAt(null);
            } else {
                p.setCameraId("C" + String.format("%03d", i + 1));
                p.setCameraCode(String.format("CAM-%03d", i + 1));
                p.setCameraName(AREAS[i]);
                // 第 5、8 台离线（心跳也已陈旧）；第 3、6 台在线且报警中；其余在线正常
                if (i == 4 || i == 7) {
                    p.setDeviceStatus(BeltMachineStatusEnum.DeviceStatusEnum.OFFLINE.name());
                    p.setAlarmStatus(BeltMachineStatusEnum.AlarmStatusEnum.NORMAL.name());
                    p.setLastHeartbeatAt(heartbeat(200));
                } else {
                    p.setDeviceStatus(BeltMachineStatusEnum.DeviceStatusEnum.ONLINE.name());
                    p.setAlarmStatus(i == 2 || i == 5
                        ? BeltMachineStatusEnum.AlarmStatusEnum.ACTIVE.name()
                        : BeltMachineStatusEnum.AlarmStatusEnum.NORMAL.name());
                    p.setLastHeartbeatAt(heartbeat(1 + i % 5));
                }
            }
            p.setLatestEventAt(i % 3 == 0 ? heartbeat(60) : null);
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
    public List<BeltMachineEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .sorted(Comparator.comparing(BeltMachineEventRespDTO::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<BeltMachineEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                              BeltMachineEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<BeltMachineEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                            BeltMachineEventQueryReqDTO query, int limit) {
        List<BeltMachineEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<BeltMachineEventRespDTO> filter(AiVisionDataScope scope,
                                                 BeltMachineEventQueryReqDTO query) {
        BeltMachineEventQueryReqDTO q = query == null ? new BeltMachineEventQueryReqDTO() : query;
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .filter(e -> q.getEventNo() == null || q.getEventNo().isBlank()
                || e.getEventNo().contains(q.getEventNo()))
            .filter(e -> q.getWarehouseId() == null
                || String.valueOf(q.getWarehouseId()).equals(e.getWarehouseId()))
            .filter(e -> q.getPointId() == null
                || String.valueOf(q.getPointId()).equals(e.getPointId()))
            .filter(e -> q.getDeviceName() == null || q.getDeviceName().isBlank()
                || e.getDeviceName().contains(q.getDeviceName()))
            .filter(e -> q.getCameraCode() == null || q.getCameraCode().isBlank()
                || e.getCameraCode().contains(q.getCameraCode()))
            .filter(e -> q.getConclusionType() == null || q.getConclusionType().isBlank()
                || q.getConclusionType().equals(e.getConclusionType()))
            .filter(e -> q.getProcessStatus() == null || q.getProcessStatus().isBlank()
                || q.getProcessStatus().equals(e.getProcessStatus()))
            .sorted(Comparator.comparing(BeltMachineEventRespDTO::getOccurredAt).reversed())
            .toList();
    }

    @Override
    public BeltMachineEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        BeltMachineEventRespDTO base = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (base == null) {
            return null;
        }
        BeltMachineEventDetailRespDTO detail = new BeltMachineEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setEventDesc("皮带机运行区域检测到人员停留，触发下站人告警");
        detail.setDurationSeconds(120L);
        // 内存兜底实现产出的全部是演示数据，必须显式标记
        detail.setIsSimulated(true);

        BeltMachineEvidenceRespDTO ev = new BeltMachineEvidenceRespDTO();
        ev.setEvidenceId(eventId + "-1");
        ev.setEvidenceType(BeltMachineStatusEnum.EvidenceTypeEnum.ANOMALY_FRAME.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        ev.setOriginalUrl("mock://belt/" + eventId + "/anomaly");
        ev.setAnnotatedUrl("mock://belt/" + eventId + "/annotated");
        ev.setThumbnailUrl("mock://belt/" + eventId + "/thumb");
        ev.setCapturedAt(base.getOccurredAt());
        ev.setCameraCode(base.getCameraCode());
        ev.setSortNo(1);
        detail.setEvidences(new ArrayList<>(List.of(ev)));
        return detail;
    }

    @Override
    public List<BeltMachineProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        BeltMachineEventRespDTO event = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (event == null) {
            return List.of();
        }
        return records.stream().filter(r -> r.getRecordId().endsWith(eventId)).toList();
    }

    @Override
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String processResult, String processOpinion,
                            String processedBy, String source) {
        BeltMachineEventRespDTO target = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (target == null || eventVersion == null || !eventVersion.equals(target.getEventVersion())) {
            return 0;
        }
        if (BeltMachineStatusEnum.ProcessStatusEnum.DONE.name().equals(target.getProcessStatus())) {
            return 0;
        }
        target.setProcessStatus(processResult);
        target.setProcessedBy(processedBy);
        target.setProcessedAt(TS.format(LocalDateTime.now()));
        target.setProcessOpinion(processOpinion);
        target.setEventVersion(eventVersion + 1);

        BeltMachineProcessRecordRespDTO record = new BeltMachineProcessRecordRespDTO();
        record.setRecordId("R-" + eventId + "-" + records.size());
        record.setStatusBefore(statusBefore);
        record.setStatusAfter(processResult);
        record.setProcessResult(processResult);
        record.setProcessOpinion(processOpinion);
        record.setProcessedBy(processedBy);
        record.setProcessedAt(TS.format(LocalDateTime.now()));
        record.setSource(source);
        records.add(record);
        return 1;
    }

    /* ==================== 算法任务（重新识别链路） ==================== */

    @Override
    public long insertAlgorithmTask(BeltMachineAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(taskSeq.incrementAndGet());
        }
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(now);
        }
        task.setUpdatedAt(now);
        algorithmTasks.add(task);
        return task.getId();
    }

    @Override
    public BeltMachineAlgorithmTask getAlgorithmTaskByNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        BeltMachineAlgorithmTask task = algorithmTasks.stream()
            .filter(t -> taskNo.equals(t.getTaskNo()))
            .findFirst()
            .orElse(null);
        if (task == null) {
            return null;
        }
        // 任务随事件受同一数据范围保护
        if (task.getEventId() != null) {
            BeltMachineEventRespDTO event = findEventById(String.valueOf(task.getEventId()));
            if (event != null && !allows(scope, event.getWarehouseId())) {
                return null;
            }
        }
        return task;
    }

    @Override
    public int updateAlgorithmTask(BeltMachineAlgorithmTask task) {
        if (task == null || task.getId() == null) {
            return 0;
        }
        boolean exists = algorithmTasks.stream().anyMatch(t -> Objects.equals(t.getId(), task.getId()));
        if (!exists) {
            return 0;
        }
        task.setUpdatedAt(LocalDateTime.now());
        // 内存实现按引用存放，Service 改写即已生效
        return 1;
    }

    @Override
    public List<BeltMachineAlgorithmTask> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit) {
        BeltMachineEventRespDTO event = findEventById(eventId);
        if (event == null || !allows(scope, event.getWarehouseId())) {
            return List.of();
        }
        ensureHistoryTask(event);
        return algorithmTasks.stream()
            .filter(t -> t.getEventId() != null && t.getEventId().equals(parseLong(eventId)))
            .sorted(Comparator.comparing(
                (BeltMachineAlgorithmTask t) -> t.getStartedAt() != null ? t.getStartedAt() : t.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(1, limit))
            .toList();
    }

    @Override
    public int writeBackEventAlgorithm(AiVisionDataScope scope, String eventId, Long taskId,
                                       String algorithmStatus, String algorithmResult,
                                       String conclusionType, BigDecimal confidence, String updatedBy) {
        BeltMachineEventRespDTO event = findEventById(eventId);
        if (event == null || !allows(scope, event.getWarehouseId())) {
            return 0;
        }
        // 只写算法列与结论类型，不触碰 process_* 与 eventVersion（与真实库同一约定）
        event.setAlgorithmStatus(algorithmStatus);
        event.setAlgorithmResult(algorithmResult);
        event.setConfidence(confidence);
        if (conclusionType != null && !conclusionType.isBlank()) {
            event.setConclusionType(conclusionType);
            for (BeltMachineStatusEnum.ConclusionTypeEnum value : BeltMachineStatusEnum.ConclusionTypeEnum.values()) {
                if (value.name().equals(conclusionType)) {
                    event.setConclusionTypeName(value.getLabel());
                }
            }
        }
        log.info("[BeltMachine] 内存模式回写事件算法列 eventId={} taskId={} by={}", eventId, taskId, updatedBy);
        return 1;
    }

    private BeltMachineEventRespDTO findEventById(String eventId) {
        if (eventId == null) {
            return null;
        }
        return events.stream().filter(e -> e.getEventId().equals(eventId)).findFirst().orElse(null);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 为事件补一条历史 SUCCEEDED 任务（内存兜底只演示链路，不造假统计）。
     *
     * <p>结论与置信度直接取事件现值，人数与 detections 数组自洽；
     * 全部标 is_simulated=TRUE。</p>
     */
    private void ensureHistoryTask(BeltMachineEventRespDTO event) {
        if (!seededTaskEvents.add(event.getEventId())) {
            return;
        }
        Long eventId = parseLong(event.getEventId());
        if (eventId == null) {
            return;
        }
        LocalDateTime occurred = parseTs(event.getOccurredAt());
        BeltMachineAlgorithmTask t = new BeltMachineAlgorithmTask();
        t.setId(taskSeq.incrementAndGet());
        t.setTaskNo("BAT-MEM-" + event.getEventId());
        t.setEventId(eventId);
        t.setPointId(parseLong(event.getPointId()));
        t.setCameraCode(event.getCameraCode());
        t.setTaskType("BELT_PERSON_DETECT");
        t.setMediaType("SINGLE_IMAGE");
        t.setMediaRefs("[\"mock://belt/" + event.getEventId() + "/anomaly\"]");
        t.setExecutionStatus(BeltMachineStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
        t.setConclusion(event.getAlgorithmResult());
        t.setConclusionType(event.getConclusionType());
        t.setConfidence(event.getConfidence());
        t.setModelName("mock-belt-person-detector");
        t.setModelVersion("mock-1.0.0");
        t.setAlgorithmVersion("mock-adapter-1.0.0");
        t.setConfigVersion("belt-cfg-1.0.0");
        t.setStartedAt(occurred);
        t.setFinishedAt(occurred == null ? null : occurred.plusSeconds(2));
        t.setInferenceDurationMs(120L);
        t.setDetections(buildDetectionsJson(event.getPersonCount(), event.getConfidence()));
        t.setTraceId("mem-trace-" + event.getEventId());
        t.setIsSimulated(Boolean.TRUE);
        t.setCreatedBy("seed-memory");
        t.setCreatedAt(occurred);
        algorithmTasks.add(t);
    }

    /** detections JSON：目标框数量 = personCount；人数未给出时为 null，不伪造 */
    private String buildDetectionsJson(Integer personCount, BigDecimal confidence) {
        if (personCount == null || personCount <= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int n = 0; n < personCount; n++) {
            if (n > 0) {
                sb.append(',');
            }
            sb.append("{\"category\":\"person_in_belt_zone\"")
                .append(",\"confidence\":").append(confidence == null ? "null" : confidence.toPlainString())
                .append(",\"bbox\":\"[").append(120 + 60 * n).append(",80,")
                .append(460 + 60 * n).append(",420]\"")
                .append(",\"trackId\":\"trk-mem-").append(n).append("\"}");
        }
        return sb.append(']').toString();
    }

    private LocalDateTime parseTs(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }
}
