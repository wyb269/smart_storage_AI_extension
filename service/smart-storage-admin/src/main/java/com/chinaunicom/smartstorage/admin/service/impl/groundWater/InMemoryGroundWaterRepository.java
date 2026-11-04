package com.chinaunicom.smartstorage.admin.service.impl.groundWater;

import com.chinaunicom.smartstorage.admin.constant.GroundWaterConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.groundWater.AiWaterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.GroundWaterStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.GroundWaterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 地面积水数据访问的内存兜底实现。
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled} 不为 true 时生效，
 * 便于在未注入数据库凭据的环境下安全启动与前端联调。</p>
 *
 * <p><b>数据一律标记为模拟数据</b>，不得混入真实生产统计。
 * 心跳时间每次调用时基于当前时刻计算，原因见 {@link #heartbeat(int)}。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Slf4j
@Repository
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryGroundWaterRepository implements GroundWaterRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与真实库种子保持同一批仓库标识，便于切换数据源后对照 */
    private static final String[][] WAREHOUSES = {
        {"9001", "一号粮库"}, {"9002", "二号粮库"}, {"9003", "三号粮库"}, {"9004", "散粮库"},
    };

    /** 点位名称，与原型图左侧「监测点位分布」卡片标题同一批口径 */
    private static final String[] POINT_NAMES = {
        "主通廊东侧", "主通廊西侧", "主通廊南段", "主通廊北段",
        "主通廊东南", "主通廊西南", "北通廊东侧", "北通廊西侧",
        "北通廊南段", "南通廊东侧", "卸粮坑东侧", "工作塔一层",
    };

    private final List<WaterEventRespDTO> events = new CopyOnWriteArrayList<>();
    private final List<ProcessRecordRespDTO> records = new CopyOnWriteArrayList<>();

    /**
     * 算法任务存储：key = taskNo。
     *
     * <p>与真实库形态对齐：事件 1:N 任务，「重新识别」每次追加一条。
     * ConcurrentHashMap 保证轮询读与提交写的并发安全。</p>
     */
    private final Map<String, AiWaterAlgorithmTask> tasks = new ConcurrentHashMap<>();

    /** 兜底 id 发号器；内存模式没有雪花，序号足够本实现内部自洽 */
    private final AtomicLong taskIdSeq = new AtomicLong(900_000L);

    public InMemoryGroundWaterRepository() {
        seed();
        seedTasks();
    }

    /** 生成 12 个监测点位对应的 40 条积水事件 */
    private void seed() {
        for (int i = 0; i < 40; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            String pointName = POINT_NAMES[i % POINT_NAMES.length];
            GroundWaterStatusEnum.WaterTypeEnum type =
                GroundWaterStatusEnum.WaterTypeEnum.values()[i % 4];
            // 每 4 条留 1 条未核实，用于验证「查看即标记」写入链路
            String processStatus = switch (i % 4) {
                case 0 -> GroundWaterStatusEnum.ProcessStatusEnum.PENDING.name();
                case 2 -> GroundWaterStatusEnum.ProcessStatusEnum.PROCESSING.name();
                default -> GroundWaterStatusEnum.ProcessStatusEnum.DONE.name();
            };

            WaterEventRespDTO e = new WaterEventRespDTO();
            e.setEventId(String.valueOf(2000 + i));
            e.setEventNo("WP" + TS.format(LocalDateTime.now().minusHours(i)).replace("-", "").replace(":", "")
                .substring(0, 8) + String.format("%04d", i + 1));
            e.setOccurredAt(TS.format(LocalDateTime.now().minusMinutes(30L * i)));
            e.setWarehouseId(wh[0]);
            e.setWarehouseName(wh[1]);
            e.setPointId(String.valueOf(101 + i % 12));
            e.setPointName(pointName);
            e.setCameraCode(String.format("CAM-%03d", i % 12 + 1));
            e.setWaterType(type.name());
            e.setWaterTypeName(type.getLabel());
            e.setAlgorithmStatus(GroundWaterStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            e.setAlgorithmResult(GroundWaterStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            // 约 15% 置信度缺失，验证页面显示「暂无」而不是 0%
            e.setConfidence(i % 7 == 3 ? null : new BigDecimal("0.8500"));
            e.setProcessStatus(processStatus);
            e.setEventVersion(1);
            if (!GroundWaterStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
                e.setProcessedBy("系统");
                e.setProcessedAt(TS.format(LocalDateTime.now().minusMinutes(10L * i)));
                e.setProcessOpinion("查看事件详情后自动标记为已处理");
            }
            events.add(e);
        }
    }

    /**
     * 监测点位状态。
     *
     * <p>心跳时间<b>必须每次调用时</b>基于当前时刻计算：页面按 90 分钟阈值
     * 判定心跳超时，若在这里固定时间戳，进程跑久了全部点位会被误判为离线，
     * 干燥/积水状态就永远演示不出来。</p>
     */
    @Override
    public List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope) {
        List<MonitorPointStatusRespDTO> list = new ArrayList<>();
        for (int i = 0; i < POINT_NAMES.length; i++) {
            String[] wh = WAREHOUSES[i % WAREHOUSES.length];
            if (!allows(scope, wh[0])) {
                continue;
            }
            MonitorPointStatusRespDTO p = new MonitorPointStatusRespDTO();
            p.setPointId(String.valueOf(101 + i));
            p.setPointName(POINT_NAMES[i]);
            p.setCameraCode(String.format("CAM-%03d", i + 1));
            p.setPlatformCameraId("C" + String.format("%03d", i + 1));
            p.setWarehouseId(wh[0]);
            p.setWarehouseName(wh[1]);
            // 状态分布：多数在线干燥，少量在线积水，两路离线，一路未知
            if (i == 1 || i == 8) {
                p.setDeviceStatus(GroundWaterStatusEnum.DeviceStatusEnum.OFFLINE.name());
                p.setAlarmStatus(GroundWaterStatusEnum.WaterAlarmStatusEnum.NORMAL.name());
                p.setLastHeartbeatAt(heartbeat(200));
            } else if (i == 11) {
                p.setDeviceStatus(GroundWaterStatusEnum.DeviceStatusEnum.UNKNOWN.name());
                p.setAlarmStatus(GroundWaterStatusEnum.WaterAlarmStatusEnum.NORMAL.name());
                p.setLastHeartbeatAt(null);
            } else {
                p.setDeviceStatus(GroundWaterStatusEnum.DeviceStatusEnum.ONLINE.name());
                p.setAlarmStatus(i == 3 || i == 6
                    ? GroundWaterStatusEnum.WaterAlarmStatusEnum.ACTIVE.name()
                    : GroundWaterStatusEnum.WaterAlarmStatusEnum.NORMAL.name());
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
    public List<WaterEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit) {
        return events.stream()
            .filter(e -> allows(scope, e.getWarehouseId()))
            .sorted(Comparator.comparing(WaterEventRespDTO::getOccurredAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<WaterEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                        WaterEventQueryReqDTO query) {
        return filter(scope, query);
    }

    @Override
    public List<WaterEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                      WaterEventQueryReqDTO query, int limit) {
        List<WaterEventRespDTO> list = filter(scope, query);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private List<WaterEventRespDTO> filter(AiVisionDataScope scope, WaterEventQueryReqDTO query) {
        WaterEventQueryReqDTO q = query == null ? new WaterEventQueryReqDTO() : query;
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
            .filter(e -> q.getWaterType() == null || q.getWaterType().isBlank()
                || q.getWaterType().equals(e.getWaterType()))
            .filter(e -> q.getProcessStatus() == null || q.getProcessStatus().isBlank()
                || q.getProcessStatus().equals(e.getProcessStatus()))
            .sorted(Comparator.comparing(WaterEventRespDTO::getOccurredAt).reversed())
            .toList();
    }

    @Override
    public WaterEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId) {
        WaterEventRespDTO base = events.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .filter(e -> allows(scope, e.getWarehouseId()))
            .findFirst()
            .orElse(null);
        if (base == null) {
            return null;
        }
        WaterEventDetailRespDTO detail = new WaterEventDetailRespDTO();
        org.springframework.beans.BeanUtils.copyProperties(base, detail);
        detail.setEventDesc("画面检测到地面存在积水，湿滑面积超过阈值");
        detail.setDurationSeconds(120L);
        // 内存兜底实现产出的每一条都是模拟数据，必须显式标记
        detail.setIsSimulated(true);

        WaterEvidenceRespDTO ev = new WaterEvidenceRespDTO();
        ev.setEvidenceId(eventId + "-1");
        ev.setEvidenceType(GroundWaterStatusEnum.EvidenceTypeEnum.WATER_FRAME.name());
        // 受控引用，由 Service 经 MediaGateway 换发短时地址；此处不放任何真实地址
        ev.setOriginalUrl("mock://ground-water/" + eventId + "/water");
        ev.setThumbnailUrl("mock://ground-water/" + eventId + "/thumb");
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
                            String processedBy, String source) {
        WaterEventRespDTO target = events.stream()
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
        record.setSource(source);
        records.add(record);
        return 1;
    }

    /* ==================== 算法任务（「重新识别」链路） ==================== */

    /**
     * 为每条种子事件预置一条 SUCCEEDED 历史任务。
     *
     * <p>与 07_seed_algorithm_tasks.sql 同口径：结论 ANOMALY、
     * 积水类型取事件自身、置信度缺产出时为 null、is_simulated 恒为 TRUE。
     * 时间每次读取时用绝对毫秒现算不可行（内存实体是固定值），
     * 因此按「距今偏移」在构造时锚定到最近，与事件发生时间同序。</p>
     */
    private void seedTasks() {
        int rn = 0;
        for (WaterEventRespDTO e : events) {
            rn++;
            LocalDateTime started = LocalDateTime.now().minusMinutes(30L * rn);
            AiWaterAlgorithmTask t = new AiWaterAlgorithmTask();
            t.setId(taskIdSeq.incrementAndGet());
            // task_no 前缀与 SQL 种子一致，页面切数据源可逐行对照
            t.setTaskNo("WATA" + TS.format(LocalDateTime.now())
                .replace("-", "").replace(":", "").replace(" ", "")
                + String.format("%04d", rn));
            t.setEventId(Long.parseLong(e.getEventId()));
            t.setPointId(Long.parseLong(e.getPointId()));
            t.setCameraCode(e.getCameraCode());
            t.setWarehouseId(Long.parseLong(e.getWarehouseId()));
            t.setTaskType(GroundWaterConstant.ALGORITHM_TASK_TYPE);
            t.setExecutionStatus(GroundWaterStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name());
            t.setConclusion(GroundWaterStatusEnum.AlgorithmResultEnum.ANOMALY.name());
            t.setWaterType(e.getWaterType());
            t.setConfidence(e.getConfidence());
            t.setModelName("mock-ground-water-detector");
            t.setModelVersion("mock-1.0.0");
            t.setAlgorithmVersion("mock-adapter-1.0.0");
            t.setConfigVersion(GroundWaterConstant.ALGORITHM_CONFIG_VERSION);
            t.setStartedAt(started);
            t.setFinishedAt(started.plusSeconds(1));
            t.setInferenceDurationMs(18L + (rn * 37) % 403);
            t.setDetections("[{\"category\":\"water_region\",\"bbox\":[120,80,460,420],"
                + "\"confidence\":0.91}]");
            t.setRetryCount(0);
            t.setTraceId("gw-mem-trace-" + rn);
            // 铁律 1：内存兜底产出的每一行都是模拟结果
            t.setIsSimulated(Boolean.TRUE);
            t.setCreatedBy("seed");
            t.setCreatedAt(LocalDateTime.now());
            tasks.put(t.getTaskNo(), t);
        }
    }

    @Override
    public AiWaterAlgorithmTask insertAlgorithmTask(AiWaterAlgorithmTask task) {
        if (task.getId() == null) {
            task.setId(taskIdSeq.incrementAndGet());
        }
        tasks.put(task.getTaskNo(), task);
        return task;
    }

    @Override
    public AiWaterAlgorithmTask getAlgorithmTaskByTaskNo(String taskNo, AiVisionDataScope scope) {
        AiWaterAlgorithmTask task = taskNo == null ? null : tasks.get(taskNo);
        if (task == null) {
            return null;
        }
        // 任务行自带 warehouse_id，范围判定与事件同一套 allows
        if (!allows(scope, String.valueOf(task.getWarehouseId()))) {
            return null;
        }
        return task;
    }

    @Override
    public int saveAlgorithmTaskOutcome(AiWaterAlgorithmTask task, String eventId,
                                        boolean writeBackEvent, AiVisionDataScope scope,
                                        String updatedBy) {
        // Map.put 覆盖同一引用即可（内存对象可变，与真实库 UPDATE 语义等价）
        tasks.put(task.getTaskNo(), task);

        if (writeBackEvent && eventId != null) {
            events.stream()
                .filter(e -> e.getEventId().equals(eventId))
                .filter(e -> allows(scope, e.getWarehouseId()))
                .findFirst()
                .ifPresent(e -> {
                    // 只回填算法侧字段；processStatus / eventVersion 一律不碰
                    e.setAlgorithmStatus(task.getExecutionStatus());
                    e.setAlgorithmResult(task.getConclusion());
                    e.setConfidence(task.getConfidence());
                    // water_type 是 NOT NULL 语义，任务没给新结论时保留事件原值
                    if (task.getWaterType() != null) {
                        e.setWaterType(task.getWaterType());
                    }
                });
        }
        return 1;
    }

    @Override
    public List<AiWaterAlgorithmTask> listAlgorithmTasksByEventId(AiVisionDataScope scope,
                                                                  String eventId, int limit) {
        List<AiWaterAlgorithmTask> list = new ArrayList<>(tasks.values());
        return list.stream()
            .filter(t -> t.getEventId() != null && String.valueOf(t.getEventId()).equals(eventId))
            .filter(t -> allows(scope, String.valueOf(t.getWarehouseId())))
            .sorted(Comparator.comparing(AiWaterAlgorithmTask::getStartedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .toList();
    }
}
