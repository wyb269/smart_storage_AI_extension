package com.chinaunicom.smartstorage.admin.service.impl.doorAnomaly.algorithm;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.enumbean.DoorAnomalyStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.algorithm.AlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.algorithm.MockAlgorithmProperties;
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
 * 模拟算法适配器。
 *
 * <p>依据 AI_HANDOFF.md 第 24.4 节，真实算法包未到时的唯一实现，必须支持：</p>
 * <ul>
 *   <li>正常结果</li>
 *   <li>异常结果</li>
 *   <li>无法判断</li>
 *   <li>执行失败</li>
 *   <li>执行超时</li>
 *   <li>可配置延迟</li>
 *   <li>正常参考图与异常证据图</li>
 * </ul>
 *
 * <p><b>强制约束</b>：</p>
 * <ol>
 *   <li>模拟结果<b>必须明确标记为 MOCK</b>，不得混入真实生产统计（第 24.4 节）。</li>
 *   <li>不安装、不引用任何真实算法依赖（Python、CUDA、OpenCV、深度学习框架等）。</li>
 *   <li>不猜测真实算法的输入输出格式；真实算法接入时只新增适配器实现类。</li>
 * </ol>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Component
public class MockAlgorithmGateway implements AlgorithmGateway {

    /** 模拟算法参数（代码常量，不来自配置文件） */
    private final MockAlgorithmProperties properties = new MockAlgorithmProperties();

    /**
     * 任务结果缓存。
     *
     * <p>仅为本期模拟使用。真实算法接入后由算法服务与数据库承担，
     * 此缓存应随之移除。</p>
     */
    private final Map<String, AlgorithmTaskOutputDTO> taskStore = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String submitTask(AlgorithmTaskInputDTO input) {
        if (input == null || input.getDoorId() == null || input.getDoorId().isBlank()) {
            throw new IllegalArgumentException("仓门标识不能为空");
        }

        final String taskId = "MOCK-TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 异步执行，模拟真实算法的任务生命周期（第24.1节：不得同步长时间等待）
        Thread worker = new Thread(() -> execute(taskId, input), "mock-algorithm-" + taskId);
        worker.setDaemon(true);
        worker.start();

        log.info("[MockAlgorithm] 已受理模拟算法任务 taskId={} doorId={}", taskId, input.getDoorId());
        return taskId;
    }

    /**
     * 执行模拟推理。
     *
     * <p>整体在独立线程内运行，异常与超时均被隔离，
     * 不会影响管理后台接口可用性（第 15.4 节）。</p>
     */
    private void execute(String taskId, AlgorithmTaskInputDTO input) {
        final LocalDateTime startedAt = LocalDateTime.now();
        try {
            Thread.sleep(properties.getBaseDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        AlgorithmTaskOutputDTO output = new AlgorithmTaskOutputDTO();
        output.setTaskId(taskId);
        // 模拟标记是本适配器的不变量，必须在任何分支之前设好：
        // 之前只在成功分支标记，TIMEOUT / FAILED 两条出口漏标，
        // simulated 留成 null 会被下游当成真实结果统计（第 24.4 节）。
        output.setSimulated(Boolean.TRUE);
        output.setModelName(properties.getModelName());
        output.setModelVersion(properties.getModelVersion());
        output.setAlgorithmVersion(properties.getAlgorithmVersion());
        output.setConfigVersion(input.getConfigVersion());
        output.setStartedAt(startedAt.format(TS));

        // 强制分支优先，便于测试特定场景
        if (properties.isForceTimeout()) {
            finish(output, startedAt, DoorAnomalyStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(), null, null);
            output.setErrorCode("MOCK_TIMEOUT");
            output.setErrorMessage("模拟算法执行超时");
            taskStore.put(taskId, output);
            return;
        }

        if (properties.isForceFailed()) {
            finish(output, startedAt, DoorAnomalyStatusEnum.AlgorithmStatusEnum.FAILED.name(), null, null);
            output.setErrorCode("MOCK_FAILURE");
            output.setErrorMessage("模拟算法执行失败");
            taskStore.put(taskId, output);
            return;
        }

        // 按仓门标识末位数字决定结果，保证同一仓门结果稳定可复现
        final int seed = Math.abs(input.getDoorId().hashCode() % 3);
        String conclusion;
        if (properties.isForceAnomaly()) {
            conclusion = DoorAnomalyStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (properties.isForceUncertain()) {
            conclusion = DoorAnomalyStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        } else if (seed == 0) {
            conclusion = DoorAnomalyStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (seed == 1) {
            conclusion = DoorAnomalyStatusEnum.AlgorithmResultEnum.NORMAL.name();
        } else {
            conclusion = DoorAnomalyStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        }

        finish(output, startedAt, DoorAnomalyStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(), conclusion, seed);
        output.setAnomalyType(
            DoorAnomalyStatusEnum.AlgorithmResultEnum.ANOMALY.name().equals(conclusion)
                ? DoorAnomalyStatusEnum.AnomalyTypeEnum.ILLEGAL_OPEN.name()
                : null);

        // 第24.4节：模拟适配器必须产出正常参考图与异常证据图
        output.setEvidenceRefs(buildEvidenceRefs(input));

        // 无论是否异常都产出检测结果，便于页面展示
        List<AlgorithmTaskOutputDTO.DetectionDTO> detections = new ArrayList<>();
        AlgorithmTaskOutputDTO.DetectionDTO detection = new AlgorithmTaskOutputDTO.DetectionDTO();
        detection.setCategory("door_panel");
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
    private void finish(AlgorithmTaskOutputDTO output, LocalDateTime startedAt,
                        String status, String conclusion, Integer seed) {
        final LocalDateTime finishedAt = LocalDateTime.now();
        output.setExecutionStatus(status);
        output.setConclusion(conclusion);
        output.setFinishedAt(finishedAt.format(TS));
        output.setInferenceDurationMs(
            java.time.Duration.between(startedAt, finishedAt).toMillis());

        if (DoorAnomalyStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
                && seed != null && seed != 2) {
            output.setConfidence(BigDecimal.valueOf(0.55d + (seed * 0.15d)).setScale(4, java.math.RoundingMode.HALF_UP));
        }
    }

    /**
     * 构造模拟证据引用。
     *
     * <p>使用受控对象存储键，<b>不返回任何完整 URL</b>（第 29.1 节）。</p>
     */
    private List<AlgorithmTaskOutputDTO.EvidenceRefDTO> buildEvidenceRefs(AlgorithmTaskInputDTO input) {
        List<AlgorithmTaskOutputDTO.EvidenceRefDTO> refs = new ArrayList<>();
        final String base = "mock://evidence/" + input.getDoorId() + "/" + input.getCapturedAt();
        final String captured = input.getCapturedAt();

        AlgorithmTaskOutputDTO.EvidenceRefDTO normal = new AlgorithmTaskOutputDTO.EvidenceRefDTO();
        normal.setEvidenceType(DoorAnomalyStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name());
        normal.setObjectKey(base + "/normal");
        normal.setCapturedAt(captured);
        refs.add(normal);

        AlgorithmTaskOutputDTO.EvidenceRefDTO abnormal = new AlgorithmTaskOutputDTO.EvidenceRefDTO();
        abnormal.setEvidenceType(DoorAnomalyStatusEnum.EvidenceTypeEnum.ABNORMAL_KEY_FRAME.name());
        abnormal.setObjectKey(base + "/abnormal");
        abnormal.setCapturedAt(captured);
        refs.add(abnormal);

        return refs;
    }

    @Override
    public AlgorithmTaskOutputDTO queryTask(String taskId) {
        AlgorithmTaskOutputDTO output = taskStore.get(taskId);
        if (output == null) {
            // 尚未产出结果，返回等待中（第24.1节状态流）
            AlgorithmTaskOutputDTO pending = new AlgorithmTaskOutputDTO();
            pending.setTaskId(taskId);
            pending.setExecutionStatus(DoorAnomalyStatusEnum.AlgorithmStatusEnum.QUEUED.name());
            pending.setSimulated(Boolean.TRUE);
            return pending;
        }
        return output;
    }

    @Override
    public AlgorithmTaskOutputDTO toStandardResult(String rawOutput) {
        // 模拟适配器不使用原始输出，直接返回当前任务结果
        // 真实算法接入时由新适配器实现此转换
        throw new UnsupportedOperationException("模拟适配器不支持原始输出转换");
    }

    @Override
    public String adapterName() {
        return "MockAlgorithmGateway";
    }

    /**
     * 获取模拟参数，供管理端或调试使用。
     */
    public MockAlgorithmProperties properties() {
        return properties;
    }
}