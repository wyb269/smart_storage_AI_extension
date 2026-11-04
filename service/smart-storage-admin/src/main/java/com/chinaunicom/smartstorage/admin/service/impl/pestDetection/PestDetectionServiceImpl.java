package com.chinaunicom.smartstorage.admin.service.impl.pestDetection;

import com.chinaunicom.smartstorage.admin.constant.PestDetectionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmWriteBackDTO;
import com.chinaunicom.smartstorage.admin.entity.pestDetection.AiPestAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.PestDetectionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.PestDetectionRepository;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.PestDetectionService;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.algorithm.PestDetectionAlgorithmGateway;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 粮面虫害智能识别主业务实现。
 *
 * <p><b>安全要求</b>：</p>
 * <ol>
 *   <li>处理人从登录态获取，不信任前端传入。</li>
 *   <li>使用事件版本号防止并发覆盖。</li>
 *   <li>算法结论不得被人工处理改写。</li>
 *   <li>所有读写路径统一注入并校验 {@link AiVisionDataScope}，禁止跨组织越权。</li>
 *   <li>写操作必须要求已认证用户，无法解析登录态时直接拒绝，不回退 anonymous。</li>
 * </ol>
 *
 * <p><b>本类刻意不加 {@code @Transactional}</b>：默认事务管理器绑定主库，
 * 而本功能读写独立库；跨事务管理器时内层不会加入外层事务，
 * 加在这里只会白占一个主库连接。事务由仓储实现自己声明并持有。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Slf4j
@Service
public class PestDetectionServiceImpl implements PestDetectionService {

    @Resource
    private PestDetectionRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /**
     * 算法适配层网关。
     *
     * <p>本页首个注入点：此前 {@code PestDetectionAlgorithmGateway} 零调用方，
     * 「重新识别」链路接入后，真实算法到达时只需换实现类，本类逻辑不动。</p>
     */
    @Resource
    private PestDetectionAlgorithmGateway algorithmGateway;

    /** 仅用于算法输出（检测框/证据引用）落库序列化，不做任何协议暴露 */
    private final ObjectMapper algorithmJsonMapper = new ObjectMapper();

    /** 与算法网关输出一致的时间字符串格式 */
    private static final DateTimeFormatter ALGORITHM_TS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 算法任务终态集合：进入终态后轮询不再调用网关 */
    private static final Set<String> TERMINAL_STATUSES = Set.of(
        PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
        PestDetectionStatusEnum.AlgorithmStatusEnum.FAILED.name(),
        PestDetectionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(),
        PestDetectionStatusEnum.AlgorithmStatusEnum.SKIPPED.name());

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[PestDetection] 写操作被拒绝：请求未携带有效登录态");
            throw new CustomException("未登录或登录已失效，请重新登录后再操作");
        }
        return scope;
    }

    private String resolveCurrentUser(AiVisionDataScope scope) {
        if (scope == null || !scope.isAuthenticated()) {
            throw new CustomException("未登录或登录已失效，请重新登录后再操作");
        }
        if (scope.getUserName() != null && !scope.getUserName().isBlank()) {
            return scope.getUserName();
        }
        return scope.getUserId();
    }

    /* ==================== 粮面点位状态 ==================== */

    @Override
    public List<MonitorPointRespDTO> listMonitorPoint() {
        return repository.listMonitorPoint(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId) {
        if (pointId == null || pointId.isBlank()) {
            throw new CustomException("点位标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从点位列表中定位粮面点位，同样受数据范围约束
        MonitorPointRespDTO point = repository.listMonitorPoint(scope).stream()
            .filter(p -> pointId.equals(p.getPointId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setPointId(pointId);

        if (point == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setProtocol(PestDetectionStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("POINT_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setCameraCode(point.getCameraCode());
        dto.setPlatformCameraId(point.getPlatformCameraId());
        dto.setProtocol(PestDetectionStatusEnum.StreamProtocolEnum.MP4.name());

        if (!PestDetectionStatusEnum.DeviceStatusEnum.ONLINE.name().equals(point.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
            return actionResultService.callBackResult(dto);
        }

        // 取流主体是点位，播放主体是其绑定的粮面摄像头
        MediaGateway.PlaybackRequest request = new MediaGateway.PlaybackRequest(
            pointId, point.getPlatformCameraId(), point.getWarehouseId(), null, null, null);

        MediaGateway.PlaybackGrant grant = mediaGateway.signPlayback(request);
        dto.setCanPlay(grant.canPlay());
        dto.setPlayableUrl(grant.playableUrl());
        dto.setExpiresAt(grant.expiresAt());
        if (grant.protocol() != null) {
            dto.setProtocol(grant.protocol());
        }
        dto.setUnavailableReason(grant.unavailableReason());

        // 日志不打印完整播放地址
        log.info("[PestDetection] 签发播放地址 pointId={} protocol={} canPlay={}",
            pointId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 记录查询 ==================== */

    @Override
    public List<PestEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), PestDetectionConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<PestEventRespDTO> listEventByCondition(PestEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<PestEventRespDTO> listEventByConditionForExport(PestExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        PestEventQueryReqDTO query = new PestEventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setPointId(parseId(exportReq.getPointId(), "点位标识"));
        query.setCameraCode(exportReq.getCameraCode());
        query.setSamplePosition(exportReq.getSamplePosition());
        query.setConclusionType(exportReq.getConclusionType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? PestDetectionConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), PestDetectionConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[PestDetection] 导出记录 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<PestEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态直接给三态文案；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(processStatusLabel(row.getProcessStatus())));
        return rows;
    }

    /** 处理状态编码 → 三态中文标签，非法编码原样返回，不让脏数据把导出打挂 */
    private String processStatusLabel(String code) {
        try {
            return PestDetectionStatusEnum.ProcessStatusEnum.valueOf(code).getLabel();
        } catch (IllegalArgumentException | NullPointerException e) {
            return code;
        }
    }

    @Override
    public PestEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("记录标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按记录 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        PestEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("记录不存在或无权查看");
        }

        // 密度是否超标为派生量：density > densityThreshold，阈值来自事件快照而非页面常量
        detail.setDensityExceeded(
            detail.getDensity() != null && detail.getDensityThreshold() != null
                && detail.getDensity().compareTo(detail.getDensityThreshold()) > 0);

        List<ProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        if (detail.getEvidences() != null) {
            for (PestEvidenceRespDTO evidence : detail.getEvidences()) {
                grantEvidenceAddresses(evidence);
            }
        }

        // 算法任务历史随详情一次带回：弹窗任务表不再单独发请求（契约 §3）。
        // 按 started_at 倒序，首条即最新任务。
        List<PestAlgorithmTaskRespDTO> algorithmTasks = repository.listAlgorithmTasks(
            scope, eventId, PestDetectionConstant.MAX_ALGORITHM_TASKS_PER_EVENT);
        detail.setAlgorithmTasks(algorithmTasks);
        detail.setLatestAlgorithmTask(
            algorithmTasks == null || algorithmTasks.isEmpty() ? null : algorithmTasks.get(0));

        return detail;
    }

    /**
     * 为证据签发短时地址。
     *
     * <p>数据库里存的是受控对象存储键，由 MediaGateway 换成短时地址后才返回前端。
     * 原始资源与算法标注资源分别签发，互不影响。</p>
     */
    private void grantEvidenceAddresses(PestEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), PestDetectionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), PestDetectionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), PestDetectionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
    }

    /**
     * 判断是否为受控对象存储引用。
     *
     * <p>只接受受控协议前缀（如 {@code mock://}、{@code s3://}）；
     * 拒绝任意 http(s) URL 与本地路径，防止越权访问。</p>
     */
    private boolean isControlledReference(String ref) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        String lower = ref.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return false;
        }
        if (ref.contains("..") || ref.startsWith("/") || ref.contains("\\")) {
            return false;
        }
        return ref.contains("://");
    }

    /* ==================== 算法任务（「重新识别」） ==================== */

    /**
     * 提交「重新识别」。
     *
     * <p>生命周期与契约 §1 一致：校验事件在数据范围内 → 组装统一算法输入 →
     * {@code gateway.submitTask()} 快速受理 → 落一行 QUEUED 任务 → 立即返回。
     * 全程不在请求线程里等待推理结果。</p>
     *
     * <p>重跑本质是<b>重新取证</b>：不产生人工结论、不改 process_status，
     * 因此权限沿用 VIEW（Controller 处注释同此取舍）；
     * 但它写库，所以按既有口径要求已认证登录态。</p>
     */
    @Override
    public PestAlgorithmRerunRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("记录标识不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();
        PestEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("记录不存在或无权操作");
        }

        String traceId = "PST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        PestAlgorithmTaskInputDTO input = new PestAlgorithmTaskInputDTO();
        input.setTaskType(PestDetectionConstant.ALGORITHM_TASK_TYPE);
        input.setEventId(eventId);
        input.setPointId(event.getPointId());
        input.setWarehouseId(event.getWarehouseId());
        input.setCameraCode(event.getCameraCode());
        input.setSamplePosition(event.getSamplePosition());
        input.setMediaType(PestDetectionConstant.ALGORITHM_MEDIA_TYPE);
        input.setMediaRefs(resolveMediaRefs(event));
        input.setCapturedAt(event.getOccurredAt());
        input.setDensityThreshold(event.getDensityThreshold());
        input.setConfigVersion(PestDetectionConstant.ALGORITHM_CONFIG_VERSION);
        input.setTraceId(traceId);

        final String taskNo = algorithmGateway.submitTask(input);

        AiPestAlgorithmTask task = new AiPestAlgorithmTask();
        task.setTaskNo(taskNo);
        task.setEventId(parseLongOrNull(eventId));
        task.setPointId(parseLongOrNull(event.getPointId()));
        task.setCameraCode(event.getCameraCode());
        task.setWarehouseId(parseLongOrNull(event.getWarehouseId()));
        task.setTaskType(PestDetectionConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(PestDetectionConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(toJsonOrNull(input.getMediaRefs()));
        task.setExecutionStatus(PestDetectionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        task.setConfigVersion(PestDetectionConstant.ALGORITHM_CONFIG_VERSION);
        task.setStartedAt(LocalDateTime.now());
        task.setRetryCount(0);
        task.setTraceId(traceId);
        // 当前唯一实现是模拟适配器：模拟标识必须落 TRUE，禁止写 FALSE 美化验收
        task.setIsSimulated(Boolean.TRUE);
        String operator = resolveCurrentUser(scope);
        task.setCreatedBy(operator);
        task.setUpdatedBy(operator);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        repository.insertAlgorithmTask(task);

        log.info("[PestDetection] 重新识别已受理 eventId={} taskNo={} adapter={}",
            eventId, taskNo, algorithmGateway.adapterName());

        PestAlgorithmRerunRespDTO resp = new PestAlgorithmRerunRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(task.getExecutionStatus());
        resp.setSimulated(Boolean.TRUE);
        return resp;
    }

    /**
     * 轮询查询算法任务。
     *
     * <p>先查库（带数据范围）；未进终态时调网关取最新 → 落库 →
     * 成功则同事务回写事件算法列（不动 process_status）。
     * 网关仍在排队时原样返回库中状态，不产生写入。</p>
     */
    @Override
    public PestAlgorithmTaskRespDTO getAlgorithmTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new CustomException("算法任务标识不能为空");
        }

        // 轮询可能触发落库与事件回写，按写操作口径要求已认证
        AiVisionDataScope scope = requireAuthenticatedScope();

        PestAlgorithmTaskRespDTO stored = repository.findAlgorithmTaskByTaskNo(scope, taskId);
        if (stored == null) {
            throw new CustomException("算法任务不存在或无权查看");
        }
        if (TERMINAL_STATUSES.contains(stored.getExecutionStatus())) {
            return stored;
        }

        PestAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskId);
        if (output == null || output.getExecutionStatus() == null
                || output.getExecutionStatus().equals(stored.getExecutionStatus())) {
            // 网关仍在排队：不写库，前端按既有节奏继续轮询
            return stored;
        }

        mergeGatewayResult(scope, stored, output);

        PestAlgorithmTaskRespDTO refreshed = repository.findAlgorithmTaskByTaskNo(scope, taskId);
        return refreshed == null ? stored : refreshed;
    }

    @Override
    public List<PestAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("记录标识不能为空");
        }
        return repository.listAlgorithmTasks(resolveDataScope(), eventId,
            PestDetectionConstant.MAX_ALGORITHM_TASKS_PER_EVENT);
    }

    /**
     * 把网关输出合并到任务行；任务成功时同事务回写事件算法列。
     *
     * <p>回写的结论列必须与 {@code detections} 自洽：
     * 网关给出 ANOMALY 时 {@code density > densityThreshold} 才落 DENSITY_HIGH
     * （该判定在网关内完成，本层只做映射与持久化）。</p>
     */
    private void mergeGatewayResult(AiVisionDataScope scope, PestAlgorithmTaskRespDTO stored,
                                    PestAlgorithmTaskOutputDTO output) {
        final LocalDateTime now = LocalDateTime.now();

        AiPestAlgorithmTask update = new AiPestAlgorithmTask();
        update.setId(parseLongOrNull(stored.getId()));
        update.setTaskNo(stored.getTaskId());
        update.setExecutionStatus(output.getExecutionStatus());
        update.setConclusion(output.getConclusion());
        update.setConclusionType(output.getConclusionType());
        update.setConfidence(output.getConfidence());
        update.setModelName(output.getModelName());
        update.setModelVersion(output.getModelVersion());
        update.setAlgorithmVersion(output.getAlgorithmVersion());
        update.setConfigVersion(output.getConfigVersion());
        update.setStartedAt(parseAlgorithmTime(output.getStartedAt()));
        update.setFinishedAt(parseAlgorithmTime(output.getFinishedAt()));
        update.setInferenceDurationMs(output.getInferenceDurationMs());
        update.setDetections(toDetectionsJson(output));
        update.setEvidenceRefs(toEvidenceRefsJson(output));
        update.setRawResultRef(output.getRawResultRef());
        update.setErrorCode(output.getErrorCode());
        update.setErrorMessage(output.getErrorMessage());
        // 模拟适配器产出的任何分支（含失败/超时）都是模拟结果；
        // 网关漏标时按 TRUE 兜底，绝不允许漏标成真实数据。
        update.setIsSimulated(output.getSimulated() == null || Boolean.TRUE.equals(output.getSimulated()));
        update.setUpdatedAt(now);

        PestAlgorithmWriteBackDTO writeBack = null;
        if (PestDetectionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
                .equals(output.getExecutionStatus()) && stored.getEventId() != null) {
            writeBack = new PestAlgorithmWriteBackDTO();
            writeBack.setEventId(parseLongOrNull(stored.getEventId()));
            writeBack.setAlgorithmTaskId(update.getId());
            writeBack.setAlgorithmStatus(output.getExecutionStatus());
            writeBack.setAlgorithmResult(output.getConclusion());
            writeBack.setConfidence(output.getConfidence());
            // 只有网关给出结论类型（ANOMALY 分支）才覆盖事件结论列，
            // NORMAL/UNCERTAIN 保持事件原结论，不把已有虫情结论洗成空值
            if (output.getConclusionType() != null) {
                writeBack.setConclusionType(output.getConclusionType());
                writeBack.setConclusionTypeName(conclusionTypeLabel(output.getConclusionType()));
                writeBack.setInsectSpecies(output.getInsectSpecies());
                writeBack.setDensity(output.getDensity());
            }
            writeBack.setScopeUnrestricted(scope.isUnrestricted());
            writeBack.setScopeWarehouseIds(scope.getWarehouseIds());
        }

        repository.saveAlgorithmTaskResult(update, writeBack);
    }

    /** 事件证据的受控引用作为算法输入媒体；为空时给约定占位引用，禁止任意 URL */
    private List<String> resolveMediaRefs(PestEventDetailRespDTO event) {
        List<String> refs = new ArrayList<>();
        if (event.getEvidences() != null) {
            for (PestEvidenceRespDTO evidence : event.getEvidences()) {
                if (isControlledReference(evidence.getOriginalUrl())) {
                    refs.add(evidence.getOriginalUrl());
                }
            }
        }
        if (refs.isEmpty()) {
            refs.add("mock://pest/" + event.getEventId() + "/frame");
        }
        return refs;
    }

    /** 检测框序列化：网关虫种/密度并入 JSON，与事件回写列保持同一份事实 */
    private String toDetectionsJson(PestAlgorithmTaskOutputDTO output) {
        if (output.getDetections() == null || output.getDetections().isEmpty()) {
            return null;
        }
        List<Map<String, Object>> boxes = new ArrayList<>();
        for (PestAlgorithmTaskOutputDTO.DetectionDTO detection : output.getDetections()) {
            Map<String, Object> box = new LinkedHashMap<>();
            box.put("category", detection.getCategory());
            box.put("confidence", detection.getConfidence());
            box.put("bbox", detection.getBbox());
            box.put("trackId", detection.getTrackId());
            if (output.getInsectSpecies() != null && output.getDensity() != null) {
                box.put("species", output.getInsectSpecies());
                // 采样按 1kg 计：虫数 = 密度（头/kg），与结论列自洽
                box.put("count", output.getDensity().intValue());
            }
            boxes.add(box);
        }
        return toJsonOrNull(boxes);
    }

    private String toEvidenceRefsJson(PestAlgorithmTaskOutputDTO output) {
        if (output.getEvidenceRefs() == null || output.getEvidenceRefs().isEmpty()) {
            return null;
        }
        List<Map<String, Object>> refs = new ArrayList<>();
        for (PestAlgorithmTaskOutputDTO.EvidenceRefDTO ref : output.getEvidenceRefs()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("evidenceType", ref.getEvidenceType());
            item.put("objectKey", ref.getObjectKey());
            item.put("capturedAt", ref.getCapturedAt());
            refs.add(item);
        }
        return toJsonOrNull(refs);
    }

    private String toJsonOrNull(Object value) {
        try {
            return algorithmJsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[PestDetection] 算法结果序列化失败，按空落库：{}", e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseAlgorithmTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, ALGORITHM_TS);
        } catch (Exception e) {
            return null;
        }
    }

    /** 结论类型编码 → 中文名；非法编码原样返回，不让脏枚举把回写打挂 */
    private String conclusionTypeLabel(String code) {
        try {
            return PestDetectionStatusEnum.ConclusionTypeEnum.valueOf(code).getLabel();
        } catch (IllegalArgumentException | NullPointerException e) {
            return code;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /* ==================== 人工处理 ==================== */

    @Override
    public ActionResult<String, String> processEvent(String eventId, String processResult,
                                                     String processOpinion, Integer eventVersion) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("记录标识不能为空");
        }
        if (processResult == null || processResult.isBlank()) {
            throw new CustomException("处理结果不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        PestEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("记录不存在或无权处理");
        }

        PestDetectionStatusEnum.ProcessStatusEnum current = parseStatus(existing.getProcessStatus());
        if (!current.isProcessable()) {
            throw new CustomException("该记录已完成核实，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), processResult, processOpinion, processedBy);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[PestDetection] 处理冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("记录已被其他用户处理，请刷新后重试");
        }

        log.info("[PestDetection] 处理成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看记录后自动标记为已处理。
     *
     * <p>三态模型：只把 PENDING 推进到终态 DONE，PROCESSING/DONE 幂等返回，
     * 不引入新枚举值、不改表结构。</p>
     *
     * <p><b>幂等</b>：非 PENDING 时直接返回成功且不产生任何写入，
     * 避免每次查看都追加一条审计。</p>
     */
    @Override
    public ActionResult<String, String> markEventViewed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("记录标识不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        PestEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("记录不存在或无权查看");
        }

        final String currentStatus = existing.getProcessStatus();
        if (PestDetectionStatusEnum.ProcessStatusEnum.DONE.name().equals(currentStatus)) {
            return actionResultService.callBackResult("该记录已是已处理状态");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), currentStatus,
            PestDetectionStatusEnum.ProcessStatusEnum.DONE.name(),
            PestDetectionConstant.AUTO_VIEW_OPINION, processedBy);

        if (affected != 1) {
            log.info("[PestDetection] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("记录状态已变化，请刷新后重试");
        }

        log.info("[PestDetection] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /* ==================== 参数解析 ==================== */

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private PestDetectionStatusEnum.ProcessStatusEnum parseStatus(String value) {
        try {
            return PestDetectionStatusEnum.ProcessStatusEnum.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[PestDetection] 处理状态非法");
            throw new CustomException("记录处理状态异常，请联系管理员");
        }
    }

    /**
     * 把导出条件里的标识字符串转为数值。
     *
     * <p>非数值输入必须给出可理解的提示，而不是让 SQL 报类型错误变成 500。</p>
     *
     * @return 空输入返回 null（表示不参与筛选）
     */
    private Long parseId(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new CustomException(label + "格式不正确");
        }
    }

    /**
     * 解析导出条件中的时间。
     *
     * <p><b>解析失败必须报错，不能返回 null。</b>null 会让时间条件整体消失，
     * 用户以为导出的是筛过的 7 天，实际拿到的是全量历史（最多 5000 条）——
     * 数据错了 yet 毫无提示。</p>
     *
     * @return 空输入返回 null（表示不加时间筛选）
     */
    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (Exception e) {
            throw new CustomException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
