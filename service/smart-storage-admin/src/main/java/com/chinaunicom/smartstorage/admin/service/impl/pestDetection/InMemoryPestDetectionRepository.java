package com.chinaunicom.smartstorage.admin.service.impl.pestDetection;

import com.chinaunicom.smartstorage.admin.constant.PestDetectionConstant;
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
import com.chinaunicom.smartstorage.admin.enumbean.PestDetectionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.PestDetectionRepository;
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
 * 粮面虫害数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳时间每次调用时基于当前时刻计算，原因见 {@link #heartbeat(int)}。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryPestDetectionRepository implements PestDetectionRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[][] WAREHOUSES = {
        {"9001", "一号仓"}, {"9002", "二号仓"}, {"9003", "三号仓"}, {"9004", "四号仓"},
    };

    /** 粮种轮转，卡片标题形如「三号仓 · 小麦5000吨」 */
    private static final String[] GRAIN_TYPES = {"小麦", "玉米", "稻谷"};
    private static final String[] GRAIN_TONNAGES = {"5000", "3000", "6000", "8000"};

    private static final String[] SAMPLE_POSITIONS = {
        "东墙地上层", "中墙下层", "西北墙中层", "东墙混上层", "南墙表层", "中央取样点",
    };

    private static final String[] INSECT_SPECIES = {"玉米象", "赤拟谷盗", "谷蠹", "麦蛾"};

    private static final String[] CONCLUSION_CODES = {"GRAIN_INSECT", "DENSITY_HIGH", "MOLD", "OTHER"};
    private static final String[] CONCLUSION_NAMES = {"粮面害虫", "密度超标", "霉变", "其他或未知"};

    private final List<PestEventRespDTO> events = new CopyOnWriteArrayList<>();
    private final List<ProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /** 「重新识别」链路的内存任务存储（与真实库行的形状一致，只是不落库） */
    private final List<AiPestAlgorithmTask> algorithmTasks = new CopyOnWriteArrayList<>();

    /** 内存实现的任务发号器：真实库由 Service 侧雪花发号，这里仅需进程内唯一 */
    private final AtomicLong algorithmTaskSeq = new AtomicLong(1);

    public InMemoryPestDetectionRepository() {
        seed();
    }

    /** 生成 24 条采样检测记录，覆盖三态处理状态与密度超标判定 */
    private void seed() {
        for (int i = 0; i < 24; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            String conclusionCode = CONCLUSION_CODES[i % CONCLUSION_CODES.length];
            BigDecimal threshold = PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD;
            // 密度超标类事件密度 > 阈值，其余 <= 阈值
            BigDecimal density = "DENSITY_HIGH".equals(conclusionCode)
                ? threshold.add(new BigDecimal("2.00"))
                : new BigDecimal(i % 3).add(new BigDecimal("0.50"));

            // 处理状态分布：每 3 条留 1 条未核实，其余在处理中/已处理间轮转
            String processStatus;
            if (i % 3 == 0) {
                processStatus = PestDetectionStatusEnum.ProcessStatusEnum.PENDING.name();
            } else if (i % 3 == 1) {
                processStatus = PestDetectionStatusEnum.ProcessStatusEnum.PROCESSING.name();
            } else {
                processStatus = PestDetectionStatusEnum.ProcessStatusEnum.DONE.name();
            }

            PestEventRespDTO e = new PestEventRespDTO();
            e.setEventId(String.valueOf(2000 + i));
            e.setEventNo(PestDetectionConstant.EVENT_NO_PREFIX
                + TS.format(LocalDateTime.now().minusHours(i)).replace("-", "").replace(":", "").substring(0, 8)
                + String.format("%04d", i + 1));
            e.setOccurredAt(TS.format(LocalDateTime.now().minusMinutes(40L * i)));
            e.setWarehouseId(wh[0]);
            e.setWarehouseName(wh[1]);
            e.setPointId(String.valueOf(100 + i % 9));
            e.setCameraCode(String.format("CAM-%03d", i % 9 + 1));
            e.setSamplePosition(SAMPLE_POSITIONS[i % SAMPLE_POSITIONS.length]);
            e.setGrainType(GRAIN_TYPES[i % GRAIN_TYPES.length]);
            e.setConclusionType(conclusionCode);
            e.setConclusionTypeName(CONCLUSION_NAMES[i % CONCLUSION_NAMES.length]);
            e.setInsectSpecies("MOLD".equals(conclusionCode) || "OTHER".equals(conclusionCode)
                ? null : INSECT_SPECIES[i % INSECT_SPECIES.length]);
            e.setDensity(density);
            e.setDensityThreshold(threshold);
            e.setAlgorithmStatus(PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            e.setAlgorithmResult(PestDetectionStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            e.setConfidence(i % 7 == 3 ? null : new BigDecimal("0.8500"));
            e.setProcessStatus(processStatus);
            e.setEventVersion(1);
            if (!PestDetectionStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                e.setProcessedBy("系统");
                e.setProcessedAt(TS.format(LocalDateTime.now().minusMinutes(15L * i)));
                e.setProcessOpinion(PestDetectionConstant.AUTO_VIEW_OPINION);
            }
            events.add(e);

            // 每条记录配一条历史算法任务，状态分布与 07_seed_algorithm_tasks.sql 一致：
            // 留出 进行中/失败/超时 样本，兜底模式下详情弹窗与「重新识别」都有内容。
            AiPestAlgorithmTask task = seedAlgorithmTask(e, i);
            algorithmTasks.add(task);
            e.setAlgorithmStatus(task.getExecutionStatus());
            e.setAlgorithmResult(task.getConclusion());
            e.setConfidence(task.getConfidence());
        }
    }

    /**
     * 内存兜底模式的历史算法任务。
     *
     * <p>状态分配（与真实库种子同口径）：第 24 条 RUNNING、第 7 条 FAILED、
     * 第 14 条 TIMEOUT，其余 SUCCEEDED。i%7==3 的成功任务置信度留空，
     * 与事件侧同形，用于验证页面「暂无」而不是 0%。</p>
     */
    private AiPestAlgorithmTask seedAlgorithmTask(PestEventRespDTO e, int index) {
        String status = PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name();
        String errorCode = null;
        String errorMessage = null;
        if (index == 6) {
            status = PestDetectionStatusEnum.AlgorithmStatusEnum.FAILED.name();
            errorCode = "ALGORITHM_ERROR";
            errorMessage = "模拟算法执行失败（历史样例，非真实故障）";
        } else if (index == 13) {
            status = PestDetectionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name();
            errorCode = "ALGORITHM_TIMEOUT";
            errorMessage = "模拟算法执行超时（历史样例，非真实故障）";
        } else if (index == 23) {
            status = PestDetectionStatusEnum.AlgorithmStatusEnum.RUNNING.name();
        }

        boolean succeeded = PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);
        boolean started = succeeded || errorCode != null;
        LocalDateTime occurred = parseTs(e.getOccurredAt());
        LocalDateTime startedAt = started ? occurred.plusSeconds(30) : LocalDateTime.now().minusMinutes(2);
        long durationMs = 18L + (index * 53L) % 403L;
        LocalDateTime finishedAt = succeeded ? startedAt.plusNanos(durationMs * 1_000_000L) : null;
        BigDecimal confidence = succeeded ? e.getConfidence() : null;

        String conclusion = null;
        String conclusionType = null;
        String detections = null;
        if (succeeded) {
            conclusionType = e.getConclusionType();
            conclusion = "OTHER".equals(conclusionType)
                ? PestDetectionStatusEnum.AlgorithmResultEnum.UNCERTAIN.name()
                : PestDetectionStatusEnum.AlgorithmResultEnum.ANOMALY.name();
            detections = seedDetections(e, confidence);
        }

        AiPestAlgorithmTask t = new AiPestAlgorithmTask();
        t.setId(51000L + index);
        t.setTaskNo("PSTASK" + TS.format(startedAt).replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
            + String.format("%04d", index + 1));
        t.setEventId(parseLongSafe(e.getEventId()));
        t.setPointId(parseLongSafe(e.getPointId()));
        t.setCameraId("C" + String.format("%03d", index % 9 + 1));
        t.setCameraCode(e.getCameraCode());
        t.setWarehouseId(parseLongSafe(e.getWarehouseId()));
        t.setRegionId("000000");
        t.setTaskType(PestDetectionConstant.ALGORITHM_TASK_TYPE);
        t.setMediaType(PestDetectionConstant.ALGORITHM_MEDIA_TYPE);
        t.setMediaRefs("[\"mock://pest/" + e.getEventId() + "/frame\","
            + "\"mock://pest/" + e.getEventId() + "/normal\"]");
        t.setNormalReferenceRef("mock://pest/" + e.getEventId() + "/normal");
        t.setRoi("[0,0,1920,1080]");
        t.setExecutionStatus(status);
        t.setConclusion(conclusion);
        t.setConclusionType(conclusionType);
        t.setConfidence(confidence);
        t.setModelName("mock-pest-detector");
        t.setModelVersion("mock-1.0.0");
        t.setAlgorithmVersion("mock-adapter-1.0.0");
        t.setConfigVersion(PestDetectionConstant.ALGORITHM_CONFIG_VERSION);
        t.setConfidenceThreshold(new BigDecimal("0.7500"));
        t.setMediaDigest("sha256:seedtask" + String.format("%04d", index + 1));
        t.setStartedAt(startedAt);
        t.setFinishedAt(finishedAt);
        t.setInferenceDurationMs(started ? durationMs : null);
        t.setDetections(detections);
        t.setEvidenceRefs("[{\"evidenceType\":\"PEST_FRAME\","
            + "\"objectKey\":\"mock://pest/" + e.getEventId() + "/frame\"}]");
        t.setRawResultRef("mock://task/seed" + String.format("%04d", index + 1) + "/raw");
        t.setErrorCode(errorCode);
        t.setErrorMessage(errorMessage);
        t.setRetryCount("ALGORITHM_ERROR".equals(errorCode) ? 2 : 0);
        t.setTraceId("seedtask-" + String.format("%04d", index + 1));
        t.setIsSimulated(Boolean.TRUE);
        t.setCreatedBy("seed-memory");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedBy("seed-memory");
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    /** 检出框与虫种/密度自洽：霉变给 mold_patch，OTHER 不伪造检出 */
    private String seedDetections(PestEventRespDTO e, BigDecimal confidence) {
        String conclusionType = e.getConclusionType();
        if ("OTHER".equals(conclusionType)) {
            return "[]";
        }
        BigDecimal density = e.getDensity() == null ? BigDecimal.ZERO : e.getDensity();
        String bbox = "MOLD".equals(conclusionType) ? "\"[300,150,900,600]\"" : "\"[120,80,460,420]\"";
        String category = "MOLD".equals(conclusionType) ? "mold_patch" : "grain_insect";
        String species = "MOLD".equals(conclusionType) || e.getInsectSpecies() == null
            ? "" : ",\"species\":\"" + e.getInsectSpecies() + "\"";
        String conf = confidence == null ? "0.9000" : confidence.toPlainString();
        return "[{\"category\":\"" + category + "\"" + species
            + ",\"count\":" + density.stripTrailingZeros().toPlainString()
            + ",\"confidence\":" + conf + ",\"bbox\":" + bbox + "}]";
    }

    private LocalDateTime parseTs(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, TS);
        } catch (Exception ex) {
            return LocalDateTime.now();
        }
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 粮面点位状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值
     * 判定心跳超时，若在这里固定时间戳，进程跑久了全部点位会被误判为离线，
     * 正常/告警状态就永远演示不出来。</p>
     */
    @Override
    public List<MonitorPointRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        List<MonitorPointRespDTO> list = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            if (!allows(scope, wh[0])) {
                continue;
            }
            MonitorPointRespDTO p = new MonitorPointRespDTO();
            p.setPointId(String.valueOf(100 + i));
            p.setPointName(wh[1] + "粮面");
            p.setWarehouseId(wh[0]);
            p.setWarehouseName(wh[1]);
            p.setGrainType(GRAIN_TYPES[i % GRAIN_TYPES.length]);
            p.setGrainTonnage(new BigDecimal(GRAIN_TONNAGES[i % GRAIN_TONNAGES.length]));
            p.setCameraCode(String.format("CAM-%03d", i + 1));
            p.setPlatformCameraId("C" + String.format("%03d", i + 1));
            // 状态分布：多数在线，少量离线与未知，其中两路处于告警
            if (i == 1 || i == 7) {
                p.setDeviceStatus(PestDetectionStatusEnum.DeviceStatusEnum.OFFLINE.name());
                p.setAlarmStatus(PestDetectionStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setLastHeartbeatAt(heartbeat(200));
            } else if (i == 8) {
                p.setDeviceStatus(PestDetectionStatusEnum.DeviceStatusEnum.UNKNOWN.name());
                p.setAlarmStatus(PestDetectionStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setLastHeartbeatAt(null);
            } else {
                p.setDeviceStatus(PestDetectionStatusEnum.DeviceStatusEnum.ONLINE.name());
                p.setAlarmStatus(i == 2 || i == 5
                    ? PestDetectionStatusEnum.AlarmStatusEnum.ACTIVE.name()
                    : PestDetectionStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setLastHeartbeatAt(heartbeat(1 + i % 5));
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
    public List<PestEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .sorted(Comparator.comparing(PestEventRespDTO::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<PestEventRespDTO> listEventByCondition(AiVisionDataScope scope, PestEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<PestEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                     PestEventQueryReqDTO query, int limit) {
        List<PestEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<PestEventRespDTO> filter(AiVisionDataScope scope, PestEventQueryReqDTO query) {
        PestEventQueryReqDTO q = query == null ? new PestEventQueryReqDTO() : query;
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .filter(e -> q.getEventNo() == null || q.getEventNo().isBlank()
                || e.getEventNo().contains(q.getEventNo()))
            .filter(e -> q.getWarehouseId() == null
                || String.valueOf(q.getWarehouseId()).equals(e.getWarehouseId()))
            .filter(e -> q.getPointId() == null
                || String.valueOf(q.getPointId()).equals(e.getPointId()))
            .filter(e -> q.getCameraCode() == null || q.getCameraCode().isBlank()
                || e.getCameraCode().contains(q.getCameraCode()))
            .filter(e -> q.getSamplePosition() == null || q.getSamplePosition().isBlank()
                || e.getSamplePosition().contains(q.getSamplePosition()))
            .filter(e -> q.getConclusionType() == null || q.getConclusionType().isBlank()
                || q.getConclusionType().equals(e.getConclusionType()))
            .filter(e -> q.getProcessStatus() == null || q.getProcessStatus().isBlank()
                || q.getProcessStatus().equals(e.getProcessStatus()))
            .sorted(Comparator.comparing(PestEventRespDTO::getOccurredAt).reversed())
            .toList();
    }

    @Override
    public PestEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        PestEventRespDTO base = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (base == null) {
            return null;
        }
        PestEventDetailRespDTO detail = new PestEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setEventDesc("粮面采样检测发现" + base.getConclusionTypeName()
            + "，密度 " + base.getDensity() + " 头/kg，阈值 " + base.getDensityThreshold() + " 头/kg");
        detail.setDurationSeconds(0L);
        detail.setIsSimulated(true);
        detail.setDensityExceeded(
            base.getDensity() != null && base.getDensityThreshold() != null
                && base.getDensity().compareTo(base.getDensityThreshold()) > 0);

        PestEvidenceRespDTO ev = new PestEvidenceRespDTO();
        ev.setEvidenceId(eventId + "-1");
        ev.setEvidenceType(PestDetectionStatusEnum.EvidenceTypeEnum.PEST_FRAME.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        ev.setOriginalUrl("mock://pest/" + eventId + "/frame");
        ev.setThumbnailUrl("mock://pest/" + eventId + "/thumb");
        ev.setCapturedAt(base.getOccurredAt());
        ev.setCameraCode(base.getCameraCode());
        ev.setSortNo(1);
        detail.setEvidences(new ArrayList<>(List.of(ev)));
        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        return records.stream().filter(r -> r.getRecordId().endsWith(eventId)).toList();
    }

    @Override
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String processResult, String processOpinion,
                            String processedBy) {
        PestEventRespDTO target = events.stream()
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

    /* ==================== 算法任务（「重新识别」链路） ==================== */

    @Override
    public Long insertAlgorithmTask(AiPestAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(900000000L + algorithmTaskSeq.getAndIncrement());
        }
        algorithmTasks.add(task);
        return task.getId();
    }

    @Override
    public PestAlgorithmTaskRespDTO findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        return algorithmTasks.stream()
            .filter(t -> taskNo.equals(t.getTaskNo()))
            .filter(t -> allows(scope, t.getWarehouseId() == null ? null : String.valueOf(t.getWarehouseId())))
            .findFirst()
            .map(this::toTaskResp)
            .orElse(null);
    }

    @Override
    public List<PestAlgorithmTaskRespDTO> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit) {
        // 事件归属必须先在数据范围内，与 MyBatis 实现同一防御口径
        boolean eventVisible = events.stream()
            .anyMatch(e -> e.getEventId().equals(eventId) && allows(scope, e.getWarehouseId()));
        if (!eventVisible) {
            return List.of();
        }
        return algorithmTasks.stream()
            .filter(t -> eventId.equals(String.valueOf(t.getEventId())))
            .sorted(Comparator.comparing(AiPestAlgorithmTask::getStartedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(1, limit))
            .map(this::toTaskResp)
            .toList();
    }

    @Override
    public void saveAlgorithmTaskResult(AiPestAlgorithmTask task, PestAlgorithmWriteBackDTO writeBack) {
        algorithmTasks.stream()
            .filter(t -> t.getId() != null && t.getId().equals(task.getId()))
            .findFirst()
            .ifPresent(existing -> {
                existing.setExecutionStatus(task.getExecutionStatus());
                existing.setConclusion(task.getConclusion());
                existing.setConclusionType(task.getConclusionType());
                existing.setConfidence(task.getConfidence());
                existing.setFinishedAt(task.getFinishedAt());
                existing.setInferenceDurationMs(task.getInferenceDurationMs());
                if (task.getDetections() != null) {
                    existing.setDetections(task.getDetections());
                }
                if (task.getModelName() != null) {
                    existing.setModelName(task.getModelName());
                    existing.setModelVersion(task.getModelVersion());
                    existing.setAlgorithmVersion(task.getAlgorithmVersion());
                }
                existing.setErrorCode(task.getErrorCode());
                existing.setErrorMessage(task.getErrorMessage());
                existing.setIsSimulated(task.getIsSimulated());
                existing.setUpdatedAt(task.getUpdatedAt());
            });

        if (writeBack == null || writeBack.getEventId() == null) {
            return;
        }
        if (!writeBack.isScopeUnrestricted()
                && (writeBack.getScopeWarehouseIds() == null || writeBack.getScopeWarehouseIds().isEmpty())) {
            return;
        }
        events.stream()
            .filter(e -> String.valueOf(writeBack.getEventId()).equals(e.getEventId()))
            .findFirst()
            .ifPresent(e -> {
                e.setAlgorithmStatus(writeBack.getAlgorithmStatus());
                e.setAlgorithmResult(writeBack.getAlgorithmResult() != null
                    ? writeBack.getAlgorithmResult() : e.getAlgorithmResult());
                e.setConfidence(writeBack.getConfidence());
                if (writeBack.getConclusionType() != null) {
                    e.setConclusionType(writeBack.getConclusionType());
                    e.setConclusionTypeName(writeBack.getConclusionTypeName());
                    e.setInsectSpecies(writeBack.getInsectSpecies());
                    e.setDensity(writeBack.getDensity());
                }
            });
    }

    /** 任务实体 → 响应对象；时间格式与真实库 SQL 侧 to_char 完全同形 */
    private PestAlgorithmTaskRespDTO toTaskResp(AiPestAlgorithmTask t) {
        PestAlgorithmTaskRespDTO r = new PestAlgorithmTaskRespDTO();
        r.setTaskId(t.getTaskNo());
        r.setId(t.getId() == null ? null : String.valueOf(t.getId()));
        r.setTaskNo(t.getTaskNo());
        r.setEventId(t.getEventId() == null ? null : String.valueOf(t.getEventId()));
        r.setPointId(t.getPointId() == null ? null : String.valueOf(t.getPointId()));
        r.setCameraCode(t.getCameraCode());
        r.setTaskType(t.getTaskType());
        r.setExecutionStatus(t.getExecutionStatus());
        r.setConclusion(t.getConclusion());
        r.setConclusionType(t.getConclusionType());
        r.setConfidence(t.getConfidence());
        r.setConfidenceThreshold(t.getConfidenceThreshold());
        r.setModelName(t.getModelName());
        r.setModelVersion(t.getModelVersion());
        r.setAlgorithmVersion(t.getAlgorithmVersion());
        r.setConfigVersion(t.getConfigVersion());
        r.setStartedAt(t.getStartedAt() == null ? null : TS.format(t.getStartedAt()));
        r.setFinishedAt(t.getFinishedAt() == null ? null : TS.format(t.getFinishedAt()));
        r.setInferenceDurationMs(t.getInferenceDurationMs());
        r.setDetectedCount(countDetectionBoxes(t.getDetections()));
        r.setDetections(t.getDetections());
        r.setMediaRefs(t.getMediaRefs());
        r.setSimulated(t.getIsSimulated());
        r.setErrorCode(t.getErrorCode());
        r.setErrorMessage(t.getErrorMessage());
        r.setTraceId(t.getTraceId());
        return r;
    }

    /**
     * 由 detections JSON 字符串统计目标框数量。
     *
     * <p>内存兜底不引入 JSON 库：数组以 {@code [} 开头且非空时，
     * 按对象起始 {@code &#123;} 计数即可；空串/null 视为「未产出」返回 null。</p>
     */
    private Integer countDetectionBoxes(String detections) {
        if (detections == null || detections.isBlank() || "[]".equals(detections.trim())) {
            return detections != null && detections.trim().equals("[]") ? 0 : null;
        }
        if (!detections.trim().startsWith("[")) {
            return null;
        }
        return (int) detections.chars().filter(c -> c == '{').count();
    }
}
