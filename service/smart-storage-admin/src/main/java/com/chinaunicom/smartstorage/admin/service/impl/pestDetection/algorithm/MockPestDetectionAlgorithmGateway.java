package com.chinaunicom.smartstorage.admin.service.impl.pestDetection.algorithm;

import com.chinaunicom.smartstorage.admin.constant.PestDetectionConstant;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.enumbean.PestDetectionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.algorithm.PestDetectionAlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.algorithm.PestDetectionMockAlgorithmProperties;
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
 * 粮面虫害模拟算法适配器。
 *
 * <p>真实算法包未到时的唯一实现，必须支持：正常结果、异常结果、无法判断、
 * 执行失败、执行超时、可配置延迟、正常参考图与异常证据图。</p>
 *
 * <p><b>强制约束</b>：</p>
 * <ol>
 *   <li>模拟结果<b>必须明确标记为模拟</b>（含失败、超时与 pending 分支），
 *       不得混入真实生产统计。</li>
 *   <li>不安装、不引用任何真实算法依赖（Python、CUDA、OpenCV、深度学习框架等）。</li>
 *   <li>不猜测真实算法的输入输出格式；真实算法到达后<b>只新增适配器实现类</b>，
 *       把厂商格式经 {@code toStandardResult} 转成统一输出，
 *       不得破坏页面接口与事件模型。</li>
 * </ol>
 *
 * <p><b>本期形态</b>：已接入「重新识别」链路（{@code PestDetectionServiceImpl}），
 * 本类的算法逻辑本体不变：真实算法到达时仍然只是换一个实现类。</p>
 *
 * @author 粮面虫害智能识别功能
 */
@Slf4j
@Component
public class MockPestDetectionAlgorithmGateway implements PestDetectionAlgorithmGateway {

    /** 模拟算法参数（代码常量，不来自配置文件） */
    private final PestDetectionMockAlgorithmProperties properties = new PestDetectionMockAlgorithmProperties();

    /**
     * 任务结果缓存。
     *
     * <p>仅为本期模拟使用。真实算法接入后由算法服务与数据库承担，此缓存应随之移除。</p>
     */
    private final Map<String, PestAlgorithmTaskOutputDTO> taskStore = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String submitTask(PestAlgorithmTaskInputDTO input) {
        if (input == null || input.getPointId() == null || input.getPointId().isBlank()) {
            throw new IllegalArgumentException("粮面点位标识不能为空");
        }

        final String taskId = "MOCK-TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 异步执行，模拟真实算法的任务生命周期（不得同步长时间等待）
        Thread worker = new Thread(() -> execute(taskId, input), "mock-pest-algorithm-" + taskId);
        worker.setDaemon(true);
        worker.start();

        log.info("[MockPestAlgorithm] 已受理模拟算法任务 taskId={} pointId={}", taskId, input.getPointId());
        return taskId;
    }

    /**
     * 执行模拟推理。
     *
     * <p>整体在独立线程内运行，异常与超时均被隔离，不会影响管理后台接口可用性。</p>
     */
    private void execute(String taskId, PestAlgorithmTaskInputDTO input) {
        final LocalDateTime startedAt = LocalDateTime.now();
        try {
            Thread.sleep(properties.getBaseDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        PestAlgorithmTaskOutputDTO output = new PestAlgorithmTaskOutputDTO();
        output.setTaskId(taskId);
        output.setModelName(properties.getModelName());
        output.setModelVersion(properties.getModelVersion());
        output.setAlgorithmVersion(properties.getAlgorithmVersion());
        output.setConfigVersion(input.getConfigVersion());
        output.setStartedAt(startedAt.format(TS));
        // 本适配器产出的任何结果——含失败、超时与 pending——都是模拟结果，
        // 必须在任何分支之前就标记，否则异常路径会漏标并污染真实统计。
        output.setSimulated(Boolean.TRUE);

        // 密度阈值：入参未指定时兜底用服务端常量，绝不写死数值
        final BigDecimal threshold = input.getDensityThreshold() != null
            ? input.getDensityThreshold()
            : PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD;
        output.setDensityThreshold(threshold);

        // 强制分支优先，便于测试特定场景
        if (properties.isForceTimeout()) {
            finish(output, startedAt, PestDetectionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(), null, null);
            output.setErrorCode("MOCK_TIMEOUT");
            output.setErrorMessage("模拟算法执行超时");
            taskStore.put(taskId, output);
            return;
        }

        if (properties.isForceFailed()) {
            finish(output, startedAt, PestDetectionStatusEnum.AlgorithmStatusEnum.FAILED.name(), null, null);
            output.setErrorCode("MOCK_FAILURE");
            output.setErrorMessage("模拟算法执行失败");
            taskStore.put(taskId, output);
            return;
        }

        // 按点位标识末位决定结果，保证同一点位结果稳定可复现
        final int seed = Math.abs(input.getPointId().hashCode() % 3);
        String conclusion;
        if (properties.isForceAnomaly()) {
            conclusion = PestDetectionStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (properties.isForceUncertain()) {
            conclusion = PestDetectionStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        } else if (seed == 0) {
            conclusion = PestDetectionStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (seed == 1) {
            conclusion = PestDetectionStatusEnum.AlgorithmResultEnum.NORMAL.name();
        } else {
            conclusion = PestDetectionStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        }

        finish(output, startedAt, PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(), conclusion, seed);

        // 异常时给出模拟密度与结论类型：density > threshold 即判密度超标
        if (PestDetectionStatusEnum.AlgorithmResultEnum.ANOMALY.name().equals(conclusion)) {
            BigDecimal density = threshold.multiply(new BigDecimal("1.4")).setScale(2, java.math.RoundingMode.HALF_UP);
            output.setDensity(density);
            output.setConclusionType(
                density.compareTo(threshold) > 0
                    ? PestDetectionStatusEnum.ConclusionTypeEnum.DENSITY_HIGH.name()
                    : PestDetectionStatusEnum.ConclusionTypeEnum.GRAIN_INSECT.name());
            output.setInsectSpecies("玉米象");
        } else {
            output.setDensity(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP));
            output.setConclusionType(null);
            output.setInsectSpecies(null);
        }

        // 模拟适配器必须产出正常参考图与异常证据图
        output.setEvidenceRefs(buildEvidenceRefs(input));

        // 无论是否异常都产出检测结果，便于页面展示
        List<PestAlgorithmTaskOutputDTO.DetectionDTO> detections = new ArrayList<>();
        PestAlgorithmTaskOutputDTO.DetectionDTO detection = new PestAlgorithmTaskOutputDTO.DetectionDTO();
        detection.setCategory("grain_surface_insect");
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
     * 补齐完成时间与耗时。
     *
     * <p>耗时含置信度缺失场景（约 15%），用于验证页面「暂无」而非 0% 的展示。</p>
     */
    private void finish(PestAlgorithmTaskOutputDTO output, LocalDateTime startedAt,
                        String status, String conclusion, Integer seed) {
        final LocalDateTime finishedAt = LocalDateTime.now();
        output.setExecutionStatus(status);
        output.setConclusion(conclusion);
        output.setFinishedAt(finishedAt.format(TS));
        output.setInferenceDurationMs(java.time.Duration.between(startedAt, finishedAt).toMillis());

        if (PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
                && seed != null && seed != 2) {
            output.setConfidence(
                BigDecimal.valueOf(0.55d + (seed * 0.15d)).setScale(4, java.math.RoundingMode.HALF_UP));
        }
    }

    /**
     * 构造模拟证据引用。
     *
     * <p>使用受控对象存储键，不返回任何完整 URL。</p>
     */
    private List<PestAlgorithmTaskOutputDTO.EvidenceRefDTO> buildEvidenceRefs(PestAlgorithmTaskInputDTO input) {
        List<PestAlgorithmTaskOutputDTO.EvidenceRefDTO> refs = new ArrayList<>();
        final String base = "mock://evidence/" + input.getPointId() + "/" + input.getCapturedAt();
        final String captured = input.getCapturedAt();

        PestAlgorithmTaskOutputDTO.EvidenceRefDTO normal = new PestAlgorithmTaskOutputDTO.EvidenceRefDTO();
        normal.setEvidenceType(PestDetectionStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name());
        normal.setObjectKey(base + "/normal");
        normal.setCapturedAt(captured);
        refs.add(normal);

        PestAlgorithmTaskOutputDTO.EvidenceRefDTO pest = new PestAlgorithmTaskOutputDTO.EvidenceRefDTO();
        pest.setEvidenceType(PestDetectionStatusEnum.EvidenceTypeEnum.PEST_FRAME.name());
        pest.setObjectKey(base + "/pest");
        pest.setCapturedAt(captured);
        refs.add(pest);

        return refs;
    }

    @Override
    public PestAlgorithmTaskOutputDTO queryTask(String taskId) {
        PestAlgorithmTaskOutputDTO output = taskStore.get(taskId);
        if (output == null) {
            // 尚未产出结果，返回等待中。pending 分支同样必须标记为模拟结果。
            PestAlgorithmTaskOutputDTO pending = new PestAlgorithmTaskOutputDTO();
            pending.setTaskId(taskId);
            pending.setExecutionStatus(PestDetectionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
            pending.setSimulated(Boolean.TRUE);
            return pending;
        }
        return output;
    }

    @Override
    public PestAlgorithmTaskOutputDTO toStandardResult(String rawOutput) {
        // 模拟适配器不使用原始输出，直接拒绝；真实算法接入时由新适配器实现此转换
        throw new UnsupportedOperationException("模拟适配器不支持原始输出转换");
    }

    @Override
    public String adapterName() {
        return "MockPestDetectionAlgorithmGateway";
    }

    /**
     * 获取模拟参数，供管理端或调试使用。
     */
    public PestDetectionMockAlgorithmProperties properties() {
        return properties;
    }
}
