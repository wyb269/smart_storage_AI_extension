package com.chinaunicom.smartstorage.admin.service.impl.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.constant.VehicleRecognitionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.VehicleRecognitionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.VehicleRecognitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 车辆识别数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳与识别时间<b>每次调用时</b>基于当前时刻计算，原因见类内说明：
 * 「今日识别车辆」按今天过滤，若在构造时固化时间戳，进程跨天后左侧会空、
 * 页面按 90 分钟阈值也会把在线点位误判成离线。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryVehicleRecognitionRepository implements VehicleRecognitionRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long DAY_MS = 86_400_000L;

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[] WAREHOUSE_ID = {"9001", "9002"};
    private static final String[] WAREHOUSE_NAME = {"一号粮库", "二号粮库"};

    /** 识别点位定义：绑摄像头、含设备态与心跳偏移 */
    private static final class PointDef {
        final String pointId;
        final String pointCode;
        final String pointName;
        final String cameraCode;
        final String cameraName;
        final String platformCameraId;
        final String warehouseId;
        final String warehouseName;
        final String deviceStatus;
        final String alarmStatus;
        /** 心跳距今分钟数；null 表示无心跳（UNKNOWN） */
        final Integer heartbeatAgoMin;

        PointDef(String pointId, String pointCode, String pointName, String cameraCode, String cameraName,
                 String platformCameraId, int whIdx, String deviceStatus, String alarmStatus,
                 Integer heartbeatAgoMin) {
            this.pointId = pointId;
            this.pointCode = pointCode;
            this.pointName = pointName;
            this.cameraCode = cameraCode;
            this.cameraName = cameraName;
            this.platformCameraId = platformCameraId;
            this.warehouseId = WAREHOUSE_ID[whIdx];
            this.warehouseName = WAREHOUSE_NAME[whIdx];
            this.deviceStatus = deviceStatus;
            this.alarmStatus = alarmStatus;
            this.heartbeatAgoMin = heartbeatAgoMin;
        }
    }

    private static final PointDef[] POINTS = {
        new PointDef("501", "GATE-E-IN", "东大门入口", "CAM-003", "东大门入口立柱", "C003", 0, "ONLINE", "NORMAL", 2),
        new PointDef("502", "GATE-E-OUT", "东大门出口", "CAM-004", "东大门出口横杆", "C004", 0, "ONLINE", "NORMAL", 4),
        new PointDef("503", "GATE-W-IN", "西大门入口", "CAM-005", "西大门入口灯杆", "C005", 1, "ONLINE", "NORMAL", 6),
        new PointDef("504", "GATE-W-OUT", "西大门出口", "CAM-006", "西大门出口雨棚", "C006", 1, "OFFLINE", "NORMAL", 200),
        new PointDef("505", "GATE-S-IN", "南大门入口", "CAM-007", "南大门入口门柱", "C007", 0, "ONLINE", "NORMAL", 3),
        new PointDef("506", "GATE-N-OUT", "北大门出口", "CAM-008", "北大门出口临时杆", "C008", 1, "UNKNOWN", "NORMAL", null),
    };

    private static final String[] PLATES = {"黑A00000", "黑A00001", "黑A12345", "黑B67890"};
    private static final String[] VEHICLE_TYPES = {"柴油货车", "空车", "客车车型", "新能源轿车", "未识别车型"};
    private static final String[] COLORS = {"白色", "黑色", "银色"};

    /** 识别记录种子：只存相对今天的偏移与可变核实状态，展示时每次现算时间 */
    private static final class EventSeed {
        final String eventId;
        final String plateNo;
        final String vehicleType;
        final String vehicleColor;
        final String direction;
        final int pointIndex;
        /** 0 = 今天；>=1 表示历史天数 */
        final int daysAgo;
        /** 当天内的时间比例 0~1（daysAgo=0 时用于把识别时间落在今天已过去的区间内） */
        final double timeOfDay;
        // 可变：seed() 用算法任务回写覆盖初始值，重新识别成功后也会刷新
        BigDecimal confidence;
        String algorithmStatus;
        // 可变：processEvent 会把 PENDING 推进到终态
        String verifyStatus;
        int eventVersion;
        String processedBy;
        /** 本次运行中被「查看即标记」写下的绝对毫秒；null 表示未标记 */
        Long processedAtAbs;

        EventSeed(String eventId, int i, int daysAgo, double timeOfDay, String verifyStatus) {
            this.eventId = eventId;
            this.plateNo = PLATES[i % PLATES.length];
            this.vehicleType = VEHICLE_TYPES[i % VEHICLE_TYPES.length];
            this.vehicleColor = COLORS[i % COLORS.length];
            // 方向：入口点位记 IN、出口点位记 OUT，与点位语义一致
            this.direction = POINTS[i % POINTS.length].pointName.contains("出口")
                ? VehicleRecognitionStatusEnum.DirectionEnum.OUT.name()
                : VehicleRecognitionStatusEnum.DirectionEnum.IN.name();
            this.pointIndex = i % POINTS.length;
            this.daysAgo = daysAgo;
            this.timeOfDay = timeOfDay;
            this.confidence = (i % 7 == 3) ? null : new BigDecimal("0.8500");
            this.verifyStatus = verifyStatus;
            this.eventVersion = 1;
            if (VehicleRecognitionStatusEnum.VerifyStatusEnum.VERIFIED.name().equals(verifyStatus)) {
                this.processedBy = "系统";
            }
        }
    }

    private final List<EventSeed> seeds = new CopyOnWriteArrayList<>();
    private final List<ProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /**
     * 内存算法任务表。
     *
     * <p>与真实库 {@code ai_vehicle_algorithm_task} 同形，供「重新识别」在
     * 未注入数据库凭据的环境下同样可跑：受理时插一行 QUEUED、轮询时按适配器
     * 输出更新、成功后回写事件。发号用独立区间，不与种子任务 12000 段重叠。</p>
     */
    private final List<VehicleRecognitionAlgorithmTask> algorithmTasks = new CopyOnWriteArrayList<>();
    private final AtomicLong algorithmTaskIdSeq = new AtomicLong(900_000L);

    public InMemoryVehicleRecognitionRepository() {
        seed();
    }

    /** 生成 12 条今日识别 + 18 条历史识别，共 30 条 */
    private void seed() {
        String[] verifyCycle = {
            VehicleRecognitionStatusEnum.VerifyStatusEnum.VERIFIED.name(),
            VehicleRecognitionStatusEnum.VerifyStatusEnum.VERIFIED.name(),
            VehicleRecognitionStatusEnum.VerifyStatusEnum.PENDING.name(),
        };
        for (int i = 0; i < 30; i++) {
            int daysAgo = i < 12 ? 0 : ((i - 12) % 6) + 1;
            double timeOfDay = (i % 12) / 12.0;
            String verify = verifyCycle[i % verifyCycle.length];
            EventSeed s = new EventSeed(String.valueOf(7000 + i), i, daysAgo, timeOfDay, verify);
            seeds.add(s);
            // 每条记录配一条历史算法任务，状态分布与 07_seed_algorithm_tasks.sql 一致：
            // 待核实的记录里刻意留出 车牌读不出来 / 进行中 的样本，
            // 这样兜底模式下详情弹窗的「算法任务」区与「重新识别」按钮都有内容可点。
            VehicleRecognitionAlgorithmTask task = seedAlgorithmTask(s, POINTS[s.pointIndex], i, verify);
            algorithmTasks.add(task);
            // 事件行的算法状态/置信度由任务回写得到，与真实库 07 的 UPDATE 同语义，
            // 避免「任务失败但事件仍显示识别成功」
            s.algorithmStatus = task.getExecutionStatus();
            s.confidence = task.getConfidence();
        }
    }

    /**
     * 内存兜底模式的历史算法任务。
     *
     * <p>状态分配（与真实库种子同口径）：待核实的记录中第 1 条 QUEUED、第 2 条 RUNNING、
     * 第 3/4 条 FAILED(PLATE_UNREADABLE)、第 5 条 TIMEOUT，其余（含全部已核实记录）SUCCEEDED。
     * <b>未成功的事件置信度留空</b>，用于验证页面「暂无」而不是 0%。</p>
     */
    private VehicleRecognitionAlgorithmTask seedAlgorithmTask(EventSeed s, PointDef p, int index, String verify) {
        boolean pending = VehicleRecognitionStatusEnum.VerifyStatusEnum.PENDING.name().equals(verify);
        String status = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name();
        // 置信度缺失的记录，算法结论给 UNCERTAIN —— 与 04 号种子的「暂无」样本同口径，
        // 让页面既能验证「暂无」渲染，也不会出现「有置信度但结论不确定」这种自相矛盾的数据
        String conclusion = s.confidence == null ? "UNCERTAIN" : "RECOGNIZED";
        String errorCode = null;
        String errorMessage = null;
        BigDecimal confidence = s.confidence;
        boolean plateReadable = true;

        if (pending && index % 30 == 0) {
            status = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.QUEUED.name();
            conclusion = null;
            confidence = null;
        } else if (pending && index % 30 == 3) {
            status = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.RUNNING.name();
            conclusion = null;
            confidence = null;
        } else if (pending && (index % 30 == 6 || index % 30 == 9)) {
            status = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.FAILED.name();
            conclusion = "UNCERTAIN";
            confidence = null;
            errorCode = "PLATE_UNREADABLE";
            errorMessage = "车牌区域反光，OCR 无法读取完整号牌";
            plateReadable = false;
        } else if (pending && index % 30 == 12) {
            status = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name();
            conclusion = null;
            confidence = null;
            errorCode = "INFER_TIMEOUT";
            errorMessage = "算法执行超时，未返回结论";
        }

        long now = System.currentTimeMillis();
        long recognizedMs = recognizedMsOf(s, now);
        long durationMs = 18L + (index * 37L) % 403L;
        // 进行中的任务时间基准必须相对当前时刻，否则「最近检测」几天后看着像坏了
        LocalDateTime startedAt = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
            || VehicleRecognitionStatusEnum.AlgorithmStatusEnum.FAILED.name().equals(status)
            || VehicleRecognitionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name().equals(status)
            ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(recognizedMs), ZoneId.systemDefault())
            : LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now - (2 + index) * 30_000L),
                ZoneId.systemDefault());
        // 与契约一致：失败/超时/进行中都不得有 finished_at，只有成功才有
        boolean succeeded = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);
        LocalDateTime finishedAt = succeeded ? startedAt.plusNanos(durationMs * 1_000_000L) : null;
        boolean ran = succeeded || errorCode != null;

        VehicleRecognitionAlgorithmTask task = new VehicleRecognitionAlgorithmTask();
        task.setId(12000L + index);
        task.setTaskNo(VehicleRecognitionConstant.ALGORITHM_TASK_NO_PREFIX
            + TS.format(startedAt).replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
            + String.format("%04d", index + 1));
        task.setEventId(Long.valueOf(s.eventId));
        task.setPointId(Long.valueOf(p.pointId));
        task.setCameraId(p.platformCameraId);
        task.setCameraCode(p.cameraCode);
        task.setWarehouseId(Long.valueOf(p.warehouseId));
        task.setRegionId("000000");
        task.setTaskType(VehicleRecognitionConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(VehicleRecognitionConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(VehicleAlgorithmTaskConverter.mediaRefsJson(List.of(
            "mock://vehicle/" + s.eventId + "/full",
            "mock://vehicle/" + s.eventId + "/plate")));
        task.setExecutionStatus(status);
        task.setConclusion(conclusion);
        task.setVehicleType(conclusion == null ? null : s.vehicleType);
        task.setConfidence(confidence);
        task.setModelName(VehicleRecognitionConstant.ALGORITHM_MODEL_NAME);
        task.setModelVersion(VehicleRecognitionConstant.ALGORITHM_MODEL_VERSION);
        task.setAlgorithmVersion(VehicleRecognitionConstant.ALGORITHM_ADAPTER_VERSION);
        task.setConfigVersion(VehicleRecognitionConstant.ALGORITHM_CONFIG_VERSION);
        task.setConfidenceThreshold(VehicleRecognitionConstant.ALGORITHM_CONFIDENCE_THRESHOLD);
        task.setStartedAt(startedAt);
        task.setFinishedAt(finishedAt);
        task.setInferenceDurationMs(ran ? durationMs : null);
        task.setDetections(VehicleAlgorithmTaskConverter.historyDetectionsJson(
            s.eventId, s.vehicleType, s.direction, confidence,
            maskPlate(s.plateNo), confidence, plateReadable));
        // 证据引用用对象数组，与适配器输出同形；只落受控键，不落任何 URL
        task.setEvidenceRefs(VehicleAlgorithmTaskConverter.toJson(java.util.List.of(
            java.util.Map.of("evidence_type", "VEHICLE_IMAGE",
                "object_key", "mock://vehicle/" + s.eventId + "/full"),
            java.util.Map.of("evidence_type", "PLATE_CROP",
                "object_key", "mock://vehicle/" + s.eventId + "/plate"))));
        task.setRawResultRef("mock://task/" + task.getTaskNo() + "/raw");
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        task.setRetryCount(0);
        task.setTraceId("seed-trace-" + s.eventId);
        task.setIsSimulated(Boolean.TRUE);
        task.setCreatedBy("seed");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedBy("seed");
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    /** 车牌掩码；算法任务侧永不落原文（PII） */
    private String maskPlate(String plateNo) {
        return VehicleAlgorithmTaskConverter.maskPlate(plateNo);
    }

    private long startOfTodayMs(long now) {
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(now), ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 把种子的相对偏移换算成绝对识别毫秒；今天的记录保证落在 [今天0点, now] 内 */
    private long recognizedMsOf(EventSeed seed, long now) {
        long startToday = startOfTodayMs(now);
        if (seed.daysAgo == 0) {
            long elapsed = now - startToday;
            return startToday + (long) (seed.timeOfDay * elapsed);
        }
        return startToday - (long) seed.daysAgo * DAY_MS + (long) (seed.timeOfDay * DAY_MS);
    }

    /* ==================== 识别点位 ==================== */

    /**
     * 识别点位状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值判定离线，
     * 若固化时间戳，进程跑久了全部点位会被误判为离线。</p>
     */
    @Override
    public List<MonitorPointRespDTO> listPoints(AiVisionDataScope scope) {
        long now = System.currentTimeMillis();
        List<MonitorPointRespDTO> list = new ArrayList<>();
        for (int i = 0; i < POINTS.length; i++) {
            PointDef p = POINTS[i];
            if (!allows(scope, p.warehouseId)) {
                continue;
            }
            MonitorPointRespDTO dto = new MonitorPointRespDTO();
            dto.setPointId(p.pointId);
            dto.setPointCode(p.pointCode);
            dto.setPointName(p.pointName);
            dto.setWarehouseId(p.warehouseId);
            dto.setWarehouseName(p.warehouseName);
            dto.setCameraCode(p.cameraCode);
            dto.setCameraName(p.cameraName);
            dto.setPlatformCameraId(p.platformCameraId);
            dto.setDeviceStatus(p.deviceStatus);
            dto.setAlarmStatus(p.alarmStatus);
            dto.setLastHeartbeatAt(p.heartbeatAgoMin == null
                ? null : TS.format(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(now - p.heartbeatAgoMin * 60_000L), ZoneId.systemDefault())));
            dto.setLatestEventAt(null);
            dto.setSortNo(i + 1);
            list.add(dto);
        }
        return list;
    }

    /* ==================== 今日识别车辆 ==================== */

    @Override
    public List<TodayVehicleRespDTO> listTodayVehicle(AiVisionDataScope scope, int limit) {
        long now = System.currentTimeMillis();
        long startToday = startOfTodayMs(now);
        List<TodayVehicleRespDTO> list = new ArrayList<>();
        for (EventSeed s : seeds) {
            long ms = recognizedMsOf(s, now);
            if (ms < startToday) {
                continue;
            }
            PointDef p = POINTS[s.pointIndex];
            if (!allows(scope, p.warehouseId)) {
                continue;
            }
            TodayVehicleRespDTO dto = new TodayVehicleRespDTO();
            dto.setEventId(s.eventId);
            dto.setPlateNo(s.plateNo);
            dto.setVehicleType(s.vehicleType);
            dto.setVehicleColor(s.vehicleColor);
            dto.setDirection(s.direction);
            dto.setDirectionName(directionLabel(s.direction));
            dto.setPointId(p.pointId);
            dto.setGateName(p.pointName);
            dto.setRecognizedAt(TS.format(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault())));
            dto.setVerifyStatus(s.verifyStatus);
            list.add(dto);
        }
        list.sort(Comparator.comparing(TodayVehicleRespDTO::getRecognizedAt).reversed());
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<VehicleEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return filter(scope, new VehicleEventQueryReqDTO()).stream().limit(limit).toList();
    }

    @Override
    public List<VehicleEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                          VehicleEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<VehicleEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                        VehicleEventQueryReqDTO query, int limit) {
        List<VehicleEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<VehicleEventRespDTO> filter(AiVisionDataScope scope, VehicleEventQueryReqDTO query) {
        long now = System.currentTimeMillis();
        VehicleEventQueryReqDTO q = query == null ? new VehicleEventQueryReqDTO() : query;
        List<VehicleEventRespDTO> list = new ArrayList<>();
        for (EventSeed s : seeds) {
            PointDef p = POINTS[s.pointIndex];
            if (!allows(scope, p.warehouseId)) {
                continue;
            }
            if (q.getPlateNo() != null && !q.getPlateNo().isBlank()
                && !s.plateNo.contains(q.getPlateNo())) {
                continue;
            }
            if (q.getVehicleType() != null && !q.getVehicleType().isBlank()
                && !q.getVehicleType().equals(s.vehicleType)) {
                continue;
            }
            if (q.getDirection() != null && !q.getDirection().isBlank()
                && !q.getDirection().equals(s.direction)) {
                continue;
            }
            if (q.getGateName() != null && !q.getGateName().isBlank()
                && !p.pointName.contains(q.getGateName())) {
                continue;
            }
            if (q.getVerifyStatus() != null && !q.getVerifyStatus().isBlank()
                && !q.getVerifyStatus().equals(s.verifyStatus)) {
                continue;
            }
            list.add(materialize(s, p, now));
        }
        list.sort(Comparator.comparing(VehicleEventRespDTO::getRecognizedAt).reversed());
        return list;
    }

    private VehicleEventRespDTO materialize(EventSeed s, PointDef p, long now) {
        long ms = recognizedMsOf(s, now);
        VehicleEventRespDTO e = new VehicleEventRespDTO();
        e.setEventId(s.eventId);
        e.setEventNo(VehicleRecognitionConstant.EVENT_NO_PREFIX
            + TS.format(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault()))
                .replace("-", "").replace(":", "").replace(" ", "").substring(0, 8)
            + s.eventId.substring(s.eventId.length() - 4));
        e.setPlateNo(s.plateNo);
        e.setVehicleType(s.vehicleType);
        e.setVehicleColor(s.vehicleColor);
        e.setDirection(s.direction);
        e.setDirectionName(directionLabel(s.direction));
        e.setPointId(p.pointId);
        e.setGateName(p.pointName);
        e.setRecognizedAt(TS.format(LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault())));
        e.setWarehouseId(p.warehouseId);
        e.setWarehouseName(p.warehouseName);
        e.setCameraCode(p.cameraCode);
        e.setAlgorithmStatus(VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
        e.setConfidence(s.confidence);
        e.setVerifyStatus(s.verifyStatus);
        e.setEventVersion(s.eventVersion);
        if (!VehicleRecognitionStatusEnum.VerifyStatusEnum.PENDING.name().equals(s.verifyStatus)) {
            e.setProcessedBy(s.processedBy);
            long processedMs = s.processedAtAbs != null ? s.processedAtAbs : ms + 60_000L;
            e.setProcessedAt(TS.format(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(processedMs), ZoneId.systemDefault())));
            e.setVerifyOpinion(VehicleRecognitionConstant.AUTO_VIEW_OPINION);
        }
        return e;
    }

    private String directionLabel(String direction) {
        return VehicleRecognitionStatusEnum.DirectionEnum.IN.name().equals(direction) ? "入库" : "出库";
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

    /* ==================== 详情与核实 ==================== */

    @Override
    public VehicleEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        EventSeed s = seeds.stream().filter(x -> x.eventId.equals(eventId)).findFirst().orElse(null);
        if (s == null) {
            return null;
        }
        PointDef p = POINTS[s.pointIndex];
        if (!allows(scope, p.warehouseId)) {
            return null;
        }
        long now = System.currentTimeMillis();
        VehicleEventDetailRespDTO detail = new VehicleEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(materialize(s, p, now), detail);
        detail.setEventDesc(p.pointName + " 于识别时间捕获车辆并识别车牌、车型与颜色");
        detail.setIsSimulated(true);

        List<VehicleEvidenceRespDTO> evidences = new ArrayList<>();
        VehicleEvidenceRespDTO full = new VehicleEvidenceRespDTO();
        full.setEvidenceId(eventId + "-1");
        full.setEvidenceType(VehicleRecognitionStatusEnum.EvidenceTypeEnum.VEHICLE_IMAGE.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        full.setOriginalUrl("mock://vehicle/" + eventId + "/full");
        full.setThumbnailUrl("mock://vehicle/" + eventId + "/full-thumb");
        full.setCapturedAt(detail.getRecognizedAt());
        full.setCameraCode(p.cameraCode);
        full.setSortNo(1);
        evidences.add(full);

        VehicleEvidenceRespDTO plate = new VehicleEvidenceRespDTO();
        plate.setEvidenceId(eventId + "-2");
        plate.setEvidenceType(VehicleRecognitionStatusEnum.EvidenceTypeEnum.PLATE_CROP.name());
        plate.setOriginalUrl("mock://vehicle/" + eventId + "/plate");
        plate.setAnnotatedUrl("mock://vehicle/" + eventId + "/plate-marked");
        plate.setCapturedAt(detail.getRecognizedAt());
        plate.setCameraCode(p.cameraCode);
        plate.setSortNo(2);
        evidences.add(plate);

        detail.setEvidences(evidences);
        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        return records.stream().filter(r -> r.getRecordId().endsWith(eventId)).toList();
    }

    @Override
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String verifyResult, String verifyOpinion,
                            String processedBy) {
        EventSeed s = seeds.stream().filter(x -> x.eventId.equals(eventId)).findFirst().orElse(null);
        if (s == null) {
            return 0;
        }
        PointDef p = POINTS[s.pointIndex];
        if (!allows(scope, p.warehouseId)) {
            return 0;
        }
        // 乐观锁 + 只允许从 PENDING 迁移，与真实库 SQL 语义一致
        if (eventVersion == null || eventVersion != s.eventVersion
            || !VehicleRecognitionStatusEnum.VerifyStatusEnum.PENDING.name().equals(s.verifyStatus)) {
            return 0;
        }
        long now = System.currentTimeMillis();
        s.verifyStatus = verifyResult;
        s.processedBy = processedBy;
        s.processedAtAbs = now;
        s.eventVersion += 1;

        ProcessRecordRespDTO record = new ProcessRecordRespDTO();
        record.setRecordId("R-" + eventId + "-" + records.size());
        record.setStatusBefore(statusBefore);
        record.setStatusAfter(verifyResult);
        record.setProcessResult(verifyResult);
        record.setProcessOpinion(verifyOpinion);
        record.setProcessedBy(processedBy);
        record.setProcessedAt(TS.format(LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(now), ZoneId.systemDefault())));
        record.setSource("WEB");
        records.add(record);
        return 1;
    }

    /* ==================== 算法任务 / 重新识别（内存兜底） ==================== */

    /**
     * 重新识别受理前，从事件对应的点位取「属于谁」的列，状态与结果列留空由 Service 补齐。
     * 记录不存在或越权返回 null，与真实库同语义。
     */
    @Override
    public VehicleRecognitionAlgorithmTask findAlgorithmTaskContext(AiVisionDataScope scope, String eventId) {
        EventSeed s = seeds.stream().filter(x -> x.eventId.equals(eventId)).findFirst().orElse(null);
        if (s == null) {
            return null;
        }
        PointDef p = POINTS[s.pointIndex];
        if (!allows(scope, p.warehouseId)) {
            return null;
        }
        VehicleRecognitionAlgorithmTask ctx = new VehicleRecognitionAlgorithmTask();
        ctx.setEventId(Long.valueOf(s.eventId));
        ctx.setPointId(Long.valueOf(p.pointId));
        ctx.setCameraId(p.platformCameraId);
        ctx.setCameraCode(p.cameraCode);
        ctx.setWarehouseId(Long.valueOf(p.warehouseId));
        ctx.setRegionId("000000");
        return ctx;
    }

    @Override
    public Long insertAlgorithmTask(VehicleRecognitionAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        if (task.getId() == null) {
            task.setId(algorithmTaskIdSeq.incrementAndGet());
        }
        algorithmTasks.add(task);
        return task.getId();
    }

    /** 只覆盖非空字段，避免适配层兜底空对象抹掉已落库的结论/置信度。 */
    @Override
    public int updateAlgorithmTask(AiVisionDataScope scope, VehicleRecognitionAlgorithmTask task) {
        if (task == null || task.getTaskNo() == null || task.getTaskNo().isBlank()) {
            return 0;
        }
        VehicleRecognitionAlgorithmTask row = findByTaskNoInScope(scope, task.getTaskNo());
        if (row == null) {
            return 0;
        }
        if (task.getExecutionStatus() != null) {
            row.setExecutionStatus(task.getExecutionStatus());
        }
        if (task.getConclusion() != null) {
            row.setConclusion(task.getConclusion());
        }
        if (task.getVehicleType() != null) {
            row.setVehicleType(task.getVehicleType());
        }
        if (task.getConfidence() != null) {
            row.setConfidence(task.getConfidence());
        }
        if (task.getDetections() != null) {
            row.setDetections(task.getDetections());
        }
        if (task.getEvidenceRefs() != null) {
            row.setEvidenceRefs(task.getEvidenceRefs());
        }
        if (task.getRawResultRef() != null) {
            row.setRawResultRef(task.getRawResultRef());
        }
        if (task.getErrorCode() != null) {
            row.setErrorCode(task.getErrorCode());
        }
        if (task.getErrorMessage() != null) {
            row.setErrorMessage(task.getErrorMessage());
        }
        if (task.getFinishedAt() != null) {
            row.setFinishedAt(task.getFinishedAt());
        }
        if (task.getInferenceDurationMs() != null) {
            row.setInferenceDurationMs(task.getInferenceDurationMs());
        }
        if (task.getRetryCount() != null) {
            row.setRetryCount(task.getRetryCount());
        }
        if (task.getModelName() != null) {
            row.setModelName(task.getModelName());
        }
        if (task.getModelVersion() != null) {
            row.setModelVersion(task.getModelVersion());
        }
        if (task.getAlgorithmVersion() != null) {
            row.setAlgorithmVersion(task.getAlgorithmVersion());
        }
        row.setUpdatedBy(task.getUpdatedBy());
        row.setUpdatedAt(task.getUpdatedAt() == null ? LocalDateTime.now() : task.getUpdatedAt());
        return 1;
    }

    @Override
    public VehicleRecognitionAlgorithmTask findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        return findByTaskNoInScope(scope, taskNo);
    }

    @Override
    public List<VehicleRecognitionAlgorithmTask> listAlgorithmTasksByEvent(AiVisionDataScope scope,
                                                                          String eventId, int limit) {
        EventSeed s = seeds.stream().filter(x -> x.eventId.equals(eventId)).findFirst().orElse(null);
        if (s == null || !allows(scope, POINTS[s.pointIndex].warehouseId)) {
            return List.of();
        }
        return algorithmTasks.stream()
            .filter(t -> String.valueOf(t.getEventId()).equals(eventId))
            .sorted(Comparator.comparing(VehicleRecognitionAlgorithmTask::getStartedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(Math.max(0, limit))
            .toList();
    }

    /** 只写 algorithm_status / confidence，绝不触碰 verify_* 与识别结果列。 */
    @Override
    public int writeBackEventAlgorithmResult(AiVisionDataScope scope, String eventId, Long taskId,
                                             String executionStatus, BigDecimal confidence,
                                             String updatedBy, LocalDateTime updatedAt) {
        EventSeed s = seeds.stream().filter(x -> x.eventId.equals(eventId)).findFirst().orElse(null);
        if (s == null || !allows(scope, POINTS[s.pointIndex].warehouseId)) {
            return 0;
        }
        s.algorithmStatus = executionStatus;
        if (confidence != null) {
            s.confidence = confidence;
        }
        return 1;
    }

    private VehicleRecognitionAlgorithmTask findByTaskNoInScope(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        VehicleRecognitionAlgorithmTask row = algorithmTasks.stream()
            .filter(t -> taskNo.equals(t.getTaskNo())).findFirst().orElse(null);
        if (row == null) {
            return null;
        }
        EventSeed s = seeds.stream()
            .filter(x -> x.eventId.equals(String.valueOf(row.getEventId()))).findFirst().orElse(null);
        if (s != null && !allows(scope, POINTS[s.pointIndex].warehouseId)) {
            return null;
        }
        return row;
    }
}
