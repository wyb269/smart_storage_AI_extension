package com.chinaunicom.smartstorage.admin.service.impl.groundWater;

import com.chinaunicom.smartstorage.admin.constant.GroundWaterConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.GroundWaterAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.GroundWaterAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.RerunAlgorithmRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.WaterAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.groundWater.AiWaterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.GroundWaterStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.GroundWaterRepository;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.GroundWaterService;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.algorithm.GroundWaterAlgorithmGateway;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 地面积水监测主业务实现。
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
 * <p><b>算法链路（契约 §1）</b>：本类是 {@link GroundWaterAlgorithmGateway} 的
 * 唯一注入点——「重新识别」提交后立即返回任务编号，进度由查询端点驱动落库；
 * 页面请求里绝不做同步长等。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Slf4j
@Service
public class GroundWaterServiceImpl implements GroundWaterService {

    /** 任务时间字段输出格式，与列表/详情接口同形 */
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 算法终态集合。与 ai_water_algorithm_task.execution_status 注释一致；
     * 处于集合外的任务（QUEUED/RUNNING）在查询时会向网关取最新产出。
     */
    private static final Set<String> ALGORITHM_TERMINAL_STATUSES = Set.of(
        GroundWaterStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
        GroundWaterStatusEnum.AlgorithmStatusEnum.FAILED.name(),
        GroundWaterStatusEnum.AlgorithmStatusEnum.TIMEOUT.name(),
        GroundWaterStatusEnum.AlgorithmStatusEnum.SKIPPED.name(),
        GroundWaterStatusEnum.AlgorithmStatusEnum.MOCK.name());

    /** 任务 detections / mediaRefs 的 JSON 读写用（只做序列化，不建对象模型） */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** 重跑任务的置信度阈值，与模拟适配器默认值同口径 */
    private static final BigDecimal RERUN_CONFIDENCE_THRESHOLD = new BigDecimal("0.7500");

    @Resource
    private GroundWaterRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /**
     * 算法适配层网关。当前唯一实现是 Mock 适配器；
     * 真实算法到达后换实现类即可，本类与页面接口都不动（第 24.5 节）。
     */
    @Resource
    private GroundWaterAlgorithmGateway algorithmGateway;

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[GroundWater] 写操作被拒绝：请求未携带有效登录态");
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

    /* ==================== 监测点位状态 ==================== */

    @Override
    public List<MonitorPointStatusRespDTO> listMonitorPoint() {
        return repository.listMonitorPoint(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    /**
     * 取点位绑定摄像头的短时播放地址。
     *
     * <p>取流主体是<b>点位</b>而不是摄像头：前端只知道自己点选了哪张卡片，
     * 点位到摄像头的绑定关系在这里解析，避免把配置结构泄露给页面。</p>
     */
    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId) {
        if (pointId == null || pointId.isBlank()) {
            throw new CustomException("监测点位标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从点位状态列表中定位点位，同样受数据范围约束
        MonitorPointStatusRespDTO point = repository.listMonitorPoint(scope).stream()
            .filter(p -> pointId.equals(p.getPointId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setPointId(pointId);
        dto.setProtocol(GroundWaterStatusEnum.StreamProtocolEnum.MP4.name());

        if (point == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setUnavailableReason("POINT_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setPointName(point.getPointName());
        dto.setCameraCode(point.getCameraCode());
        // 点位表里的 camera_id 就是视频平台侧标识，DTO 统一叫 platformCameraId，
        // 与遮挡那版的响应字段名保持一致
        dto.setPlatformCameraId(point.getPlatformCameraId());

        if (!GroundWaterStatusEnum.DeviceStatusEnum.ONLINE.name().equals(point.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
            return actionResultService.callBackResult(dto);
        }

        // 点位未绑定摄像头：给出可理解的原因，而不是拿 null 去签发后 NPE
        if (point.getPlatformCameraId() == null || point.getPlatformCameraId().isBlank()) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("POINT_NO_CAMERA");
            return actionResultService.callBackResult(dto);
        }

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
        log.info("[GroundWater] 签发播放地址 pointId={} protocol={} canPlay={}",
            pointId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<WaterEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), GroundWaterConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<WaterEventRespDTO> listEventByCondition(WaterEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<WaterEventRespDTO> listEventByConditionForExport(WaterExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        WaterEventQueryReqDTO query = new WaterEventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setPointId(parseId(exportReq.getPointId(), "监测点位标识"));
        query.setCameraCode(exportReq.getCameraCode());
        query.setWaterType(exportReq.getWaterType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? GroundWaterConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), GroundWaterConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[GroundWater] 导出事件 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<WaterEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态直接给可读文案；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(processStatusLabel(row.getProcessStatus())));
        return rows;
    }

    /** 导出面的处理状态中文文案；未知编码原样返回，不让导出因为脏数据整体失败 */
    private String processStatusLabel(String code) {
        GroundWaterStatusEnum.ProcessStatusEnum status = tryParseStatus(code);
        return status == null ? String.valueOf(code) : statusLabel(status);
    }

    private String statusLabel(GroundWaterStatusEnum.ProcessStatusEnum status) {
        return switch (status) {
            case PENDING -> "未核实";
            case PROCESSING -> "处理中";
            case DONE -> "已处理";
        };
    }

    @Override
    public WaterEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按事件 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        WaterEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        List<ProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        // 算法任务随详情一并返回（契约 §1：最多 20 条，started_at 倒序），
        // 详情弹窗不再为任务表单独发请求；「重新识别」后的刷新也复用本接口。
        List<AiWaterAlgorithmTask> taskEntities = repository.listAlgorithmTasksByEventId(
            scope, eventId, GroundWaterConstant.DETAIL_ALGORITHM_TASK_LIMIT);
        List<WaterAlgorithmTaskRespDTO> taskDtos = new ArrayList<>();
        for (AiWaterAlgorithmTask t : taskEntities) {
            taskDtos.add(toTaskRespDTO(t));
        }
        detail.setAlgorithmTasks(taskDtos);
        detail.setLatestAlgorithmTask(taskDtos.isEmpty() ? null : taskDtos.get(0));

        if (detail.getEvidences() != null) {
            for (WaterEvidenceRespDTO evidence : detail.getEvidences()) {
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
    private void grantEvidenceAddresses(WaterEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), GroundWaterConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), GroundWaterConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), GroundWaterConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
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

        WaterEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权处理");
        }

        GroundWaterStatusEnum.ProcessStatusEnum current = parseStatus(existing.getProcessStatus());
        if (!current.isProcessable()) {
            throw new CustomException("该事件已处理完成，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), processResult, processOpinion, processedBy,
            GroundWaterConstant.PROCESS_SOURCE_WEB);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[GroundWater] 事件处理冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("事件已被其他用户处理，请刷新后重试");
        }

        log.info("[GroundWater] 事件处理成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p><b>三态模型下的取舍</b>：只把 PENDING（未核实）推进到 DONE（已处理）。
     * PROCESSING（处理中）说明已经有人在现场跟进，被一次「点开看看」冲掉
     * 会丢失真实的处置进度，因此不在自动标记范围内。</p>
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

        WaterEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        final String currentStatus = existing.getProcessStatus();
        if (GroundWaterStatusEnum.ProcessStatusEnum.DONE.name().equals(currentStatus)) {
            return actionResultService.callBackResult("该事件无需自动标记");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), currentStatus,
            GroundWaterStatusEnum.ProcessStatusEnum.DONE.name(),
            GroundWaterConstant.AUTO_VIEW_OPINION, processedBy,
            GroundWaterConstant.PROCESS_SOURCE_SYSTEM);

        if (affected != 1) {
            log.info("[GroundWater] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("事件状态已变化，请刷新后重试");
        }

        log.info("[GroundWater] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /* ==================== 算法任务（「重新识别」链路，契约 §1） ==================== */

    /**
     * 对事件重新提交算法识别：受理即返回，绝不在本请求里等结果。
     *
     * <p>权限口径与查询端点一致取 VIEW：重跑本质是<b>重新取证</b>，
     * 只新增算法任务行、只回写算法侧字段，不产生任何人工结论，
     * 因此不需要（也尚未开放）PROCESS。取舍写在 Controller 注释中同步声明。</p>
     */
    @Override
    public RerunAlgorithmRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 提交是写路径：必须已认证，且事件在登录态数据范围内
        AiVisionDataScope scope = requireAuthenticatedScope();
        WaterEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象存在性
            throw new CustomException("事件不存在或无权操作");
        }

        // 取点位绑定的平台摄像头标识；点位不在范围内时缺省 null，不影响受理
        String platformCameraId = repository.listMonitorPoint(scope).stream()
            .filter(p -> Objects.equals(p.getPointId(), event.getPointId()))
            .map(MonitorPointStatusRespDTO::getPlatformCameraId)
            .findFirst()
            .orElse(null);

        GroundWaterAlgorithmTaskInputDTO input = new GroundWaterAlgorithmTaskInputDTO();
        input.setTaskType(GroundWaterConstant.ALGORITHM_TASK_TYPE);
        input.setEventId(eventId);
        input.setWarehouseId(event.getWarehouseId());
        input.setPointId(event.getPointId());
        input.setCameraId(platformCameraId);
        input.setCameraCode(event.getCameraCode());
        input.setMediaType(GroundWaterConstant.ALGORITHM_MEDIA_TYPE);
        List<String> mediaRefs = collectControlledMediaRefs(event, eventId);
        input.setMediaRefs(mediaRefs);
        input.setCapturedAt(event.getOccurredAt());
        input.setNormalReferenceRef(findNormalReference(event));
        input.setConfidenceThreshold(RERUN_CONFIDENCE_THRESHOLD);
        input.setConfigVersion(GroundWaterConstant.ALGORITHM_CONFIG_VERSION);
        String traceId = "gw-" + UUID.randomUUID().toString().replace("-", "");
        input.setTraceId(traceId);

        // 网关只负责受理：Mock 适配器起后台线程出结果，本请求立即拿到 taskId
        String taskId = algorithmGateway.submitTask(input);

        final LocalDateTime now = LocalDateTime.now();
        AiWaterAlgorithmTask task = new AiWaterAlgorithmTask();
        task.setTaskNo(taskId);
        task.setEventId(parseLongQuietly(eventId));
        task.setPointId(parseLongQuietly(event.getPointId()));
        task.setCameraId(platformCameraId);
        task.setCameraCode(event.getCameraCode());
        task.setWarehouseId(parseLongQuietly(event.getWarehouseId()));
        task.setTaskType(GroundWaterConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(GroundWaterConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(toJsonQuietly(mediaRefs));
        task.setNormalReferenceRef(input.getNormalReferenceRef());
        task.setExecutionStatus(GroundWaterStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        task.setConfigVersion(GroundWaterConstant.ALGORITHM_CONFIG_VERSION);
        task.setConfidenceThreshold(RERUN_CONFIDENCE_THRESHOLD);
        task.setMediaDigest("gw-" + Integer.toHexString(
            Objects.hash(mediaRefs, event.getOccurredAt(), event.getPointId())));
        task.setStartedAt(now);
        task.setRetryCount(0);
        task.setTraceId(traceId);
        // 铁律 1：当前唯一实现是 Mock 适配器，落库必须 is_simulated = TRUE
        task.setIsSimulated(Boolean.TRUE);
        task.setCreatedBy(resolveCurrentUser(scope));
        task.setCreatedAt(now);
        repository.insertAlgorithmTask(task);

        log.info("[GroundWater] 算法重跑已受理 eventId={} taskId={} adapter={}",
            eventId, taskId, algorithmGateway.adapterName());
        return new RerunAlgorithmRespDTO(taskId,
            GroundWaterStatusEnum.AlgorithmStatusEnum.QUEUED.name(), Boolean.TRUE);
    }

    /**
     * 查询算法任务：先查库，未进终态才向网关取最新产出并落库。
     *
     * <p><b>可见性节流</b>（{@link GroundWaterConstant#ALGORITHM_RUNNING_VISIBLE_MIN_MS}）：
     * Mock 适配器 200ms 就能出终态，若查询一命中终态立刻落库，
     * 前端 800ms 轮询只会看到 QUEUED→SUCCEEDED 一步到位，
     * 任务生命周期不可观察。规则：QUEUED 的任务首次拿到终态先落 RUNNING，
     * 距任务开始超过阈值后的下一次轮询才采纳终态。
     * 每个请求仍然即时返回，不是同步长等。</p>
     */
    @Override
    public WaterAlgorithmTaskRespDTO getAlgorithmTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new CustomException("算法任务标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();
        AiWaterAlgorithmTask task = repository.getAlgorithmTaskByTaskNo(taskId, scope);
        if (task == null) {
            throw new CustomException("算法任务不存在或无权查看");
        }

        if (!isTerminalStatus(task.getExecutionStatus())) {
            GroundWaterAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskId);
            String gatewayStatus = output == null ? null : output.getExecutionStatus();
            if (isTerminalStatus(gatewayStatus)) {
                task = adoptTerminalOutcome(task, output, scope);
            } else if (gatewayStatus != null
                    && !gatewayStatus.equals(task.getExecutionStatus())
                    && !isTerminalStatus(task.getExecutionStatus())) {
                // QUEUED -> RUNNING 的正常推进（网关已在跑）
                task.setExecutionStatus(gatewayStatus);
                repository.saveAlgorithmTaskOutcome(task, null, false, scope, auditor(scope));
            }
        }
        return toTaskRespDTO(task);
    }

    @Override
    public List<WaterAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        // 查询自带数据范围；越权与「事件没有任务」同为空数组，不泄露存在性
        List<AiWaterAlgorithmTask> entities = repository.listAlgorithmTasksByEventId(
            scope, eventId, GroundWaterConstant.LIST_ALGORITHM_TASK_LIMIT);
        List<WaterAlgorithmTaskRespDTO> dtos = new ArrayList<>();
        for (AiWaterAlgorithmTask t : entities) {
            dtos.add(toTaskRespDTO(t));
        }
        return dtos;
    }

    /**
     * 采纳网关的终态产出：落任务行；SUCCEEDED 时同事务回写事件算法侧字段。
     *
     * <p>回写范围严格限定 algorithm_task_id / algorithm_status / algorithm_result /
     * confidence / water_type——<b>process_status 与 event_version 一律不碰</b>：
     * 算法重跑不产生人工结论，也不该搅动人工处理的乐观锁。</p>
     */
    private AiWaterAlgorithmTask adoptTerminalOutcome(AiWaterAlgorithmTask task,
                                                      GroundWaterAlgorithmTaskOutputDTO output,
                                                      AiVisionDataScope scope) {
        long elapsedMs = task.getStartedAt() == null ? Long.MAX_VALUE
            : Duration.between(task.getStartedAt(), LocalDateTime.now()).toMillis();

        // 终态不得从 QUEUED 一步跳入；进行中状态至少要被观察到一拍
        boolean holdRunning = GroundWaterStatusEnum.AlgorithmStatusEnum.QUEUED.name()
                .equals(task.getExecutionStatus())
            || elapsedMs < GroundWaterConstant.ALGORITHM_RUNNING_VISIBLE_MIN_MS;
        if (holdRunning) {
            task.setExecutionStatus(GroundWaterStatusEnum.AlgorithmStatusEnum.RUNNING.name());
            repository.saveAlgorithmTaskOutcome(task, null, false, scope, auditor(scope));
            return task;
        }

        fillOutcome(task, output);
        boolean writeBack = GroundWaterStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
            .equals(output.getExecutionStatus());
        String eventId = task.getEventId() == null ? null : String.valueOf(task.getEventId());
        repository.saveAlgorithmTaskOutcome(task, eventId, writeBack, scope, auditor(scope));
        return task;
    }

    /** 把统一算法输出映射到任务实体待落库的产出字段（startedAt 保留受理时刻） */
    private void fillOutcome(AiWaterAlgorithmTask task, GroundWaterAlgorithmTaskOutputDTO output) {
        task.setExecutionStatus(output.getExecutionStatus());
        task.setConclusion(output.getConclusion());
        task.setWaterType(output.getWaterType());
        task.setConfidence(output.getConfidence());
        task.setModelName(output.getModelName());
        task.setModelVersion(output.getModelVersion());
        task.setAlgorithmVersion(output.getAlgorithmVersion());
        if (output.getConfigVersion() != null && !output.getConfigVersion().isBlank()) {
            task.setConfigVersion(output.getConfigVersion());
        }
        task.setFinishedAt(parseTimeQuietly(output.getFinishedAt()));
        task.setInferenceDurationMs(output.getInferenceDurationMs());
        task.setDetections(toJsonQuietly(output.getDetections()));
        task.setEvidenceRefs(toJsonQuietly(output.getEvidenceRefs()));
        task.setRawResultRef(output.getRawResultRef());
        task.setErrorCode(output.getErrorCode());
        task.setErrorMessage(output.getErrorMessage());
        // 铁律 1：模拟适配器连失败/超时都标 simulated=true；
        // 网关漏标时按 TRUE 兜底，绝不允许未标注的结果入库混入真实统计
        task.setIsSimulated(output.getSimulated() == null || Boolean.TRUE.equals(output.getSimulated()));
    }

    /** 详情接口与查询端点共用的任务出参映射 */
    private WaterAlgorithmTaskRespDTO toTaskRespDTO(AiWaterAlgorithmTask t) {
        WaterAlgorithmTaskRespDTO dto = new WaterAlgorithmTaskRespDTO();
        dto.setTaskId(t.getId() == null ? null : String.valueOf(t.getId()));
        dto.setTaskNo(t.getTaskNo());
        dto.setEventId(t.getEventId() == null ? null : String.valueOf(t.getEventId()));
        dto.setExecutionStatus(t.getExecutionStatus());
        dto.setConclusion(t.getConclusion());
        dto.setWaterType(t.getWaterType());
        dto.setConfidence(t.getConfidence());
        dto.setModelName(t.getModelName());
        dto.setModelVersion(t.getModelVersion());
        dto.setAlgorithmVersion(t.getAlgorithmVersion());
        dto.setConfigVersion(t.getConfigVersion());
        dto.setInferenceDurationMs(t.getInferenceDurationMs());
        dto.setStartedAt(t.getStartedAt() == null ? null : TS.format(t.getStartedAt()));
        dto.setFinishedAt(t.getFinishedAt() == null ? null : TS.format(t.getFinishedAt()));
        dto.setDetectedCount(countDetections(t.getDetections()));
        dto.setSimulated(t.getIsSimulated());
        dto.setErrorCode(t.getErrorCode());
        dto.setErrorMessage(t.getErrorMessage());
        return dto;
    }

    /** detections JSON 的框数；解析失败返回 null 由页面显示「—」，不让脏数据打爆接口 */
    private Integer countDetections(String detections) {
        if (detections == null || detections.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(detections);
            return node.isArray() ? node.size() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 收集事件的受控媒体引用作为算法输入。
     *
     * <p>仓储层返回的证据此刻仍是受控引用（短时地址由本类另一条路径签发），
     * 但仍过一遍 {@link #isControlledReference} —— 算法输入禁止任意 URL，
     * 不能依赖上游永远正确。</p>
     */
    private List<String> collectControlledMediaRefs(WaterEventDetailRespDTO event, String eventId) {
        List<String> refs = new ArrayList<>();
        if (event.getEvidences() != null) {
            for (WaterEvidenceRespDTO ev : event.getEvidences()) {
                if (isControlledReference(ev.getOriginalUrl())) {
                    refs.add(ev.getOriginalUrl());
                }
            }
        }
        if (refs.isEmpty()) {
            refs.add("mock://ground-water/" + eventId + "/frame");
        }
        return refs;
    }

    /** 干燥参考图引用：没有就不传，适配器不强制要求 */
    private String findNormalReference(WaterEventDetailRespDTO event) {
        if (event.getEvidences() == null) {
            return null;
        }
        return event.getEvidences().stream()
            .filter(ev -> GroundWaterStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name()
                .equals(ev.getEvidenceType()))
            .map(WaterEvidenceRespDTO::getOriginalUrl)
            .filter(this::isControlledReference)
            .findFirst()
            .orElse(null);
    }

    private boolean isTerminalStatus(String status) {
        return status != null && ALGORITHM_TERMINAL_STATUSES.contains(status);
    }

    /** 写路径署名：已认证用登录用户名，未认证用 SYSTEM（仅在查询推进状态时出现） */
    private String auditor(AiVisionDataScope scope) {
        return scope.isAuthenticated() ? resolveCurrentUser(scope) : "SYSTEM";
    }

    private String toJsonQuietly(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[GroundWater] 算法任务 JSON 字段序列化失败，按 null 落库");
            return null;
        }
    }

    /* ==================== 参数解析 ==================== */

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private GroundWaterStatusEnum.ProcessStatusEnum parseStatus(String value) {
        GroundWaterStatusEnum.ProcessStatusEnum status = tryParseStatus(value);
        if (status == null) {
            log.warn("[GroundWater] 事件处理状态非法");
            throw new CustomException("事件处理状态异常，请联系管理员");
        }
        return status;
    }

    /** 解析失败返回 null，由调用方决定是报错还是原样透出（导出路径不能因脏数据整体失败） */
    private GroundWaterStatusEnum.ProcessStatusEnum tryParseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return GroundWaterStatusEnum.ProcessStatusEnum.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
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

    /** 网关回传时间的宽松解析：脏格式按 null 落库，不让观测链路因格式问题中断 */
    private LocalDateTime parseTimeQuietly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, TS);
        } catch (Exception e) {
            return null;
        }
    }

    /** 标识字符串转数值；非法返回 null（事件/点位标识由后端生成，出现脏值只跳过回填不报错） */
    private Long parseLongQuietly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
