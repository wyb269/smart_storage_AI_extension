package com.chinaunicom.smartstorage.admin.service.impl.vehicleRecognition.algorithm;

import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.enumbean.VehicleRecognitionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.algorithm.VehicleRecognitionMockAlgorithmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆识别模拟算法适配器。
 *
 * <p>真实算法包未到时的唯一实现，必须支持：正常识别、未识别到车辆、无法判断、
 * 执行失败、执行超时、可配置延迟、车辆全景图与车牌特写证据。</p>
 *
 * <p><b>强制约束</b>：</p>
 * <ol>
 *   <li>模拟结果<b>必须明确标记 is_simulated=true</b>（含失败/超时/pending 分支），
 *       不得混入真实生产统计。</li>
 *   <li>不安装、不引用任何真实算法依赖（Python、CUDA、OpenCV、深度学习框架等）。</li>
 *   <li>不猜测真实算法的输入输出格式；真实算法到达后只新增适配器实现类，
 *       把厂商格式经 {@code toStandardResult} 转成统一输出，
 *       不得破坏页面接口与事件模型。</li>
 *   <li><b>PII</b>：车牌号属于个人轨迹信息，日志严禁打印完整车牌，只打掩码。</li>
 * </ol>
 *
 * <p><b>本期形态</b>：预留接缝，无任何注入点，不被 Service/Controller 调用。
 * 与仓门异动 / 视频遮挡那一套的交付现状一致。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Slf4j
@Component
public class MockVehicleRecognitionAlgorithmGateway implements VehicleRecognitionAlgorithmGateway {

    /** 模拟算法参数（代码常量，不来自配置文件） */
    private final VehicleRecognitionMockAlgorithmProperties properties =
            new VehicleRecognitionMockAlgorithmProperties();

    /**
     * 任务结果缓存。仅为本期模拟使用。真实算法接入后由算法服务与数据库承担，此缓存应随之移除。
     */
    private final Map<String, VehicleRecognitionAlgorithmTaskOutputDTO> taskStore = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String submitTask(VehicleRecognitionAlgorithmTaskInputDTO input) {
        if (input == null || input.getPointId() == null || input.getPointId().isBlank()) {
            throw new IllegalArgumentException("识别点位标识不能为空");
        }

        final String taskId = "MOCK-VEH-TASK-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 异步执行，模拟真实算法的任务生命周期（不得同步长时间等待）
        Thread worker = new Thread(() -> execute(taskId, input), "mock-vehicle-algorithm-" + taskId);
        worker.setDaemon(true);
        worker.start();

        // 只打点位标识，不打车牌（PII）
        log.info("[MockVehicleAlgorithm] 已受理模拟算法任务 taskId={} pointId={}", taskId, input.getPointId());
        return taskId;
    }

    /**
     * 执行模拟推理。整体在独立线程内运行，异常与超时均被隔离，
     * 不会影响管理后台接口可用性。
     */
    private void execute(String taskId, VehicleRecognitionAlgorithmTaskInputDTO input) {
        final LocalDateTime startedAt = LocalDateTime.now();
        try {
            Thread.sleep(properties.getBaseDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        VehicleRecognitionAlgorithmTaskOutputDTO output = new VehicleRecognitionAlgorithmTaskOutputDTO();
        output.setTaskId(taskId);
        output.setModelName(properties.getModelName());
        output.setModelVersion(properties.getModelVersion());
        output.setAlgorithmVersion(properties.getAlgorithmVersion());
        output.setConfigVersion(input.getConfigVersion());
        output.setStartedAt(startedAt.format(TS));
        // 本适配器产出的任何结果——含失败、超时与 pending——都是模拟结果，
        // 必须在所有分支之前就标记，否则异常路径会漏标并污染真实统计。
        output.setSimulated(Boolean.TRUE);

        // 强制分支优先，便于测试特定场景
        if (properties.isForceTimeout()) {
            finish(output, startedAt, VehicleRecognitionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(), null, null);
            output.setErrorCode("MOCK_TIMEOUT");
            output.setErrorMessage("模拟算法执行超时");
            taskStore.put(taskId, output);
            return;
        }

        if (properties.isForceFailed()) {
            finish(output, startedAt, VehicleRecognitionStatusEnum.AlgorithmStatusEnum.FAILED.name(), null, null);
            output.setErrorCode("MOCK_FAILURE");
            output.setErrorMessage("模拟算法执行失败");
            taskStore.put(taskId, output);
            return;
        }

        // 按点位标识决定结果，保证同一点位结果稳定可复现
        final int seed = Math.abs(input.getPointId().hashCode() % 3);
        String conclusion;
        if (properties.isForceNoVehicle()) {
            conclusion = "NO_VEHICLE";
        } else if (properties.isForceUncertain()) {
            conclusion = "UNCERTAIN";
        } else if (seed == 0) {
            conclusion = "NO_VEHICLE";
        } else if (seed == 1) {
            conclusion = "RECOGNIZED";
        } else {
            conclusion = "UNCERTAIN";
        }

        finish(output, startedAt, VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(), conclusion, seed);

        if ("RECOGNIZED".equals(conclusion)) {
            // 模拟识别结论：车辆类型与方向；车牌为占位值，绝不写入日志
            output.setVehicleType("柴油货车");
            output.setPlateNo("黑A00000");
            output.setDirection(VehicleRecognitionStatusEnum.DirectionEnum.IN.name());
        }

        // 模拟适配器必须产出车辆全景图与车牌特写证据
        output.setEvidenceRefs(buildEvidenceRefs(input));

        // 无论是否识别到车辆都产出检测结果，便于页面展示
        List<VehicleRecognitionAlgorithmTaskOutputDTO.DetectionDTO> detections = new ArrayList<>();
        VehicleRecognitionAlgorithmTaskOutputDTO.DetectionDTO detection =
                new VehicleRecognitionAlgorithmTaskOutputDTO.DetectionDTO();
        detection.setCategory("vehicle");
        detection.setConfidence(output.getConfidence());
        detection.setBbox("[120,80,460,420]");
        detection.setTrackId("track-" + taskId.substring(taskId.length() - 6));
        detections.add(detection);
        output.setDetections(detections);

        // 原始结果引用：仅存受控标识，前端永不解析
        output.setRawResultRef("mock://task/" + taskId + "/raw");

        taskStore.put(taskId, output);
    }

    /**
     * 补齐完成时间与耗时。耗时含置信度缺失场景，用于验证页面「暂无」而非 0% 的展示。
     */
    private void finish(VehicleRecognitionAlgorithmTaskOutputDTO output, LocalDateTime startedAt,
                        String status, String conclusion, Integer seed) {
        final LocalDateTime finishedAt = LocalDateTime.now();
        output.setExecutionStatus(status);
        output.setConclusion(conclusion);
        output.setFinishedAt(finishedAt.format(TS));
        output.setInferenceDurationMs(Duration.between(startedAt, finishedAt).toMillis());

        if (VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
                && seed != null && seed != 2) {
            output.setConfidence(BigDecimal.valueOf(0.55d + (seed * 0.15d)).setScale(4, RoundingMode.HALF_UP));
        }
    }

    /**
     * 构造模拟证据引用。使用受控对象存储键，<b>不返回任何完整 URL</b>。
     */
    private List<VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO> buildEvidenceRefs(
            VehicleRecognitionAlgorithmTaskInputDTO input) {
        List<VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO> refs = new ArrayList<>();
        final String base = "mock://evidence/" + input.getPointId() + "/" + input.getCapturedAt();
        final String captured = input.getCapturedAt();

        VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO full =
                new VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO();
        full.setEvidenceType(VehicleRecognitionStatusEnum.EvidenceTypeEnum.VEHICLE_IMAGE.name());
        full.setObjectKey(base + "/vehicle");
        full.setCapturedAt(captured);
        refs.add(full);

        VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO plate =
                new VehicleRecognitionAlgorithmTaskOutputDTO.EvidenceRefDTO();
        plate.setEvidenceType(VehicleRecognitionStatusEnum.EvidenceTypeEnum.PLATE_CROP.name());
        plate.setObjectKey(base + "/plate");
        plate.setCapturedAt(captured);
        refs.add(plate);

        return refs;
    }

    @Override
    public VehicleRecognitionAlgorithmTaskOutputDTO queryTask(String taskId) {
        VehicleRecognitionAlgorithmTaskOutputDTO output = taskStore.get(taskId);
        if (output == null) {
            // 尚未产出结果，返回等待中。pending 分支同样必须标记为模拟结果。
            VehicleRecognitionAlgorithmTaskOutputDTO pending = new VehicleRecognitionAlgorithmTaskOutputDTO();
            pending.setTaskId(taskId);
            pending.setExecutionStatus(VehicleRecognitionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
            pending.setSimulated(Boolean.TRUE);
            return pending;
        }
        return output;
    }

    @Override
    public VehicleRecognitionAlgorithmTaskOutputDTO toStandardResult(String rawOutput) {
        // 模拟适配器不使用原始输出，直接抛不支持；真实算法接入时由新适配器实现此转换
        throw new UnsupportedOperationException("模拟适配器不支持原始输出转换");
    }

    @Override
    public String adapterName() {
        return "MockVehicleRecognitionAlgorithmGateway";
    }

    /**
     * 获取模拟参数，供管理端或调试使用。
     */
    public VehicleRecognitionMockAlgorithmProperties properties() {
        return properties;
    }
}
