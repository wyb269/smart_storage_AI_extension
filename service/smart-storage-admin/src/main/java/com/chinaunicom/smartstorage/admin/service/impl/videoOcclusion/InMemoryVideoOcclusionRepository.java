package com.chinaunicom.smartstorage.admin.service.impl.videoOcclusion;

import com.chinaunicom.smartstorage.admin.constant.VideoOcclusionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.videoOcclusion.AiOcclusionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.VideoOcclusionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion.VideoOcclusionRepository;
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
 * 视频遮挡数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳时间每次调用时基于当前时刻计算，原因见 {@link #heartbeat(int)}。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryVideoOcclusionRepository implements VideoOcclusionRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[][] WAREHOUSES = {
        {"9001", "一号粮库"}, {"9002", "二号粮库"}, {"9003", "三号粮库"}, {"9004", "散粮库"},
    };

    private static final String[] POSITIONS = {
        "东门", "西门", "南门", "北门", "主通道西侧", "主通道东侧", "卸粮坑", "工作塔一层",
    };

    private final List<OcclusionEventRespDTO> events = new CopyOnWriteArrayList<>();
    private final List<ProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /** 算法任务内存存储；兜底模式下同样能跑通「重新识别」全链路 */
    private final List<AiOcclusionAlgorithmTask> algorithmTasks = new CopyOnWriteArrayList<>();
    private final AtomicLong taskSequence = new AtomicLong(900_000L);

    public InMemoryVideoOcclusionRepository() {
        seed();
    }

    /** 生成 12 路摄像头与 40 条遮挡事件 */
    private void seed() {
        for (int i = 0; i < 40; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            String position = wh[1] + POSITIONS[i % POSITIONS.length];
            VideoOcclusionStatusEnum.OcclusionTypeEnum type =
                VideoOcclusionStatusEnum.OcclusionTypeEnum.values()[i % 5];
            String processStatus = i % 4 == 0
                ? VideoOcclusionStatusEnum.ProcessStatusEnum.PENDING.name()
                : VideoOcclusionStatusEnum.ProcessStatusEnum.CLOSED.name();

            OcclusionEventRespDTO e = new OcclusionEventRespDTO();
            e.setEventId(String.valueOf(2000 + i));
            e.setEventNo("VD" + TS.format(LocalDateTime.now().minusHours(i)).replace("-", "").replace(":", "").substring(0, 8)
                + String.format("%04d", i + 1));
            e.setOccurredAt(TS.format(LocalDateTime.now().minusMinutes(30L * i)));
            e.setWarehouseId(wh[0]);
            e.setWarehouseName(wh[1]);
            e.setCameraConfigId(String.valueOf(100 + i % 12));
            e.setCameraCode(String.format("CAM-%03d", i % 12 + 1));
            e.setCameraPosition(position);
            e.setOcclusionType(type.name());
            e.setOcclusionTypeName(type.getLabel());
            e.setAlgorithmStatus(VideoOcclusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            e.setAlgorithmResult(VideoOcclusionStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            e.setConfidence(i % 7 == 3 ? null : new BigDecimal("0.8500"));
            e.setProcessStatus(processStatus);
            e.setEventVersion(1);
            if (!VideoOcclusionStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                e.setProcessedBy("系统");
                e.setProcessedAt(TS.format(LocalDateTime.now().minusMinutes(10L * i)));
                e.setProcessOpinion("查看事件详情后自动标记为已处理");
            }
            events.add(e);

            // 每条事件配一条历史算法任务，状态分布与 07_seed_algorithm_tasks.sql 一致：
            // 留出 进行中/失败/超时 样本，兜底模式下详情弹窗与「重新识别」都有内容。
            AiOcclusionAlgorithmTask task = seedAlgorithmTask(e, i);
            algorithmTasks.add(task);
            e.setAlgorithmStatus(task.getExecutionStatus());
            e.setAlgorithmResult(task.getConclusion());
            e.setConfidence(task.getConfidence());
        }
    }

    /**
     * 内存兜底模式的历史算法任务。
     *
     * <p>状态分配（与真实库种子同口径）：第 12 条 QUEUED、第 32 条 RUNNING、
     * 第 18 条 FAILED、第 28 条 TIMEOUT，其余 SUCCEEDED。未成功的事件结论留空，
     * 用于验证页面「暂无」而不是伪造 NORMAL。</p>
     */
    private AiOcclusionAlgorithmTask seedAlgorithmTask(OcclusionEventRespDTO e, int index) {
        String status = VideoOcclusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name();
        String errorCode = null;
        String errorMessage = null;
        if (index == 11) {
            status = VideoOcclusionStatusEnum.AlgorithmStatusEnum.QUEUED.name();
        } else if (index == 31) {
            status = VideoOcclusionStatusEnum.AlgorithmStatusEnum.RUNNING.name();
        } else if (index == 17) {
            status = VideoOcclusionStatusEnum.AlgorithmStatusEnum.FAILED.name();
            errorCode = "FRAME_BLUR";
            errorMessage = "夜间画面噪点过高，遮挡区域置信度不足";
        } else if (index == 27) {
            status = VideoOcclusionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name();
            errorCode = "INFER_TIMEOUT";
            errorMessage = "算法执行超时，未返回结论";
        }

        boolean succeeded = VideoOcclusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);
        boolean started = succeeded || errorCode != null;
        LocalDateTime occurred = parseTs(e.getOccurredAt());
        LocalDateTime startedAt = started ? occurred.plusSeconds(2) : LocalDateTime.now().minusMinutes(2 + index);
        long durationMs = 16L + (index * 89L) % 372L;
        LocalDateTime finishedAt = succeeded ? startedAt.plusNanos(durationMs * 1_000_000L) : null;
        BigDecimal confidence = succeeded ? new BigDecimal("0.8500") : null;

        AiOcclusionAlgorithmTask t = new AiOcclusionAlgorithmTask();
        t.setId(95000L + index);
        t.setTaskNo("OCAT" + TS.format(startedAt).replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
            + String.format("%04d", index + 1));
        t.setEventId(parseLongSafe(e.getEventId()));
        t.setCameraConfigId(parseLongSafe(e.getCameraConfigId()));
        t.setCameraId("C" + String.format("%03d", index % 12 + 1));
        t.setCameraCode(e.getCameraCode());
        t.setWarehouseId(parseLongSafe(e.getWarehouseId()));
        t.setRegionId("000000");
        t.setTaskType(VideoOcclusionConstant.ALGORITHM_TASK_TYPE);
        t.setMediaType(VideoOcclusionConstant.ALGORITHM_MEDIA_TYPE);
        t.setMediaRefs("[\"mock://occlusion/" + e.getEventId() + "/occluded\"]");
        t.setNormalReferenceRef("mock://occlusion/" + e.getEventId() + "/normal");
        t.setRoi("{\"x\":0.18,\"y\":0.12,\"w\":0.64,\"h\":0.72}");
        t.setExecutionStatus(status);
        t.setConclusion(succeeded ? VideoOcclusionStatusEnum.AlgorithmResultEnum.ANOMALY.name() : null);
        t.setOcclusionType(succeeded ? e.getOcclusionType() : null);
        t.setConfidence(confidence);
        t.setModelName("occlusion-detector");
        t.setModelVersion("v1.1.0");
        t.setAlgorithmVersion("algo-2026.09.1");
        t.setConfigVersion(VideoOcclusionConstant.ALGORITHM_CONFIG_VERSION);
        t.setConfidenceThreshold(new BigDecimal(VideoOcclusionConstant.ALGORITHM_CONFIDENCE_THRESHOLD));
        t.setStartedAt(startedAt);
        t.setFinishedAt(finishedAt);
        t.setInferenceDurationMs(started ? durationMs : null);
        t.setDetections(succeeded ? "[{\"category\":\"OCCLUSION\",\"confidence\":0.85,"
            + "\"bbox\":\"[180,120,820,700]\"}]" : null);
        t.setEvidenceRefs("[{\"evidenceType\":\"OCCLUDED_FRAME\","
            + "\"objectKey\":\"mock://occlusion/" + e.getEventId() + "/occluded\"}]");
        t.setRawResultRef("mock://task/" + t.getTaskNo() + "/raw");
        t.setErrorCode(errorCode);
        t.setErrorMessage(errorMessage);
        t.setRetryCount(0);
        t.setTraceId("seed-trace-oc-" + e.getEventId());
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
     * 摄像头状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值
     * 判定心跳超时，若在这里固定时间戳，进程跑久了全部摄像头会被误判为离线，
     * 正常/遮挡状态就永远演示不出来。</p>
     */
    @Override
    public List<CameraStatusRespDTO> listCameraStatus(AiVisionDataScope scope) {
        List<CameraStatusRespDTO> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            if (!allows(scope, wh[0])) {
                continue;
            }
            CameraStatusRespDTO c = new CameraStatusRespDTO();
            c.setCameraId(String.valueOf(100 + i));
            c.setCameraCode(String.format("CAM-%03d", i + 1));
            c.setCameraName(wh[1] + POSITIONS[i % POSITIONS.length]);
            c.setWarehouseId(wh[0]);
            c.setWarehouseName(wh[1]);
            c.setPlatformCameraId("C" + String.format("%03d", i + 1));
            // 状态分布：多数在线，少量离线与未知，其中两路处于遮挡告警
            if (i == 1 || i == 8) {
                c.setDeviceStatus(VideoOcclusionStatusEnum.DeviceStatusEnum.OFFLINE.name());
                c.setOcclusionStatus(VideoOcclusionStatusEnum.OcclusionStatusEnum.NORMAL.name());
                c.setLastHeartbeatAt(heartbeat(200));
            } else if (i == 11) {
                c.setDeviceStatus(VideoOcclusionStatusEnum.DeviceStatusEnum.UNKNOWN.name());
                c.setOcclusionStatus(VideoOcclusionStatusEnum.OcclusionStatusEnum.NORMAL.name());
                c.setLastHeartbeatAt(null);
            } else {
                c.setDeviceStatus(VideoOcclusionStatusEnum.DeviceStatusEnum.ONLINE.name());
                c.setOcclusionStatus(i == 3 || i == 6
                    ? VideoOcclusionStatusEnum.OcclusionStatusEnum.ACTIVE.name()
                    : VideoOcclusionStatusEnum.OcclusionStatusEnum.NORMAL.name());
                c.setLastHeartbeatAt(heartbeat(1 + i % 5));
            }
            c.setSortNo(i + 1);
            list.add(c);
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
    public List<OcclusionEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .sorted(Comparator.comparing(OcclusionEventRespDTO::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<OcclusionEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                            OcclusionEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<OcclusionEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                          OcclusionEventQueryReqDTO query, int limit) {
        List<OcclusionEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<OcclusionEventRespDTO> filter(AiVisionDataScope scope,
                                               OcclusionEventQueryReqDTO query) {
        OcclusionEventQueryReqDTO q = query == null ? new OcclusionEventQueryReqDTO() : query;
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .filter(e -> q.getEventNo() == null || q.getEventNo().isBlank()
                || e.getEventNo().contains(q.getEventNo()))
            .filter(e -> q.getWarehouseId() == null
                || String.valueOf(q.getWarehouseId()).equals(e.getWarehouseId()))
            .filter(e -> q.getCameraCode() == null || q.getCameraCode().isBlank()
                || e.getCameraCode().contains(q.getCameraCode()))
            .filter(e -> q.getOcclusionType() == null || q.getOcclusionType().isBlank()
                || q.getOcclusionType().equals(e.getOcclusionType()))
            .filter(e -> q.getProcessStatus() == null || q.getProcessStatus().isBlank()
                || q.getProcessStatus().equals(e.getProcessStatus()))
            .sorted(Comparator.comparing(OcclusionEventRespDTO::getOccurredAt).reversed())
            .toList();
    }

    @Override
    public OcclusionEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        OcclusionEventRespDTO base = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (base == null) {
            return null;
        }
        OcclusionEventDetailRespDTO detail = new OcclusionEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setEventDesc("画面检测到镜头被遮挡，遮挡面积超过阈值");
        detail.setDurationSeconds(120L);
        detail.setIsSimulated(true);

        OcclusionEvidenceRespDTO ev = new OcclusionEvidenceRespDTO();
        ev.setEvidenceId(eventId + "-1");
        ev.setEvidenceType(VideoOcclusionStatusEnum.EvidenceTypeEnum.OCCLUDED_FRAME.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        ev.setOriginalUrl("mock://occlusion/" + eventId + "/occluded");
        ev.setThumbnailUrl("mock://occlusion/" + eventId + "/thumb");
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
        OcclusionEventRespDTO target = events.stream()
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

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    @Override
    public String insertAlgorithmTask(AiOcclusionAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        if (task.getId() == null) {
            task.setId(taskSequence.incrementAndGet());
        }
        algorithmTasks.add(task);
        return String.valueOf(task.getId());
    }

    @Override
    public OcclusionAlgorithmTaskRespDTO findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        return algorithmTasks.stream()
            .filter(t -> taskNo.equals(t.getTaskNo()))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .findFirst()
            .map(this::toTaskDto)
            .orElse(null);
    }

    @Override
    public int updateAlgorithmTask(AiOcclusionAlgorithmTask task) {
        if (task == null || task.getId() == null) {
            return 0;
        }
        AiOcclusionAlgorithmTask target = algorithmTasks.stream()
            .filter(t -> t.getId().equals(task.getId()))
            .findFirst()
            .orElse(null);
        if (target == null) {
            return 0;
        }
        // 与 SQL 回填语句同一列集：结果列整体覆盖，排队起点缺失时保留原值
        target.setExecutionStatus(task.getExecutionStatus());
        target.setConclusion(task.getConclusion());
        target.setOcclusionType(task.getOcclusionType());
        target.setConfidence(task.getConfidence());
        target.setModelName(task.getModelName());
        target.setModelVersion(task.getModelVersion());
        target.setAlgorithmVersion(task.getAlgorithmVersion());
        target.setConfigVersion(task.getConfigVersion());
        if (task.getStartedAt() != null) {
            target.setStartedAt(task.getStartedAt());
        }
        target.setFinishedAt(task.getFinishedAt());
        target.setInferenceDurationMs(task.getInferenceDurationMs());
        target.setDetections(task.getDetections());
        target.setEvidenceRefs(task.getEvidenceRefs());
        target.setRawResultRef(task.getRawResultRef());
        target.setErrorCode(task.getErrorCode());
        target.setErrorMessage(task.getErrorMessage());
        target.setUpdatedBy(task.getUpdatedBy());
        target.setUpdatedAt(task.getUpdatedAt());
        return 1;
    }

    @Override
    public List<OcclusionAlgorithmTaskRespDTO> listAlgorithmTasksByEvent(AiVisionDataScope scope,
                                                                        String eventId, int limit) {
        List<OcclusionAlgorithmTaskRespDTO> list = algorithmTasks.stream()
            .filter(t -> t.getEventId() != null && String.valueOf(t.getEventId()).equals(eventId))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .sorted(Comparator.comparing(AiOcclusionAlgorithmTask::getStartedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AiOcclusionAlgorithmTask::getId, Comparator.reverseOrder()))
            .limit(Math.max(0, limit))
            .map(this::toTaskDto)
            .toList();
        return new ArrayList<>(list);
    }

    @Override
    public int writeBackEventAlgorithmResult(AiVisionDataScope scope, String eventId, Long taskId,
                                             String algorithmStatus, String algorithmResult,
                                             BigDecimal confidence, String updatedBy) {
        OcclusionEventRespDTO target = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (target == null) {
            return 0;
        }
        // 只同步算法侧四列；process_status 与 event_version 不动（与 SQL 版本一致）
        target.setAlgorithmStatus(algorithmStatus);
        target.setAlgorithmResult(algorithmResult);
        target.setConfidence(confidence);
        return 1;
    }

    /** 实体 → 响应 DTO；detectedCount 统计检测框 JSON 中 "category" 出现次数（与 SQL 口径一致） */
    private OcclusionAlgorithmTaskRespDTO toTaskDto(AiOcclusionAlgorithmTask t) {
        OcclusionAlgorithmTaskRespDTO dto = new OcclusionAlgorithmTaskRespDTO();
        dto.setTaskId(String.valueOf(t.getId()));
        dto.setTaskNo(t.getTaskNo());
        dto.setEventId(t.getEventId() == null ? null : String.valueOf(t.getEventId()));
        dto.setExecutionStatus(t.getExecutionStatus());
        dto.setConclusion(t.getConclusion());
        dto.setOcclusionType(t.getOcclusionType());
        dto.setConfidence(t.getConfidence());
        dto.setModelName(t.getModelName());
        dto.setModelVersion(t.getModelVersion());
        dto.setAlgorithmVersion(t.getAlgorithmVersion());
        dto.setConfigVersion(t.getConfigVersion());
        dto.setInferenceDurationMs(t.getInferenceDurationMs());
        dto.setStartedAt(t.getStartedAt() == null ? null : TS.format(t.getStartedAt()));
        dto.setFinishedAt(t.getFinishedAt() == null ? null : TS.format(t.getFinishedAt()));
        dto.setDetectedCount(countDetections(t.getDetections()));
        dto.setSimulated(t.getIsSimulated());
        dto.setErrorCode(t.getErrorCode());
        dto.setErrorMessage(t.getErrorMessage());
        dto.setRetryCount(t.getRetryCount());
        return dto;
    }

    private int countDetections(String detections) {
        if (detections == null || detections.isBlank()) {
            return 0;
        }
        int count = 0;
        int idx = detections.indexOf("\"category\"");
        while (idx >= 0) {
            count++;
            idx = detections.indexOf("\"category\"", idx + 1);
        }
        return count;
    }
}
