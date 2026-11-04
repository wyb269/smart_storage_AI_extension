package com.chinaunicom.smartstorage.admin.service.impl.perimeterIntrusion.algorithm;

import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm.PerimeterAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm.PerimeterAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.enumbean.PerimeterIntrusionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.algorithm.PerimeterIntrusionAlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.algorithm.PerimeterIntrusionMockAlgorithmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 周界入侵模拟算法适配器。
 *
 * <p>依据 AI_HANDOFF.md 第 24.4 节，真实算法包未到时的唯一实现，必须支持：</p>
 * <ul>
 *   <li>正常结果</li>
 *   <li>异常结果</li>
 *   <li>无法判断</li>
 *   <li>执行失败</li>
 *   <li>执行超时</li>
 *   <li>可配置延迟</li>
 *   <li>正常参考图与入侵证据图</li>
 * </ul>
 *
 * <p><b>强制约束</b>：</p>
 * <ol>
 *   <li>模拟结果<b>必须明确标记为模拟</b>——含失败、超时与「尚未产出」三条
 *       非成功路径（第 24.4 节），不得混入真实生产统计。</li>
 *   <li>不安装、不引用任何真实算法依赖（Python、CUDA、OpenCV、深度学习框架等）。</li>
 *   <li>不猜测真实算法的输入输出格式；<b>真实算法到达后只新增适配器实现类</b>，
 *       把厂商格式经 {@code toStandardResult} 转成统一输出，
 *       不得破坏页面接口与事件模型（第 24.5 节）。</li>
 * </ol>
 *
 * <p><b>本期形态</b>：预留接缝，无任何注入点，不被 Service/Controller 调用。
 * 与遮挡那一套的交付现状一致。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Slf4j
@Component
public class MockPerimeterIntrusionAlgorithmGateway implements PerimeterIntrusionAlgorithmGateway {

    /** 模拟算法参数（代码常量，不来自配置文件） */
    private final PerimeterIntrusionMockAlgorithmProperties properties =
            new PerimeterIntrusionMockAlgorithmProperties();

    /**
     * 任务结果缓存。
     *
     * <p>仅为本期模拟使用。真实算法接入后由算法服务与数据库承担，
     * 此缓存应随之移除。</p>
     */
    private final Map<String, PerimeterAlgorithmTaskOutputDTO> taskStore = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String submitTask(PerimeterAlgorithmTaskInputDTO input) {
        if (input == null || input.getPointId() == null || input.getPointId().isBlank()) {
            throw new IllegalArgumentException("防区段标识不能为空");
        }

        final String taskId = "MOCK-TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 异步执行，模拟真实算法的任务生命周期（第24.1节：不得同步长时间等待）
        Thread worker = new Thread(() -> execute(taskId, input), "mock-perimeter-algorithm-" + taskId);
        worker.setDaemon(true);
        worker.start();

        log.info("[MockPerimeterAlgorithm] 已受理模拟算法任务 taskId={} pointId={}",
                taskId, input.getPointId());
        return taskId;
    }

    /**
     * 执行模拟推理。
     *
     * <p>整体在独立线程内运行，异常与超时均被隔离，
     * 不会影响管理后台接口可用性（第 15.4 节）。</p>
     */
    private void execute(String taskId, PerimeterAlgorithmTaskInputDTO input) {
        final LocalDateTime startedAt = LocalDateTime.now();
        try {
            Thread.sleep(properties.getBaseDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        PerimeterAlgorithmTaskOutputDTO output = new PerimeterAlgorithmTaskOutputDTO();
        output.setTaskId(taskId);
        output.setModelName(properties.getModelName());
        output.setModelVersion(properties.getModelVersion());
        output.setAlgorithmVersion(properties.getAlgorithmVersion());
        output.setConfigVersion(input.getConfigVersion());
        output.setStartedAt(startedAt.format(TS));
        // 第24.4节：本适配器产出的任何结果——含失败、超时与撤防跳过——都是模拟结果，
        // 必须在分支之前就标记，否则异常路径会漏标并污染真实统计。
        output.setSimulated(Boolean.TRUE);

        // 强制分支优先，便于测试特定场景
        if (properties.isForceTimeout()) {
            finish(output, startedAt, PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(), null, null);
            output.setErrorCode("MOCK_TIMEOUT");
            output.setErrorMessage("模拟算法执行超时");
            taskStore.put(taskId, output);
            return;
        }

        if (properties.isForceFailed()) {
            finish(output, startedAt, PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.FAILED.name(), null, null);
            output.setErrorCode("MOCK_FAILURE");
            output.setErrorMessage("模拟算法执行失败");
            taskStore.put(taskId, output);
            return;
        }

        // 撤防的防区段不做识别：这是周界功能相对遮挡那一版多出来的一条分支。
        // 结论留空而不是伪造 NORMAL，否则统计里会出现「撤防期间一切正常」的假安全。
        if (PerimeterIntrusionStatusEnum.DefenseStatusEnum.DISARMED.name().equals(input.getDefenseStatus())) {
            finish(output, startedAt, PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SKIPPED.name(), null, null);
            taskStore.put(taskId, output);
            return;
        }

        // 按防区段标识决定结果，保证同一段结果稳定可复现
        final int seed = Math.abs(input.getPointId().hashCode() % 3);
        String conclusion;
        if (properties.isForceAnomaly()) {
            conclusion = PerimeterIntrusionStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (properties.isForceUncertain()) {
            conclusion = PerimeterIntrusionStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        } else if (seed == 0) {
            conclusion = PerimeterIntrusionStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (seed == 1) {
            conclusion = PerimeterIntrusionStatusEnum.AlgorithmResultEnum.NORMAL.name();
        } else {
            conclusion = PerimeterIntrusionStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        }

        finish(output, startedAt, PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
                conclusion, seed);
        output.setIntrusionType(
            PerimeterIntrusionStatusEnum.AlgorithmResultEnum.ANOMALY.name().equals(conclusion)
                ? PerimeterIntrusionStatusEnum.IntrusionTypeEnum.CLIMB.name()
                : null);

        // 第24.4节：模拟适配器必须产出正常参考图与入侵证据图
        output.setEvidenceRefs(buildEvidenceRefs(input));

        // 无论是否异常都产出检测结果，便于页面展示
        List<PerimeterAlgorithmTaskOutputDTO.DetectionDTO> detections = new ArrayList<>();
        PerimeterAlgorithmTaskOutputDTO.DetectionDTO detection =
                new PerimeterAlgorithmTaskOutputDTO.DetectionDTO();
        detection.setCategory("intrusion_region");
        detection.setConfidence(output.getConfidence());
        detection.setBbox("[120,80,460,420]");
        detection.setTrackId("track-" + taskId.substring(taskId.length() - 6));
        detections.add(detection);
        output.setDetections(detections);

        // 原始结果引用：仅存受控标识，前端永不解析（第24.3节）
        output.setRawResultRef("mock://task/" + taskId + "/raw");

        taskStore.put(taskId, output);
    }

    /**
     * 补齐完成时间与耗时。
     *
     * <p>耗时含置信度缺失场景（约 15%），
     * 用于验证页面「暂无」而非 0% 的展示（第 17.3 节）。</p>
     */
    private void finish(PerimeterAlgorithmTaskOutputDTO output, LocalDateTime startedAt,
                        String status, String conclusion, Integer seed) {
        final LocalDateTime finishedAt = LocalDateTime.now();
        output.setExecutionStatus(status);
        output.setConclusion(conclusion);
        output.setFinishedAt(finishedAt.format(TS));
        output.setInferenceDurationMs(
            java.time.Duration.between(startedAt, finishedAt).toMillis());

        if (PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
                && seed != null && seed != 2) {
            output.setConfidence(BigDecimal.valueOf(0.55d + (seed * 0.15d))
                    .setScale(4, java.math.RoundingMode.HALF_UP));
        }
    }

    /**
     * 构造模拟证据引用。
     *
     * <p>使用受控对象存储键，<b>不返回任何完整 URL</b>（第 29.1 节）。</p>
     */
    private List<PerimeterAlgorithmTaskOutputDTO.EvidenceRefDTO> buildEvidenceRefs(
            PerimeterAlgorithmTaskInputDTO input) {
        List<PerimeterAlgorithmTaskOutputDTO.EvidenceRefDTO> refs = new ArrayList<>();
        final String base = "mock://evidence/" + input.getPointId() + "/" + input.getCapturedAt();
        final String captured = input.getCapturedAt();

        PerimeterAlgorithmTaskOutputDTO.EvidenceRefDTO normal =
                new PerimeterAlgorithmTaskOutputDTO.EvidenceRefDTO();
        normal.setEvidenceType(PerimeterIntrusionStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name());
        normal.setObjectKey(base + "/normal");
        normal.setCapturedAt(captured);
        refs.add(normal);

        PerimeterAlgorithmTaskOutputDTO.EvidenceRefDTO intrusion =
                new PerimeterAlgorithmTaskOutputDTO.EvidenceRefDTO();
        intrusion.setEvidenceType(PerimeterIntrusionStatusEnum.EvidenceTypeEnum.INTRUSION_FRAME.name());
        intrusion.setObjectKey(base + "/intrusion");
        intrusion.setCapturedAt(captured);
        refs.add(intrusion);

        return refs;
    }

    @Override
    public PerimeterAlgorithmTaskOutputDTO queryTask(String taskId) {
        PerimeterAlgorithmTaskOutputDTO output = taskStore.get(taskId);
        if (output == null) {
            // 尚未产出结果，返回等待中（第24.1节状态流）。
            // 这条分支同样必须标 simulated：pending 对象也会被上层当作结果消费。
            PerimeterAlgorithmTaskOutputDTO pending = new PerimeterAlgorithmTaskOutputDTO();
            pending.setTaskId(taskId);
            pending.setExecutionStatus(PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
            pending.setSimulated(Boolean.TRUE);
            return pending;
        }
        return output;
    }

    @Override
    public PerimeterAlgorithmTaskOutputDTO toStandardResult(String rawOutput) {
        // 模拟适配器不使用原始输出，直接返回当前任务结果
        // 真实算法接入时由新适配器实现此转换
        throw new UnsupportedOperationException("模拟适配器不支持原始输出转换");
    }

    @Override
    public String adapterName() {
        return "MockPerimeterIntrusionAlgorithmGateway";
    }

    /**
     * 获取模拟参数，供管理端或调试使用。
     */
    public PerimeterIntrusionMockAlgorithmProperties properties() {
        return properties;
    }
}
