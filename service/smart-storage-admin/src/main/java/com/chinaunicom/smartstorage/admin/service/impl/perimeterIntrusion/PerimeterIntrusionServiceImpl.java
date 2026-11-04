package com.chinaunicom.smartstorage.admin.service.impl.perimeterIntrusion;

import com.chinaunicom.smartstorage.admin.constant.PerimeterIntrusionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm.PerimeterAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm.PerimeterAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion.AiPerimeterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.PerimeterIntrusionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.PerimeterIntrusionRepository;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.PerimeterIntrusionService;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.algorithm.PerimeterIntrusionAlgorithmGateway;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 周界入侵识别主业务实现。
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
 * @author aiVision 功能族 · 周界入侵识别
 */
@Slf4j
@Service
public class PerimeterIntrusionServiceImpl implements PerimeterIntrusionService {

    @Resource
    private PerimeterIntrusionRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /** 算法适配层网关（本期唯一实现为 MockPerimeterIntrusionAlgorithmGateway）。 */
    @Resource
    private PerimeterIntrusionAlgorithmGateway algorithmGateway;

    private static final ObjectMapper ALGORITHM_JSON = new ObjectMapper();
    private static final DateTimeFormatter ALGO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 终态：进入后不再向网关轮询，直接回库内结果（第 24.1 节）。 */
    private static final List<String> ALGO_TERMINAL_STATUS = List.of(
        PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
        PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.FAILED.name(),
        PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name());

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[PerimeterIntrusion] 写操作被拒绝：请求未携带有效登录态");
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

    /* ==================== 防区段状态 ==================== */

    @Override
    public List<MonitorPointStatusRespDTO> listMonitorPoint() {
        return repository.listMonitorPoint(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId) {
        if (pointId == null || pointId.isBlank()) {
            throw new CustomException("防区段标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从状态列表中定位防区段，同样受数据范围约束
        MonitorPointStatusRespDTO point = repository.listMonitorPoint(scope).stream()
            .filter(p -> pointId.equals(p.getPointId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setPointId(pointId);

        if (point == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setProtocol(PerimeterIntrusionStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("POINT_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setCameraConfigId(point.getCameraConfigId());
        dto.setPlatformCameraId(point.getPlatformCameraId());
        dto.setProtocol(PerimeterIntrusionStatusEnum.StreamProtocolEnum.MP4.name());

        if (!PerimeterIntrusionStatusEnum.DeviceStatusEnum.ONLINE.name().equals(point.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
            return actionResultService.callBackResult(dto);
        }

        // 撤防的防区段不提供实时画面：值班人员点开一张「没设防」的画面容易误读为安全，
        // 且撤防期间算法不识别，实时流没有任何监控意义。
        if (PerimeterIntrusionStatusEnum.DefenseStatusEnum.DISARMED.name().equals(point.getDefenseStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("POINT_DISARMED");
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
        log.info("[PerimeterIntrusion] 签发播放地址 pointId={} protocol={} canPlay={}",
            pointId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<PerimeterEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), PerimeterIntrusionConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<PerimeterEventRespDTO> listEventByCondition(PerimeterEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<PerimeterEventRespDTO> listEventByConditionForExport(PerimeterExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        PerimeterEventQueryReqDTO query = new PerimeterEventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setPointId(parseId(exportReq.getPointId(), "防区段标识"));
        query.setZone(exportReq.getZone());
        query.setCameraCode(exportReq.getCameraCode());
        query.setIntrusionType(exportReq.getIntrusionType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? PerimeterIntrusionConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), PerimeterIntrusionConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[PerimeterIntrusion] 导出事件 source={} zone={} limit={} authenticated={}",
            exportReq.getSource(), exportReq.getZone(), limit, scope.isAuthenticated());

        List<PerimeterEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态直接给可读文案；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(processStatusNameOf(row.getProcessStatus())));
        return rows;
    }

    /** 处理状态编码 → 页面文案；未知编码原样返回，不静默降级成「未核实」 */
    private String processStatusNameOf(String processStatus) {
        if (processStatus == null || processStatus.isBlank()) {
            return "未核实";
        }
        if (PerimeterIntrusionStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
            return "未核实";
        }
        if (PerimeterIntrusionStatusEnum.ProcessStatusEnum.PROCESSING.name().equals(processStatus)) {
            return "处理中";
        }
        if (PerimeterIntrusionStatusEnum.ProcessStatusEnum.DONE.name().equals(processStatus)) {
            return "已处理";
        }
        return processStatus;
    }

    @Override
    public PerimeterEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按事件 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        PerimeterEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        List<ProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        // 算法任务随详情一次带回（弹窗不再单独请求），按开始时间倒序
        List<PerimeterAlgorithmTaskRespDTO> taskViews = repository
            .listAlgorithmTasks(scope, eventId, PerimeterIntrusionConstant.DETAIL_ALGORITHM_TASK_LIMIT)
            .stream().map(this::toTaskResp).toList();
        detail.setAlgorithmTasks(taskViews);
        detail.setLatestAlgorithmTask(taskViews.isEmpty() ? null : taskViews.get(0));

        if (detail.getEvidences() != null) {
            for (PerimeterEvidenceRespDTO evidence : detail.getEvidences()) {
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
    private void grantEvidenceAddresses(PerimeterEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), PerimeterIntrusionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), PerimeterIntrusionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), PerimeterIntrusionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
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

        PerimeterEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权处理");
        }

        PerimeterIntrusionStatusEnum.ProcessStatusEnum current = parseStatus(existing.getProcessStatus());
        if (!current.isProcessable()) {
            throw new CustomException("该事件已完成核实，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), processResult, processOpinion, processedBy);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[PerimeterIntrusion] 事件处理冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("事件已被其他用户处理，请刷新后重试");
        }

        log.info("[PerimeterIntrusion] 事件处理成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>状态模型三值：PENDING 未核实 / PROCESSING 处理中 / DONE 已处理。
     * 只把 PENDING 写成 DONE；处于 PROCESSING 说明已有人在跟，
     * 不能被「谁看了一眼」顶掉，因此不写。</p>
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

        PerimeterEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        final String currentStatus = existing.getProcessStatus();
        if (PerimeterIntrusionStatusEnum.ProcessStatusEnum.DONE.name().equals(currentStatus)) {
            return actionResultService.callBackResult("该事件已是处理中或已处理状态");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), currentStatus,
            PerimeterIntrusionStatusEnum.ProcessStatusEnum.DONE.name(),
            PerimeterIntrusionConstant.AUTO_VIEW_OPINION, processedBy);

        if (affected != 1) {
            log.info("[PerimeterIntrusion] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("事件状态已变化，请刷新后重试");
        }

        log.info("[PerimeterIntrusion] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /* ==================== 参数解析 ==================== */

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private PerimeterIntrusionStatusEnum.ProcessStatusEnum parseStatus(String value) {
        try {
            return PerimeterIntrusionStatusEnum.ProcessStatusEnum.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[PerimeterIntrusion] 事件处理状态非法");
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

    /* ==================== 算法任务 / 重新识别（契约 §1） ==================== */

    @Override
    public PerimeterAlgorithmRerunRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        // 重跑是写路径：必须已认证，且事件在本人数据范围内
        AiVisionDataScope scope = requireAuthenticatedScope();
        PerimeterEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("事件不存在或无权操作");
        }

        String traceId = "pd-" + UUID.randomUUID().toString().replace("-", "");
        PerimeterAlgorithmTaskInputDTO input = new PerimeterAlgorithmTaskInputDTO();
        input.setEventId(eventId);
        input.setTaskType(PerimeterIntrusionConstant.ALGORITHM_TASK_TYPE);
        input.setWarehouseId(event.getWarehouseId());
        input.setPointId(event.getPointId());
        input.setZone(event.getZone());
        input.setCameraConfigId(event.getCameraConfigId());
        input.setCameraCode(event.getCameraCode());
        input.setMediaType(PerimeterIntrusionConstant.ALGORITHM_MEDIA_TYPE);
        // 受控内部引用，禁止透传签名后的短时 URL（第 24.2、29.3 节）
        input.setMediaRefs(List.of("mock://perimeter/" + eventId + "/intrusion"));
        input.setNormalReferenceRef("mock://perimeter/" + eventId + "/normal");
        input.setCapturedAt(event.getOccurredAt());
        input.setConfidenceThreshold(PerimeterIntrusionConstant.ALGORITHM_CONFIDENCE_THRESHOLD);
        input.setDefenseStatus(event.getDefenseStatus());
        input.setConfigVersion(PerimeterIntrusionConstant.ALGORITHM_CONFIG_VERSION);
        input.setTraceId(traceId);

        final String taskNo = algorithmGateway.submitTask(input);

        AiPerimeterAlgorithmTask task = new AiPerimeterAlgorithmTask();
        task.setTaskNo(taskNo);
        task.setEventId(parseLongOrNull(eventId));
        task.setPointId(parseLongOrNull(event.getPointId()));
        task.setCameraConfigId(parseLongOrNull(event.getCameraConfigId()));
        task.setCameraCode(event.getCameraCode());
        task.setWarehouseId(parseLongOrNull(event.getWarehouseId()));
        task.setRegionId(scope.getRegionId());
        task.setOrgId(parseLongOrNull(scope.getOrgId()));
        task.setTaskType(PerimeterIntrusionConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(PerimeterIntrusionConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(toJson(input.getMediaRefs()));
        task.setNormalReferenceRef(input.getNormalReferenceRef());
        task.setExecutionStatus(PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        task.setConfidenceThreshold(PerimeterIntrusionConstant.ALGORITHM_CONFIDENCE_THRESHOLD);
        task.setConfigVersion(PerimeterIntrusionConstant.ALGORITHM_CONFIG_VERSION);
        task.setStartedAt(LocalDateTime.now());
        task.setTraceId(traceId);
        task.setRetryCount(0);
        // 铁律 1：模拟结果必须显式 TRUE，页面据此显示「模拟」标识
        task.setIsSimulated(Boolean.TRUE);
        String operator = resolveCurrentUser(scope);
        task.setCreatedBy(operator);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedBy(operator);
        task.setUpdatedAt(LocalDateTime.now());
        repository.insertAlgorithmTask(task);

        log.info("[PerimeterIntrusion] 已受理重新识别 eventId={} taskNo={} adapter={}",
            eventId, taskNo, algorithmGateway.adapterName());

        PerimeterAlgorithmRerunRespDTO resp = new PerimeterAlgorithmRerunRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        resp.setSimulated(Boolean.TRUE);
        return resp;
    }

    @Override
    public PerimeterAlgorithmTaskRespDTO getAlgorithmTask(String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            throw new CustomException("算法任务编号不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        AiPerimeterAlgorithmTask task = repository.getAlgorithmTaskByNo(scope, taskNo);
        if (task == null) {
            throw new CustomException("算法任务不存在或无权查看");
        }
        // 未进终态才向网关取最新进度；终态任务直接回库内结果
        if (!ALGO_TERMINAL_STATUS.contains(task.getExecutionStatus())) {
            PerimeterAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskNo);
            if (output != null && ALGO_TERMINAL_STATUS.contains(output.getExecutionStatus())) {
                persistTaskResult(task, output, scope);
            }
        }
        return toTaskResp(task);
    }

    @Override
    public List<PerimeterAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        return repository.listAlgorithmTasks(resolveDataScope(), eventId,
                PerimeterIntrusionConstant.DETAIL_ALGORITHM_TASK_LIMIT)
            .stream().map(this::toTaskResp).toList();
    }

    /**
     * 把网关终态输出落库，并在成功时回写事件算法列。
     *
     * <p>失败/超时只更新任务行，<b>不回写事件</b>：事件旧算法结论仍是上一次成功取证的记录。
     * 回写只写 algorithm_* 与 intrusion_type，不动 process_status / event_version（第 23 节）。</p>
     */
    private void persistTaskResult(AiPerimeterAlgorithmTask task,
                                   PerimeterAlgorithmTaskOutputDTO output, AiVisionDataScope scope) {
        task.setExecutionStatus(output.getExecutionStatus());
        task.setConclusion(output.getConclusion());
        task.setIntrusionType(output.getIntrusionType());
        task.setConfidence(output.getConfidence());
        task.setModelName(output.getModelName());
        task.setModelVersion(output.getModelVersion());
        task.setAlgorithmVersion(output.getAlgorithmVersion());
        task.setConfigVersion(output.getConfigVersion());
        task.setStartedAt(parseAlgoTime(output.getStartedAt()));
        task.setFinishedAt(parseAlgoTime(output.getFinishedAt()));
        task.setInferenceDurationMs(output.getInferenceDurationMs());
        task.setDetections(toJson(output.getDetections()));
        task.setEvidenceRefs(toJson(output.getEvidenceRefs()));
        task.setRawResultRef(output.getRawResultRef());
        task.setErrorCode(output.getErrorCode());
        task.setErrorMessage(output.getErrorMessage());
        task.setIsSimulated(output.getSimulated() == null ? Boolean.TRUE : output.getSimulated());
        task.setUpdatedBy(resolveCurrentUserSafe(scope));
        task.setUpdatedAt(LocalDateTime.now());
        repository.updateAlgorithmTaskResult(task);

        boolean success = PerimeterIntrusionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
            .equals(output.getExecutionStatus());
        if (success && task.getEventId() != null) {
            int written = repository.writeBackEventAlgorithmResult(task.getEventId(), task.getId(),
                output.getExecutionStatus(), output.getConclusion(), output.getConfidence(),
                output.getIntrusionType(), resolveCurrentUserSafe(scope), LocalDateTime.now());
            if (written == 0) {
                log.info("[PerimeterIntrusion] 算法结果回写未生效（越权或事件已删除）eventId={}",
                    task.getEventId());
            }
        }
    }

    /** 实体 → 前端任务视图。时间统一格式化为 {@code yyyy-MM-dd HH:mm:ss}。 */
    private PerimeterAlgorithmTaskRespDTO toTaskResp(AiPerimeterAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        PerimeterAlgorithmTaskRespDTO dto = new PerimeterAlgorithmTaskRespDTO();
        dto.setTaskId(task.getTaskNo());
        dto.setTaskNo(task.getTaskNo());
        dto.setEventId(task.getEventId() == null ? null : String.valueOf(task.getEventId()));
        dto.setTaskType(task.getTaskType());
        dto.setExecutionStatus(task.getExecutionStatus());
        dto.setConclusion(task.getConclusion());
        dto.setIntrusionType(task.getIntrusionType());
        dto.setConfidence(task.getConfidence());
        dto.setModelName(task.getModelName());
        dto.setModelVersion(task.getModelVersion());
        dto.setAlgorithmVersion(task.getAlgorithmVersion());
        dto.setConfigVersion(task.getConfigVersion());
        dto.setInferenceDurationMs(task.getInferenceDurationMs());
        dto.setStartedAt(task.getStartedAt() == null ? null : ALGO_TS.format(task.getStartedAt()));
        dto.setFinishedAt(task.getFinishedAt() == null ? null : ALGO_TS.format(task.getFinishedAt()));
        dto.setDetectedCount(countDetections(task.getDetections()));
        dto.setSimulated(Boolean.TRUE.equals(task.getIsSimulated()));
        dto.setErrorCode(task.getErrorCode());
        dto.setErrorMessage(task.getErrorMessage());
        return dto;
    }

    /** 读路径兜底操作人：任务回填只是算法侧写库，未登录时以算法适配器名义记录 */
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
            log.warn("[PerimeterIntrusion] 算法输出时间格式异常，按空处理");
            return null;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return ALGORITHM_JSON.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[PerimeterIntrusion] 算法字段序列化失败，按空处理");
            return null;
        }
    }

    /** 周界算法任务表没有 detected_count 列，检出数量在读取时按 detections 数组现算 */
    private Integer countDetections(String detectionsJson) {
        if (detectionsJson == null || detectionsJson.isBlank()) {
            return null;
        }
        try {
            var node = ALGORITHM_JSON.readTree(detectionsJson);
            return node.isArray() ? node.size() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
