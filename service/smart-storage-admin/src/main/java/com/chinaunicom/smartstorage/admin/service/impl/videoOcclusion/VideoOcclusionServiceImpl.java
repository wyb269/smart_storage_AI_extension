package com.chinaunicom.smartstorage.admin.service.impl.videoOcclusion;

import com.chinaunicom.smartstorage.admin.constant.VideoOcclusionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.RerunAlgorithmRespDTO;
import com.chinaunicom.smartstorage.admin.entity.videoOcclusion.AiOcclusionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.VideoOcclusionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion.VideoOcclusionRepository;
import com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion.VideoOcclusionService;
import com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion.algorithm.OcclusionAlgorithmGateway;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 视频遮挡检测主业务实现。
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
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Slf4j
@Service
public class VideoOcclusionServiceImpl implements VideoOcclusionService {

    @Resource
    private VideoOcclusionRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /**
     * 算法适配层网关（本期唯一实现为 MockOcclusionAlgorithmGateway）。
     *
     * <p>契约 §0 铁律 2：不改网关算法逻辑本体，这里只是<b>第一个注入点</b>；
     * 真实算法到达时换一个实现类，本服务与页面接口都不动。</p>
     */
    @Resource
    private OcclusionAlgorithmGateway algorithmGateway;

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[VideoOcclusion] 写操作被拒绝：请求未携带有效登录态");
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

    /* ==================== 摄像头状态 ==================== */

    @Override
    public List<CameraStatusRespDTO> listCameraStatus() {
        return repository.listCameraStatus(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String cameraId) {
        if (cameraId == null || cameraId.isBlank()) {
            throw new CustomException("摄像头标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从状态列表中定位摄像头，同样受数据范围约束
        CameraStatusRespDTO camera = repository.listCameraStatus(scope).stream()
            .filter(c -> cameraId.equals(c.getCameraId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setCameraId(cameraId);

        if (camera == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setProtocol(VideoOcclusionStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("CAMERA_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setPlatformCameraId(camera.getPlatformCameraId());
        dto.setProtocol(VideoOcclusionStatusEnum.StreamProtocolEnum.MP4.name());

        if (!VideoOcclusionStatusEnum.DeviceStatusEnum.ONLINE.name().equals(camera.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
            return actionResultService.callBackResult(dto);
        }

        MediaGateway.PlaybackRequest request = new MediaGateway.PlaybackRequest(
            cameraId, camera.getPlatformCameraId(), camera.getWarehouseId(), null, null, null);

        MediaGateway.PlaybackGrant grant = mediaGateway.signPlayback(request);
        dto.setCanPlay(grant.canPlay());
        dto.setPlayableUrl(grant.playableUrl());
        dto.setExpiresAt(grant.expiresAt());
        if (grant.protocol() != null) {
            dto.setProtocol(grant.protocol());
        }
        dto.setUnavailableReason(grant.unavailableReason());

        // 日志不打印完整播放地址
        log.info("[VideoOcclusion] 签发播放地址 cameraId={} protocol={} canPlay={}",
            cameraId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<OcclusionEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), VideoOcclusionConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<OcclusionEventRespDTO> listEventByCondition(OcclusionEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<OcclusionEventRespDTO> listEventByConditionForExport(OcclusionExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        OcclusionEventQueryReqDTO query = new OcclusionEventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setCameraConfigId(parseId(exportReq.getCameraConfigId(), "摄像头标识"));
        query.setCameraCode(exportReq.getCameraCode());
        query.setOcclusionType(exportReq.getOcclusionType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? VideoOcclusionConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), VideoOcclusionConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[VideoOcclusion] 导出事件 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<OcclusionEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态按产品口径折叠成两态；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(
            VideoOcclusionStatusEnum.ProcessStatusEnum.PENDING.name().equals(row.getProcessStatus())
                ? "未处理" : "已处理"));
        return rows;
    }

    @Override
    public OcclusionEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按事件 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        OcclusionEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        List<ProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        // 算法任务随详情一次拉齐（契约 §3：任务表不再单独请求）
        List<OcclusionAlgorithmTaskRespDTO> tasks = repository.listAlgorithmTasksByEvent(
            scope, eventId, VideoOcclusionConstant.ALGORITHM_TASKS_MAX_PER_EVENT);
        detail.setAlgorithmTasks(tasks);
        detail.setLatestAlgorithmTask(tasks.isEmpty() ? null : tasks.get(0));

        if (detail.getEvidences() != null) {
            for (OcclusionEvidenceRespDTO evidence : detail.getEvidences()) {
                grantEvidenceAddresses(evidence);
            }
        }

        return detail;
    }

    /**
     * 为证据签发短时地址。
     *
     * <p>数据库里存的是受控对象存储键，由 MediaGateway 换成短时地址后才返回前端。
     * 原始资源与算法标注资源分别签发，互不影响。</p>
     */
    private void grantEvidenceAddresses(OcclusionEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), VideoOcclusionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), VideoOcclusionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), VideoOcclusionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
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

    /* ==================== 人工处理 ==================== */

    @Override
    public ActionResult<String, String> processEvent(String eventId, String processResult,
                                                     String processOpinion, Integer eventVersion) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        if (processResult == null || processResult.isBlank()) {
            throw new CustomException("处理结果不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        OcclusionEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权处理");
        }

        VideoOcclusionStatusEnum.ProcessStatusEnum current = parseStatus(existing.getProcessStatus());
        if (!current.isProcessable()) {
            throw new CustomException("该事件已完成核实，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), processResult, processOpinion, processedBy);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[VideoOcclusion] 事件处理冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("事件已被其他用户处理，请刷新后重试");
        }

        log.info("[VideoOcclusion] 事件处理成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>状态模型只有两态：PENDING = 未处理，其余 = 已处理。
     * 因此只把 PENDING 写成 CLOSED（终态），不引入新枚举值、不改表结构。</p>
     *
     * <p><b>幂等</b>：已是其它状态时直接返回成功且不产生任何写入，
     * 避免每次查看都追加一条审计。</p>
     */
    @Override
    public ActionResult<String, String> markEventViewed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        OcclusionEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        final String pending = VideoOcclusionStatusEnum.ProcessStatusEnum.PENDING.name();
        if (!pending.equals(existing.getProcessStatus())) {
            return actionResultService.callBackResult("该事件已是已处理状态");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), pending,
            VideoOcclusionStatusEnum.ProcessStatusEnum.CLOSED.name(),
            VideoOcclusionConstant.AUTO_VIEW_OPINION, processedBy);

        if (affected != 1) {
            log.info("[VideoOcclusion] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("事件状态已变化，请刷新后重试");
        }

        log.info("[VideoOcclusion] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    /** 网关结果时间字符串格式，与接口输出统一 yyyy-MM-dd HH:mm:ss */
    private static final DateTimeFormatter ALGO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 终态集合：进入终态后不再向网关取进度 */
    private static final List<String> ALGO_TERMINAL_STATUS = List.of(
        VideoOcclusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
        VideoOcclusionStatusEnum.AlgorithmStatusEnum.FAILED.name(),
        VideoOcclusionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(),
        VideoOcclusionStatusEnum.AlgorithmStatusEnum.SKIPPED.name(),
        VideoOcclusionStatusEnum.AlgorithmStatusEnum.MOCK.name());

    @Override
    public RerunAlgorithmRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 重跑是写路径：必须已认证，且事件在本人数据范围内（复用详情范围校验）
        AiVisionDataScope scope = requireAuthenticatedScope();
        OcclusionEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("事件不存在或无权操作");
        }

        String traceId = "oc-" + UUID.randomUUID().toString().replace("-", "");

        OcclusionAlgorithmTaskInputDTO input = new OcclusionAlgorithmTaskInputDTO();
        input.setTaskType(VideoOcclusionConstant.ALGORITHM_TASK_TYPE);
        input.setEventId(event.getEventId());
        input.setWarehouseId(event.getWarehouseId());
        input.setCameraConfigId(event.getCameraConfigId());
        input.setCameraCode(event.getCameraCode());
        input.setMediaType(VideoOcclusionConstant.ALGORITHM_MEDIA_TYPE);
        // 受控内部引用：与证据种子同一键形态，禁止透传签名后的短时 URL（第 24.2、29.3 节）
        input.setMediaRefs(List.of("mock://occlusion/" + eventId + "/occluded"));
        input.setNormalReferenceRef("mock://occlusion/" + eventId + "/normal");
        input.setCapturedAt(event.getOccurredAt());
        input.setConfidenceThreshold(new BigDecimal(VideoOcclusionConstant.ALGORITHM_CONFIDENCE_THRESHOLD));
        input.setConfigVersion(VideoOcclusionConstant.ALGORITHM_CONFIG_VERSION);
        input.setTraceId(traceId);

        final String taskNo = algorithmGateway.submitTask(input);

        AiOcclusionAlgorithmTask task = new AiOcclusionAlgorithmTask();
        task.setTaskNo(taskNo);
        task.setEventId(parseLongOrNull(eventId));
        task.setCameraConfigId(parseLongOrNull(event.getCameraConfigId()));
        task.setCameraCode(event.getCameraCode());
        task.setWarehouseId(parseLongOrNull(event.getWarehouseId()));
        task.setRegionId(scope.getRegionId());
        task.setOrgId(parseLongOrNull(scope.getOrgId()));
        task.setTaskType(VideoOcclusionConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(VideoOcclusionConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(toJsonArrayText(input.getMediaRefs()));
        task.setNormalReferenceRef(input.getNormalReferenceRef());
        task.setExecutionStatus(VideoOcclusionConstant.ALGORITHM_INITIAL_STATUS);
        task.setConfidenceThreshold(new BigDecimal(VideoOcclusionConstant.ALGORITHM_CONFIDENCE_THRESHOLD));
        task.setConfigVersion(VideoOcclusionConstant.ALGORITHM_CONFIG_VERSION);
        task.setStartedAt(LocalDateTime.now());
        task.setTraceId(traceId);
        // 铁律 1：模拟结果必须显式 TRUE，页面据此显示「模拟」标识
        task.setIsSimulated(Boolean.TRUE);
        String operator = resolveCurrentUser(scope);
        task.setCreatedBy(operator);
        task.setCreatedAt(LocalDateTime.now());

        repository.insertAlgorithmTask(task);

        log.info("[VideoOcclusion] 已提交算法重新识别 taskNo={} eventId={} adapter={}",
            taskNo, eventId, algorithmGateway.adapterName());

        RerunAlgorithmRespDTO resp = new RerunAlgorithmRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(VideoOcclusionConstant.ALGORITHM_INITIAL_STATUS);
        resp.setSimulated(Boolean.TRUE);
        return resp;
    }

    @Override
    public OcclusionAlgorithmTaskRespDTO getAlgorithmTask(String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            throw new CustomException("算法任务编号不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();
        OcclusionAlgorithmTaskRespDTO task = repository.findAlgorithmTaskByTaskNo(scope, taskNo);
        if (task == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象存在性
            throw new CustomException("算法任务不存在或无权查看");
        }

        // 未进终态才向网关取最新进度；终态任务直接回库内结果（第 24.1 节）
        if (!ALGO_TERMINAL_STATUS.contains(task.getExecutionStatus())) {
            OcclusionAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskNo);
            if (output != null && ALGO_TERMINAL_STATUS.contains(output.getExecutionStatus())) {
                persistTaskResult(scope, task, output);
                OcclusionAlgorithmTaskRespDTO refreshed =
                    repository.findAlgorithmTaskByTaskNo(scope, taskNo);
                return refreshed == null ? task : refreshed;
            }
        }
        return task;
    }

    @Override
    public List<OcclusionAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        return repository.listAlgorithmTasksByEvent(resolveDataScope(), eventId,
            VideoOcclusionConstant.ALGORITHM_TASKS_MAX_PER_EVENT);
    }

    /**
     * 把网关终态输出落库，并在成功时回写事件算法列。
     *
     * <p>失败/超时只更新任务行，<b>不回写事件</b>：事件的旧算法结论仍是
     * 上一次成功取证的记录，不能被一次失败的尝试清空。</p>
     */
    private void persistTaskResult(AiVisionDataScope scope, OcclusionAlgorithmTaskRespDTO task,
                                   OcclusionAlgorithmTaskOutputDTO output) {
        Long taskId = parseLongOrNull(task.getTaskId());
        if (taskId == null) {
            return;
        }

        AiOcclusionAlgorithmTask entity = new AiOcclusionAlgorithmTask();
        entity.setId(taskId);
        entity.setExecutionStatus(output.getExecutionStatus());
        entity.setConclusion(output.getConclusion());
        entity.setOcclusionType(output.getOcclusionType());
        entity.setConfidence(output.getConfidence());
        entity.setModelName(output.getModelName());
        entity.setModelVersion(output.getModelVersion());
        entity.setAlgorithmVersion(output.getAlgorithmVersion());
        entity.setConfigVersion(output.getConfigVersion());
        entity.setStartedAt(parseAlgoTime(output.getStartedAt()));
        entity.setFinishedAt(parseAlgoTime(output.getFinishedAt()));
        entity.setInferenceDurationMs(output.getInferenceDurationMs());
        entity.setDetections(toDetectionJsonText(output.getDetections()));
        entity.setEvidenceRefs(toEvidenceJsonText(output.getEvidenceRefs()));
        entity.setRawResultRef(output.getRawResultRef());
        entity.setErrorCode(output.getErrorCode());
        entity.setErrorMessage(output.getErrorMessage());
        // 网关自身的模拟标识为准；缺省按铁律 1 落 TRUE
        entity.setIsSimulated(output.getSimulated() == null ? Boolean.TRUE : output.getSimulated());
        entity.setUpdatedBy(resolveCurrentUserSafe(scope));
        entity.setUpdatedAt(LocalDateTime.now());

        int updated = repository.updateAlgorithmTask(entity);
        if (updated == 0) {
            log.warn("[VideoOcclusion] 算法任务回填未命中 taskNo={}", task.getTaskNo());
            return;
        }

        boolean success = VideoOcclusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
            .equals(output.getExecutionStatus());
        if (success && task.getEventId() != null) {
            // 契约 §1：成功回写 algorithm_task_id / algorithm_status /
            // confidence + 本功能结论列（algorithm_result），不动 process_status
            int written = repository.writeBackEventAlgorithmResult(scope, task.getEventId(), taskId,
                output.getExecutionStatus(), output.getConclusion(), output.getConfidence(),
                resolveCurrentUserSafe(scope));
            if (written == 0) {
                log.info("[VideoOcclusion] 算法结果回写未生效（越权或事件已删除）eventId={}",
                    task.getEventId());
            }
        }
    }

    /** 读路径的兜底操作人：任务回填只是算法侧写库，未登录时以算法适配器名义记录 */
    private String resolveCurrentUserSafe(AiVisionDataScope scope) {
        try {
            return resolveCurrentUser(scope);
        } catch (CustomException e) {
            return "algorithm-gateway";
        }
    }

    private LocalDateTime parseAlgoTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, ALGO_TS);
        } catch (Exception e) {
            log.warn("[VideoOcclusion] 算法输出时间格式异常，按空处理");
            return null;
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

    /**
     * 字符串数组 → JSON 数组文本。
     *
     * <p>内容全部是系统生成的受控引用（mock:// 键），
     * 这里仍做引号/反斜杠转义，防止将来键来源变化时写出非法 JSON。</p>
     */
    private String toJsonArrayText(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(escapeJson(items.get(i))).append('"');
        }
        return sb.append(']').toString();
    }

    private String toDetectionJsonText(List<OcclusionAlgorithmTaskOutputDTO.DetectionDTO> detections) {
        if (detections == null || detections.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < detections.size(); i++) {
            OcclusionAlgorithmTaskOutputDTO.DetectionDTO d = detections.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"category\":\"").append(escapeJson(d.getCategory())).append("\",")
                .append("\"confidence\":").append(d.getConfidence() == null ? "null" : d.getConfidence().toPlainString()).append(',')
                .append("\"bbox\":\"").append(escapeJson(d.getBbox())).append("\",")
                .append("\"trackId\":\"").append(escapeJson(d.getTrackId())).append("\"}");
        }
        return sb.append(']').toString();
    }

    private String toEvidenceJsonText(List<OcclusionAlgorithmTaskOutputDTO.EvidenceRefDTO> refs) {
        if (refs == null || refs.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < refs.size(); i++) {
            OcclusionAlgorithmTaskOutputDTO.EvidenceRefDTO r = refs.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"evidenceType\":\"").append(escapeJson(r.getEvidenceType())).append("\",")
                .append("\"objectKey\":\"").append(escapeJson(r.getObjectKey())).append("\",")
                .append("\"capturedAt\":\"").append(escapeJson(r.getCapturedAt())).append("\"}");
        }
        return sb.append(']').toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /* ==================== 参数解析 ==================== */

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private VideoOcclusionStatusEnum.ProcessStatusEnum parseStatus(String value) {
        try {
            return VideoOcclusionStatusEnum.ProcessStatusEnum.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[VideoOcclusion] 事件处理状态非法");
            throw new CustomException("事件处理状态异常，请联系管理员");
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
