package com.chinaunicom.smartstorage.admin.service.impl.groundWater.algorithm;

import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.GroundWaterAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.GroundWaterAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.enumbean.GroundWaterStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.algorithm.GroundWaterAlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.algorithm.GroundWaterMockAlgorithmProperties;
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
 * 地面积水模拟算法适配器。
 *
 * <p>依据 AI_HANDOFF.md 第 24.4 节，真实算法包未到时的唯一实现，必须支持：</p>
 * <ul>
 *   <li>正常结果（地面干燥）</li>
 *   <li>异常结果（积水/渗漏/结冰）</li>
 *   <li>无法判断</li>
 *   <li>执行失败</li>
 *   <li>执行超时</li>
 *   <li>可配置延迟</li>
 *   <li>干燥参考图与积水证据图</li>
 * </ul>
 *
 * <p><b>强制约束</b>：</p>
 * <ol>
 *   <li>模拟结果<b>必须明确标记为模拟</b>（含失败、超时与未产出的 pending 分支），
 *       不得混入真实生产统计（第 24.4 节）。</li>
 *   <li>不安装、不引用任何真实算法依赖（Python、CUDA、OpenCV、深度学习框架等）。</li>
 *   <li>不猜测真实算法的输入输出格式；<b>真实算法到达后只新增适配器实现类</b>，
 *       把厂商格式经 {@code toStandardResult} 转成统一输出，
 *       不得破坏页面接口与事件模型（第 24.5 节）。</li>
 * </ol>
 *
 * <p><b>当前形态</b>：由 {@code GroundWaterServiceImpl} 经「重新识别」链路调用
 * （提交 → 后台线程出结果 → 轮询查询取回）。本类的算法逻辑本体不因接入而改动，
 * 真实算法到达时只换一个实现类。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Slf4j
@Component
public class MockGroundWaterAlgorithmGateway implements GroundWaterAlgorithmGateway {

    /** 模拟算法参数（代码常量，不来自配置文件） */
    private final GroundWaterMockAlgorithmProperties properties = new GroundWaterMockAlgorithmProperties();

    /**
     * 任务结果缓存。
     *
     * <p>仅为本期模拟使用。真实算法接入后由算法服务与数据库承担，
     * 此缓存应随之移除。</p>
     */
    private final Map<String, GroundWaterAlgorithmTaskOutputDTO> taskStore = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String submitTask(GroundWaterAlgorithmTaskInputDTO input) {
        if (input == null || input.getPointId() == null || input.getPointId().isBlank()) {
            throw new IllegalArgumentException("监测点位标识不能为空");
        }

        final String taskId = "MOCK-TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 异步执行，模拟真实算法的任务生命周期（第 24.1 节：不得同步长时间等待）
        Thread worker = new Thread(() -> execute(taskId, input), "mock-ground-water-algorithm-" + taskId);
        worker.setDaemon(true);
        worker.start();

        log.info("[MockGroundWaterAlgorithm] 已受理模拟算法任务 taskId={} pointId={}",
                taskId, input.getPointId());
        return taskId;
    }

    /**
     * 执行模拟推理。
     *
     * <p>整体在独立线程内运行，异常与超时均被隔离，
     * 不会影响管理后台接口可用性（第 15.4 节）。</p>
     */
    private void execute(String taskId, GroundWaterAlgorithmTaskInputDTO input) {
        final LocalDateTime startedAt = LocalDateTime.now();
        try {
            Thread.sleep(properties.getBaseDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        GroundWaterAlgorithmTaskOutputDTO output = new GroundWaterAlgorithmTaskOutputDTO();
        output.setTaskId(taskId);
        output.setModelName(properties.getModelName());
        output.setModelVersion(properties.getModelVersion());
        output.setAlgorithmVersion(properties.getAlgorithmVersion());
        output.setConfigVersion(input.getConfigVersion());
        output.setStartedAt(startedAt.format(TS));
        // 第 24.4 节：本适配器产出的任何结果——含失败、超时——都是模拟结果，
        // 必须在所有分支之前就标记，否则异常路径会漏标并污染真实统计。
        output.setSimulated(Boolean.TRUE);

        // 强制分支优先，便于测试特定场景
        if (properties.isForceTimeout()) {
            finish(output, startedAt, GroundWaterStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(), null, null);
            output.setErrorCode("MOCK_TIMEOUT");
            output.setErrorMessage("模拟算法执行超时");
            taskStore.put(taskId, output);
            return;
        }

        if (properties.isForceFailed()) {
            finish(output, startedAt, GroundWaterStatusEnum.AlgorithmStatusEnum.FAILED.name(), null, null);
            output.setErrorCode("MOCK_FAILURE");
            output.setErrorMessage("模拟算法执行失败");
            taskStore.put(taskId, output);
            return;
        }

        // 按点位标识决定结果，保证同一点位结果稳定可复现
        final int seed = Math.abs(input.getPointId().hashCode() % 3);
        String conclusion;
        if (properties.isForceAnomaly()) {
            conclusion = GroundWaterStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (properties.isForceUncertain()) {
            conclusion = GroundWaterStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        } else if (seed == 0) {
            conclusion = GroundWaterStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (seed == 1) {
            conclusion = GroundWaterStatusEnum.AlgorithmResultEnum.NORMAL.name();
        } else {
            conclusion = GroundWaterStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        }

        finish(output, startedAt, GroundWaterStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(), conclusion, seed);
        output.setWaterType(
            GroundWaterStatusEnum.AlgorithmResultEnum.ANOMALY.name().equals(conclusion)
                ? GroundWaterStatusEnum.WaterTypeEnum.PONDING.name()
                : null);

        // 第 24.4 节：模拟适配器必须产出干燥参考图与积水证据图
        output.setEvidenceRefs(buildEvidenceRefs(input));

        // 无论是否异常都产出检测结果，便于页面展示
        List<GroundWaterAlgorithmTaskOutputDTO.DetectionDTO> detections = new ArrayList<>();
        GroundWaterAlgorithmTaskOutputDTO.DetectionDTO detection =
                new GroundWaterAlgorithmTaskOutputDTO.DetectionDTO();
        detection.setCategory("water_region");
        detection.setConfidence(output.getConfidence());
        detection.setBbox("[120,80,460,420]");
        detection.setTrackId("track-" + taskId.substring(taskId.length() - 6));
        detections.add(detection);
        output.setDetections(detections);

        // 原始结果引用：仅存受控标识，前端永不解析（第 24.3 节）
        output.setRawResultRef("mock://task/" + taskId + "/raw");

        taskStore.put(taskId, output);
    }

    /**
     * 补齐完成时间与耗时。
     *
     * <p>耗时含置信度缺失场景（约三分之一），
     * 用于验证页面「暂无」而非 0% 的展示（第 17.3 节）。</p>
     */
    private void finish(GroundWaterAlgorithmTaskOutputDTO output, LocalDateTime startedAt,
                        String status, String conclusion, Integer seed) {
        final LocalDateTime finishedAt = LocalDateTime.now();
        output.setExecutionStatus(status);
        output.setConclusion(conclusion);
        output.setFinishedAt(finishedAt.format(TS));
        output.setInferenceDurationMs(Duration.between(startedAt, finishedAt).toMillis());

        if (GroundWaterStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
                && seed != null && seed != 2) {
            output.setConfidence(BigDecimal.valueOf(0.55d + (seed * 0.15d))
                .setScale(4, RoundingMode.HALF_UP));
        }
    }

    /**
     * 构造模拟证据引用。
     *
     * <p>使用受控对象存储键，<b>不返回任何完整 URL</b>（第 29.1 节）。</p>
     */
    private List<GroundWaterAlgorithmTaskOutputDTO.EvidenceRefDTO> buildEvidenceRefs(
            GroundWaterAlgorithmTaskInputDTO input) {
        List<GroundWaterAlgorithmTaskOutputDTO.EvidenceRefDTO> refs = new ArrayList<>();
        final String base = "mock://evidence/" + input.getPointId() + "/" + input.getCapturedAt();
        final String captured = input.getCapturedAt();

        GroundWaterAlgorithmTaskOutputDTO.EvidenceRefDTO normal =
                new GroundWaterAlgorithmTaskOutputDTO.EvidenceRefDTO();
        normal.setEvidenceType(GroundWaterStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name());
        normal.setObjectKey(base + "/normal");
        normal.setCapturedAt(captured);
        refs.add(normal);

        GroundWaterAlgorithmTaskOutputDTO.EvidenceRefDTO water =
                new GroundWaterAlgorithmTaskOutputDTO.EvidenceRefDTO();
        water.setEvidenceType(GroundWaterStatusEnum.EvidenceTypeEnum.WATER_FRAME.name());
        water.setObjectKey(base + "/water");
        water.setCapturedAt(captured);
        refs.add(water);

        return refs;
    }

    /**
     * 查询任务结果。
     *
     * <p><b>未产出（pending）分支同样必须标记为模拟</b>：这条路径返回的对象
     * 不会经过 {@code execute}，如果在构造处漏掉 {@code setSimulated(TRUE)}，
     * 一次「任务还在排队」的查询就会被下游当成真实算法结果。</p>
     */
    @Override
    public GroundWaterAlgorithmTaskOutputDTO queryTask(String taskId) {
        GroundWaterAlgorithmTaskOutputDTO output = taskStore.get(taskId);
        if (output == null) {
            // 尚未产出结果，返回等待中（第 24.1 节状态流）
            GroundWaterAlgorithmTaskOutputDTO pending = new GroundWaterAlgorithmTaskOutputDTO();
            pending.setTaskId(taskId);
            pending.setExecutionStatus(GroundWaterStatusEnum.AlgorithmStatusEnum.QUEUED.name());
            pending.setSimulated(Boolean.TRUE);
            return pending;
        }
        return output;
    }

    @Override
    public GroundWaterAlgorithmTaskOutputDTO toStandardResult(String rawOutput) {
        // 模拟适配器不使用原始输出，直接抛不支持；
        // 真实算法接入时由新适配器实现此转换
        throw new UnsupportedOperationException("模拟适配器不支持原始输出转换");
    }

    @Override
    public String adapterName() {
        return "MockGroundWaterAlgorithmGateway";
    }

    /**
     * 获取模拟参数，供管理端或调试使用。
     */
    public GroundWaterMockAlgorithmProperties properties() {
        return properties;
    }
}
