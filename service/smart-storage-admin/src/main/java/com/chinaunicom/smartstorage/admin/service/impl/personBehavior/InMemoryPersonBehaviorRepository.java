package com.chinaunicom.smartstorage.admin.service.impl.personBehavior;

import com.chinaunicom.smartstorage.admin.constant.PersonBehaviorConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.personBehavior.AiPersonAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.PersonBehaviorStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.personBehavior.PersonBehaviorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 人员行为监测数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳时间每次调用时基于当前时刻计算，原因见 {@link #heartbeat(int)}。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryPersonBehaviorRepository implements PersonBehaviorRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[][] WAREHOUSES = {
        {"9001", "一号粮库"}, {"9002", "二号粮库"}, {"9003", "三号粮库"}, {"9004", "散粮库"},
    };

    /** 区域名后缀，与仓库名拼成「一号仓库前作业区」这类原型上的卡片标题 */
    private static final String[] AREA_SUFFIXES = {
        "前作业区", "门口作业区", "卸粮坑作业区", "工作塔一层",
        "罩棚下作业区", "北侧装卸区", "输送廊道", "西门值守区",
    };

    private final List<PersonEventRespDTO> events = new CopyOnWriteArrayList<>();
    private final List<ProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /** 算法任务内存存储：历史种子 + 运行期「重新识别」产出 */
    private final List<AiPersonAlgorithmTask> algorithmTasks = new CopyOnWriteArrayList<>();

    /** 运行期任务发号（避开历史种子的 91000 段） */
    private final AtomicLong taskSeq = new AtomicLong(1);

    public InMemoryPersonBehaviorRepository() {
        seed();
        seedAlgorithmTasks();
    }

    /** 生成 40 条人员行为事件 */
    private void seed() {
        for (int i = 0; i < 40; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            int areaIndex = i % 12;
            String areaName = wh[1] + AREA_SUFFIXES[areaIndex % AREA_SUFFIXES.length];
            PersonBehaviorStatusEnum.BehaviorTypeEnum type =
                PersonBehaviorStatusEnum.BehaviorTypeEnum.values()[i % 5];
            // 分布：每 4 条 1 条未核实、1 条处理中、2 条已处理，三态都能演示到
            String processStatus;
            if (i % 4 == 0) {
                processStatus = PersonBehaviorStatusEnum.ProcessStatusEnum.PENDING.name();
            } else if (i % 4 == 1) {
                processStatus = PersonBehaviorStatusEnum.ProcessStatusEnum.PROCESSING.name();
            } else {
                processStatus = PersonBehaviorStatusEnum.ProcessStatusEnum.DONE.name();
            }

            PersonEventRespDTO e = new PersonEventRespDTO();
            e.setEventId(String.valueOf(2000 + i));
            e.setEventNo("PR" + TS.format(LocalDateTime.now().minusHours(i)).replace("-", "")
                .replace(":", "").replace(" ", "").substring(0, 8) + String.format("%04d", i + 1));
            e.setOccurredAt(TS.format(LocalDateTime.now().minusMinutes(30L * i)));
            e.setWarehouseId(wh[0]);
            e.setWarehouseName(wh[1]);
            e.setPointId(String.valueOf(100 + areaIndex));
            e.setCameraCode(String.format("CAM-%03d", areaIndex + 1));
            e.setAreaName(areaName);
            e.setBehaviorType(type.name());
            e.setBehaviorTypeName(type.getLabel());
            e.setAlgorithmStatus(PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            e.setAlgorithmResult(PersonBehaviorStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            e.setConfidence(i % 7 == 3 ? null : new BigDecimal("0.8500"));
            // 聚集人数只有 CROWD 类事件才有，其余留空而不是写 0：
            // 0 会被读成「画面里没人」，与「该指标不适用」是两回事
            e.setPersonCount(type == PersonBehaviorStatusEnum.BehaviorTypeEnum.CROWD ? 5 + (i % 6) : null);
            e.setProcessStatus(processStatus);
            e.setEventVersion(1);
            if (!PersonBehaviorStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                e.setProcessedBy("系统");
                e.setProcessedAt(TS.format(LocalDateTime.now().minusMinutes(10L * i)));
                e.setProcessOpinion(
                    PersonBehaviorStatusEnum.ProcessStatusEnum.DONE.name().equals(processStatus)
                        ? "查看事件详情后自动标记为已处理" : "已通知现场班组长跟进");
            }
            events.add(e);
        }
    }

    /**
     * 历史算法任务种子：40 条事件每条 1 个任务。
     *
     * <p>状态分布与 07 种子脚本一致：SUCCEEDED 34 / FAILED 2 / TIMEOUT 2 /
     * RUNNING 1 / QUEUED 1（约 85% / 5% / 5% / 5%），进行中的两条让
     * 「重新识别」的可观察态在兜底模式下也能演示。</p>
     *
     * <p>检测框数量与事件 person_count 自洽（CROWD 框数 = 聚集人数，其余 1 框），
     * 与 07 脚本同一约定；时间相对当前时刻生成，进程跑久也不会显示陈旧时间。</p>
     */
    private void seedAlgorithmTasks() {
        final String day = TS.format(LocalDateTime.now())
            .replace("-", "").replace(":", "").replace(" ", "").substring(0, 8);

        for (int i = 0; i < events.size(); i++) {
            PersonEventRespDTO e = events.get(i);
            final int rn = i + 1;
            final String status;
            if (rn == 39) {
                status = PersonBehaviorStatusEnum.AlgorithmStatusEnum.RUNNING.name();
            } else if (rn == 40) {
                status = PersonBehaviorStatusEnum.AlgorithmStatusEnum.QUEUED.name();
            } else if (rn == 17 || rn == 37) {
                status = PersonBehaviorStatusEnum.AlgorithmStatusEnum.FAILED.name();
            } else if (rn == 9 || rn == 29) {
                status = PersonBehaviorStatusEnum.AlgorithmStatusEnum.TIMEOUT.name();
            } else {
                status = PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name();
            }
            final boolean succeeded =
                PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);

            AiPersonAlgorithmTask t = new AiPersonAlgorithmTask();
            t.setId(91000L + rn);
            t.setTaskNo("PAT" + day + String.format("%04d", rn));
            t.setEventId(parseLongSafe(e.getEventId()));
            t.setPointId(parseLongSafe(e.getPointId()));
            t.setCameraCode(e.getCameraCode());
            t.setWarehouseId(parseLongSafe(e.getWarehouseId()));
            t.setTaskType(PersonBehaviorConstant.ALGORITHM_TASK_TYPE);
            t.setMediaType(PersonBehaviorConstant.ALGORITHM_MEDIA_TYPE);
            t.setMediaRefs("[\"mock://person/" + e.getEventId() + "/behavior\"]");
            t.setNormalReferenceRef("mock://person/" + e.getEventId() + "/normal");
            t.setExecutionStatus(status);
            t.setRetryCount(0);
            t.setTraceId("trace-pat-" + day + "-" + String.format("%03d", rn));
            t.setIsSimulated(Boolean.TRUE);
            t.setCreatedBy("seed-memory");
            t.setCreatedAt(LocalDateTime.now());

            LocalDateTime occurred = parseTs(e.getOccurredAt());
            if (succeeded) {
                t.setConclusion(PersonBehaviorStatusEnum.AlgorithmResultEnum.ANOMALY.name());
                t.setBehaviorType(e.getBehaviorType());
                t.setConfidence(BigDecimal.valueOf(0.55 + ((rn * 37) % 44) * 0.01)
                    .setScale(4, RoundingMode.HALF_UP));
                t.setModelName("person-behavior-detector");
                t.setModelVersion("v1.3.0");
                t.setAlgorithmVersion("algo-2026.09.1");
                t.setConfigVersion(PersonBehaviorConstant.ALGORITHM_CONFIG_VERSION);
                t.setConfidenceThreshold(new BigDecimal("0.7500"));
                t.setMediaDigest("md5:mock-person-" + e.getEventId());
                LocalDateTime started = occurred.plusSeconds(2);
                long durationMs = 18 + (rn * 97) % 403;
                t.setStartedAt(started);
                t.setFinishedAt(started.plus(durationMs, ChronoUnit.MILLIS));
                t.setInferenceDurationMs(durationMs);
                int boxes = e.getPersonCount() == null ? 1 : e.getPersonCount();
                t.setDetections(buildDetectionsJson(boxes, t.getConfidence(),
                    e.getEventId(), e.getBehaviorType()));
                t.setDetectedCount(boxes);
                t.setEvidenceRefs("[{\"evidenceType\":\"NORMAL_REFERENCE\","
                    + "\"objectKey\":\"mock://person/" + e.getEventId() + "/normal\"},"
                    + "{\"evidenceType\":\"BEHAVIOR_FRAME\","
                    + "\"objectKey\":\"mock://person/" + e.getEventId() + "/behavior\"}]");
                t.setRawResultRef("mock://task/" + t.getTaskNo() + "/raw");
            } else if (PersonBehaviorStatusEnum.AlgorithmStatusEnum.FAILED.name().equals(status)
                    || PersonBehaviorStatusEnum.AlgorithmStatusEnum.TIMEOUT.name().equals(status)) {
                // 失败/超时没有 finished_at 与置信度
                t.setStartedAt(occurred.plusSeconds(2));
                boolean failed = PersonBehaviorStatusEnum.AlgorithmStatusEnum.FAILED.name().equals(status);
                t.setErrorCode(failed ? "MOCK_FAILURE" : "MOCK_TIMEOUT");
                t.setErrorMessage(failed ? "模拟算法执行失败" : "模拟算法执行超时");
            } else if (PersonBehaviorStatusEnum.AlgorithmStatusEnum.RUNNING.name().equals(status)) {
                t.setStartedAt(LocalDateTime.now().minusMinutes(2));
            }

            algorithmTasks.add(t);

            // 与 07 脚本的回写口径一致：事件算法侧字段跟随任务
            e.setAlgorithmStatus(status);
            e.setAlgorithmResult(succeeded
                ? PersonBehaviorStatusEnum.AlgorithmResultEnum.ANOMALY.name() : null);
            e.setConfidence(succeeded ? t.getConfidence() : null);
        }
    }

    /** 生成与 07 脚本同形的 detections JSON（框数 = count） */
    private String buildDetectionsJson(int count, BigDecimal conf, String eventId, String behaviorType) {
        StringBuilder sb = new StringBuilder("[");
        for (int k = 1; k <= count; k++) {
            if (k > 1) {
                sb.append(',');
            }
            String category = "NO_HELMET".equals(behaviorType) && k == 1 ? "no_helmet" : "person";
            sb.append("{\"category\":\"").append(category)
                .append("\",\"confidence\":").append(conf == null ? "null" : conf.toPlainString())
                .append(",\"bbox\":\"[").append(10 + (k * 55) % 400)
                .append(',').append(60 + (k % 4) * 45)
                .append(',').append(150 + (k * 55) % 400)
                .append(',').append(240 + (k % 4) * 45)
                .append("]\",\"trackId\":\"track-").append(eventId)
                .append('-').append(String.format("%03d", k)).append("\"}");
        }
        return sb.append(']').toString();
    }

    private LocalDateTime parseTs(String text) {
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (RuntimeException ex) {
            return LocalDateTime.now();
        }
    }

    private Long parseLongSafe(String value) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 监测区域状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值
     * 判定心跳超时，若在这里固定时间戳，进程跑久了全部区域会被误判为离线，
     * 正常/聚集/未戴安全帽状态就永远演示不出来。</p>
     */
    @Override
    public List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        List<MonitorPointStatusRespDTO> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            if (!allows(scope, wh[0])) {
                continue;
            }
            MonitorPointStatusRespDTO p = new MonitorPointStatusRespDTO();
            p.setPointId(String.valueOf(100 + i));
            p.setPointCode(String.format("AREA-%03d", i + 1));
            p.setPointName(wh[1] + AREA_SUFFIXES[i % AREA_SUFFIXES.length]);
            p.setWarehouseId(wh[0]);
            p.setWarehouseName(wh[1]);
            p.setCameraId(String.format("C%03d", i + 1));
            p.setCameraCode(String.format("CAM-%03d", i + 1));
            p.setPlatformCameraId(String.format("C%03d", i + 1));
            // 状态分布：多数在线，少量离线与未知；在线告警项分别落在聚集与未戴安全帽
            if (i == 1 || i == 8) {
                p.setDeviceStatus(PersonBehaviorStatusEnum.DeviceStatusEnum.OFFLINE.name());
                p.setAlarmStatus(PersonBehaviorStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setAlarmBehaviorType(null);
                p.setLastHeartbeatAt(heartbeat(200));
            } else if (i == 11) {
                p.setDeviceStatus(PersonBehaviorStatusEnum.DeviceStatusEnum.UNKNOWN.name());
                p.setAlarmStatus(PersonBehaviorStatusEnum.AlarmStatusEnum.NORMAL.name());
                p.setAlarmBehaviorType(null);
                p.setLastHeartbeatAt(null);
            } else {
                p.setDeviceStatus(PersonBehaviorStatusEnum.DeviceStatusEnum.ONLINE.name());
                if (i == 2 || i == 6) {
                    p.setAlarmStatus(PersonBehaviorStatusEnum.AlarmStatusEnum.ACTIVE.name());
                    p.setAlarmBehaviorType(PersonBehaviorStatusEnum.BehaviorTypeEnum.CROWD.name());
                } else if (i == 4 || i == 9) {
                    p.setAlarmStatus(PersonBehaviorStatusEnum.AlarmStatusEnum.ACTIVE.name());
                    p.setAlarmBehaviorType(PersonBehaviorStatusEnum.BehaviorTypeEnum.NO_HELMET.name());
                } else {
                    p.setAlarmStatus(PersonBehaviorStatusEnum.AlarmStatusEnum.NORMAL.name());
                    p.setAlarmBehaviorType(null);
                }
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
    public List<PersonEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .sorted(Comparator.comparing(PersonEventRespDTO::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<PersonEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                         PersonEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<PersonEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                       PersonEventQueryReqDTO query, int limit) {
        List<PersonEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<PersonEventRespDTO> filter(AiVisionDataScope scope,
                                            PersonEventQueryReqDTO query) {
        PersonEventQueryReqDTO q = query == null ? new PersonEventQueryReqDTO() : query;
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
            .filter(e -> q.getBehaviorType() == null || q.getBehaviorType().isBlank()
                || q.getBehaviorType().equals(e.getBehaviorType()))
            .filter(e -> q.getProcessStatus() == null || q.getProcessStatus().isBlank()
                || q.getProcessStatus().equals(e.getProcessStatus()))
            .sorted(Comparator.comparing(PersonEventRespDTO::getOccurredAt).reversed())
            .toList();
    }

    @Override
    public PersonEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        PersonEventRespDTO base = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (base == null) {
            return null;
        }
        PersonEventDetailRespDTO detail = new PersonEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setEventDesc(base.getAreaName() + " 检测到" + base.getBehaviorTypeName() + "，已超过判定阈值。");
        detail.setDurationSeconds(120L);
        detail.setIsSimulated(true);

        List<PersonEvidenceRespDTO> evidences = new ArrayList<>();
        PersonEvidenceRespDTO behavior = new PersonEvidenceRespDTO();
        behavior.setEvidenceId(eventId + "-1");
        behavior.setEvidenceType(PersonBehaviorStatusEnum.EvidenceTypeEnum.BEHAVIOR_FRAME.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        behavior.setOriginalUrl("mock://person/" + eventId + "/behavior");
        behavior.setAnnotatedUrl("mock://person/" + eventId + "/marked");
        behavior.setThumbnailUrl("mock://person/" + eventId + "/thumb");
        behavior.setCapturedAt(base.getOccurredAt());
        behavior.setCameraCode(base.getCameraCode());
        behavior.setSortNo(1);
        evidences.add(behavior);

        PersonEvidenceRespDTO reference = new PersonEvidenceRespDTO();
        reference.setEvidenceId(eventId + "-2");
        reference.setEvidenceType(PersonBehaviorStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name());
        reference.setOriginalUrl("mock://person/" + eventId + "/normal");
        reference.setCapturedAt(base.getOccurredAt());
        reference.setCameraCode(base.getCameraCode());
        reference.setSortNo(2);
        evidences.add(reference);

        detail.setEvidences(evidences);
        return detail;
    }

    @Override
    public List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId) {
        return records.stream().filter(r -> r.getRecordId().endsWith(eventId)).toList();
    }

    @Override
    public int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                            String statusBefore, String processResult, String processOpinion,
                            String processedBy, String source) {
        PersonEventRespDTO target = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (target == null || eventVersion == null || !eventVersion.equals(target.getEventVersion())) {
            return 0;
        }
        if (PersonBehaviorStatusEnum.ProcessStatusEnum.DONE.name().equals(target.getProcessStatus())) {
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
        record.setSource(source);
        records.add(record);
        return 1;
    }

    /* ==================== 算法任务（重新识别） ==================== */

    @Override
    public void insertAlgorithmTask(AiPersonAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(950000000L + taskSeq.incrementAndGet());
        }
        algorithmTasks.add(task);
    }

    @Override
    public void updateAlgorithmTask(AiPersonAlgorithmTask task) {
        // Service 持有并原地修改 find 返回的同一实例，列表已同步生效；
        // 这里只兜底保证行存在（不会重复插入）。
        boolean exists = algorithmTasks.stream()
            .anyMatch(t -> t.getTaskNo().equals(task.getTaskNo()));
        if (!exists) {
            algorithmTasks.add(task);
        }
    }

    @Override
    public AiPersonAlgorithmTask findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            return null;
        }
        // 与真实库实现同形：越权与不存在都返回 null
        return algorithmTasks.stream()
            .filter(t -> taskNo.equals(t.getTaskNo()))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<AiPersonAlgorithmTask> listAlgorithmTasksByEventId(AiVisionDataScope scope,
                                                                  String eventId, int limit) {
        if (eventId == null || eventId.isBlank()) {
            return List.of();
        }
        return algorithmTasks.stream()
            .filter(t -> eventId.equals(String.valueOf(t.getEventId())))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .sorted(Comparator.comparing(
                (AiPersonAlgorithmTask t) -> t.getStartedAt() != null ? t.getStartedAt() : t.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .toList();
    }

    @Override
    public int backfillEventAlgorithmResult(AiVisionDataScope scope, Long eventId, Long taskId,
                                            String algorithmStatus, String algorithmResult,
                                            BigDecimal confidence, String behaviorType,
                                            String updatedBy, LocalDateTime updatedAt) {
        if (eventId == null) {
            return 0;
        }
        PersonEventRespDTO target = events.stream()
            .filter(e -> String.valueOf(eventId).equals(e.getEventId()))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (target == null) {
            return 0;
        }
        // 只写算法侧字段；处理状态与乐观锁版本不被动算法链路推进。
        // taskId 在内存态没有承载列（列表接口经任务表查询即得），不额外落字段。
        target.setAlgorithmStatus(algorithmStatus);
        target.setAlgorithmResult(algorithmResult);
        target.setConfidence(confidence);
        if (behaviorType != null && !behaviorType.isBlank()) {
            target.setBehaviorType(behaviorType);
            try {
                target.setBehaviorTypeName(
                    PersonBehaviorStatusEnum.BehaviorTypeEnum.valueOf(behaviorType).getLabel());
            } catch (IllegalArgumentException ignored) {
                // 非法编码保留原名称，不覆盖为 null
            }
        }
        return 1;
    }
}
