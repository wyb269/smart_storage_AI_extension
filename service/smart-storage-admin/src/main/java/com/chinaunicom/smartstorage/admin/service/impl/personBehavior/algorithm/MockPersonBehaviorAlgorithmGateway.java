package com.chinaunicom.smartstorage.admin.service.impl.personBehavior.algorithm;

import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.enumbean.PersonBehaviorStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.personBehavior.algorithm.PersonBehaviorAlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.personBehavior.algorithm.PersonBehaviorMockAlgorithmProperties;
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
 * 人员行为模拟算法适配器。
 *
 * <p>真实算法包未到之前的唯一实现，必须支持：正常结果、异常结果、无法判断、
 * 执行失败、执行超时、可配置延迟、正常参考图与行为证据图。</p>
 *
 * <p><b>强制约束</b>：</p>
 * <ol>
 *   <li>本适配器产出的<b>每一个</b>输出对象都必须标记 {@code simulated=TRUE} ——
 *       含失败、超时与「尚未产出」三条非成功路径。漏标的后果是模拟结论
 *       混进真实行为统计，且事后无法从数据里剔除。</li>
 *   <li>不安装、不引用任何真实算法依赖（Python、CUDA、OpenCV、深度学习框架等）。</li>
 *   <li>不猜测真实算法的输入输出格式；真实算法到达后<b>只新增适配器实现类</b>，
 *       把厂商格式经 {@code toStandardResult} 转成统一输出，
 *       不得破坏页面接口与事件模型。</li>
 * </ol>
 *
 * <p><b>本期形态</b>：预留接缝，无任何注入点，不被 Service/Controller 调用。
 * 与仓门异动、视频遮挡两侧的交付现状一致。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Slf4j
@Component
public class MockPersonBehaviorAlgorithmGateway implements PersonBehaviorAlgorithmGateway {

    /** 模拟算法参数（代码常量，不来自配置文件） */
    private final PersonBehaviorMockAlgorithmProperties properties = new PersonBehaviorMockAlgorithmProperties();

    /**
     * 任务结果缓存。
     *
     * <p>仅为本期模拟使用。真实算法接入后由算法服务与数据库承担，
     * 此缓存应随之移除。</p>
     */
    private final Map<String, PersonAlgorithmTaskOutputDTO> taskStore = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String submitTask(PersonAlgorithmTaskInputDTO input) {
        if (input == null || input.getPointId() == null || input.getPointId().isBlank()) {
            throw new IllegalArgumentException("监测区域标识不能为空");
        }

        final String taskId = "MOCK-TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 异步执行，模拟真实算法的任务生命周期：不得由页面请求同步长时间等待
        Thread worker = new Thread(() -> execute(taskId, input), "mock-person-behavior-algorithm-" + taskId);
        worker.setDaemon(true);
        worker.start();

        log.info("[MockPersonBehaviorAlgorithm] 已受理模拟算法任务 taskId={} pointId={}",
                taskId, input.getPointId());
        return taskId;
    }

    /**
     * 执行模拟推理。
     *
     * <p>整体在独立线程内运行，异常与超时均被隔离，
     * 不会影响管理后台接口可用性。</p>
     */
    private void execute(String taskId, PersonAlgorithmTaskInputDTO input) {
        final LocalDateTime startedAt = LocalDateTime.now();
        try {
            Thread.sleep(properties.getBaseDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        PersonAlgorithmTaskOutputDTO output = new PersonAlgorithmTaskOutputDTO();
        output.setTaskId(taskId);
        output.setModelName(properties.getModelName());
        output.setModelVersion(properties.getModelVersion());
        output.setAlgorithmVersion(properties.getAlgorithmVersion());
        output.setConfigVersion(input.getConfigVersion());
        output.setStartedAt(startedAt.format(TS));
        // 所有分支（含失败/超时）共用这一个输出对象，因此标记必须落在分支之前，
        // 而不是每个成功分支各写一次 —— 遮挡那一版就漏过失败分支。
        output.setSimulated(Boolean.TRUE);

        // 强制分支优先，便于测试特定场景
        if (properties.isForceTimeout()) {
            finish(output, startedAt, PersonBehaviorStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(), null, null);
            output.setErrorCode("MOCK_TIMEOUT");
            output.setErrorMessage("模拟算法执行超时");
            taskStore.put(taskId, output);
            return;
        }

        if (properties.isForceFailed()) {
            finish(output, startedAt, PersonBehaviorStatusEnum.AlgorithmStatusEnum.FAILED.name(), null, null);
            output.setErrorCode("MOCK_FAILURE");
            output.setErrorMessage("模拟算法执行失败");
            taskStore.put(taskId, output);
            return;
        }

        // 按区域标识决定结果，保证同一区域结果稳定可复现
        final int seed = Math.abs(input.getPointId().hashCode() % 3);
        String conclusion;
        if (properties.isForceAnomaly()) {
            conclusion = PersonBehaviorStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (properties.isForceUncertain()) {
            conclusion = PersonBehaviorStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        } else if (seed == 0) {
            conclusion = PersonBehaviorStatusEnum.AlgorithmResultEnum.ANOMALY.name();
        } else if (seed == 1) {
            conclusion = PersonBehaviorStatusEnum.AlgorithmResultEnum.NORMAL.name();
        } else {
            conclusion = PersonBehaviorStatusEnum.AlgorithmResultEnum.UNCERTAIN.name();
        }

        finish(output, startedAt, PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(), conclusion, seed);
        output.setBehaviorType(
            PersonBehaviorStatusEnum.AlgorithmResultEnum.ANOMALY.name().equals(conclusion)
                ? PersonBehaviorStatusEnum.BehaviorTypeEnum.CROWD.name()
                : null);

        // 模拟适配器必须同时产出正常参考图与行为异常帧
        output.setEvidenceRefs(buildEvidenceRefs(input));

        // 无论是否异常都产出检测结果，便于页面展示
        List<PersonAlgorithmTaskOutputDTO.DetectionDTO> detections = new ArrayList<>();
        PersonAlgorithmTaskOutputDTO.DetectionDTO detection = new PersonAlgorithmTaskOutputDTO.DetectionDTO();
        detection.setCategory("person");
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
     * <p>耗时含置信度缺失场景（约三分之一），
     * 用于验证页面「暂无」而非 0% 的展示。</p>
     */
    private void finish(PersonAlgorithmTaskOutputDTO output, LocalDateTime startedAt,
                        String status, String conclusion, Integer seed) {
        final LocalDateTime finishedAt = LocalDateTime.now();
        output.setExecutionStatus(status);
        output.setConclusion(conclusion);
        output.setFinishedAt(finishedAt.format(TS));
        output.setInferenceDurationMs(Duration.between(startedAt, finishedAt).toMillis());

        if (PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
                && seed != null && seed != 2) {
            output.setConfidence(BigDecimal.valueOf(0.55d + (seed * 0.15d)).setScale(4, RoundingMode.HALF_UP));
        }
    }

    /**
     * 构造模拟证据引用。
     *
     * <p>使用受控对象存储键，<b>不返回任何完整 URL</b>。</p>
     */
    private List<PersonAlgorithmTaskOutputDTO.EvidenceRefDTO> buildEvidenceRefs(
            PersonAlgorithmTaskInputDTO input) {
        List<PersonAlgorithmTaskOutputDTO.EvidenceRefDTO> refs = new ArrayList<>();
        final String base = "mock://evidence/" + input.getPointId() + "/" + input.getCapturedAt();
        final String captured = input.getCapturedAt();

        PersonAlgorithmTaskOutputDTO.EvidenceRefDTO normal = new PersonAlgorithmTaskOutputDTO.EvidenceRefDTO();
        normal.setEvidenceType(PersonBehaviorStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name());
        normal.setObjectKey(base + "/normal");
        normal.setCapturedAt(captured);
        refs.add(normal);

        PersonAlgorithmTaskOutputDTO.EvidenceRefDTO behavior =
                new PersonAlgorithmTaskOutputDTO.EvidenceRefDTO();
        behavior.setEvidenceType(PersonBehaviorStatusEnum.EvidenceTypeEnum.BEHAVIOR_FRAME.name());
        behavior.setObjectKey(base + "/behavior");
        behavior.setCapturedAt(captured);
        refs.add(behavior);

        return refs;
    }

    @Override
    public PersonAlgorithmTaskOutputDTO queryTask(String taskId) {
        PersonAlgorithmTaskOutputDTO output = taskStore.get(taskId);
        if (output == null) {
            // 尚未产出结果，返回等待中。这条路径同样要标模拟：
            // 它是本适配器构造的对象，不是真实算法的排队状态。
            PersonAlgorithmTaskOutputDTO pending = new PersonAlgorithmTaskOutputDTO();
            pending.setTaskId(taskId);
            pending.setExecutionStatus(PersonBehaviorStatusEnum.AlgorithmStatusEnum.QUEUED.name());
            pending.setSimulated(Boolean.TRUE);
            return pending;
        }
        return output;
    }

    @Override
    public PersonAlgorithmTaskOutputDTO toStandardResult(String rawOutput) {
        // 模拟适配器不使用原始输出，直接返回当前任务结果
        // 真实算法接入时由新适配器实现此转换
        throw new UnsupportedOperationException("模拟适配器不支持原始输出转换");
    }

    @Override
    public String adapterName() {
        return "MockPersonBehaviorAlgorithmGateway";
    }

    /**
     * 获取模拟参数，供管理端或调试使用。
     */
    public PersonBehaviorMockAlgorithmProperties properties() {
        return properties;
    }
}
