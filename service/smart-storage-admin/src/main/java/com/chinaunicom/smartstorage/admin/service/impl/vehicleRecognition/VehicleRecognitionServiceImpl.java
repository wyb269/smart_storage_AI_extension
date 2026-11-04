package com.chinaunicom.smartstorage.admin.service.impl.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.constant.VehicleRecognitionConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.VehicleRecognitionStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.impl.vehicleRecognition.VehicleAlgorithmTaskConverter;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.VehicleRecognitionRepository;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.VehicleRecognitionService;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.algorithm.VehicleRecognitionAlgorithmGateway;
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
 * 车辆智能识别主业务实现。
 *
 * <p><b>安全要求</b>：</p>
 * <ol>
 *   <li>核实人从登录态获取，不信任前端传入。</li>
 *   <li>使用记录版本号防止并发覆盖。</li>
 *   <li>算法识别结果（车牌/车型/颜色）不得被人工核实改写。</li>
 *   <li>所有读写路径统一注入并校验 {@link AiVisionDataScope}，禁止跨组织越权。</li>
 *   <li>写操作必须要求已认证用户，无法解析登录态时直接拒绝，不回退 anonymous。</li>
 * </ol>
 *
 * <p><b>车牌脱敏（PII 出口控制）</b>：车牌号 + 识别时间 + 识别位置可拼出个人车辆轨迹，
 * 属于个人信息。因此<b>接口原样返回完整车牌</b>（原型页面需要展示完整车牌），
 * 但<b>任何日志都不得打印完整车牌</b>——本类所有涉及车牌的日志一律先经
 * {@link #maskPlate(String)} 掩码（如 {@code 黑A***00}），只保留可读定位用的首尾片段。
 * 新增日志时务必沿用掩码，切勿把原始 plateNo 塞进占位符。</p>
 *
 * <p><b>本类刻意不加 {@code @Transactional}</b>：默认事务管理器绑定主库，
 * 而本功能读写独立库；事务由仓储实现自己声明并持有。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Slf4j
@Service
public class VehicleRecognitionServiceImpl implements VehicleRecognitionService {

    @Resource
    private VehicleRecognitionRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /** 算法适配层网关（本期唯一实现为 MockVehicleRecognitionAlgorithmGateway）。 */
    @Resource
    private VehicleRecognitionAlgorithmGateway algorithmGateway;

    private static final DateTimeFormatter ALGO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 终态：进入后不再向网关轮询，直接回库内结果（第 24.1 节）。 */
    private static final List<String> ALGO_TERMINAL_STATUS = List.of(
        VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
        VehicleRecognitionStatusEnum.AlgorithmStatusEnum.FAILED.name(),
        VehicleRecognitionStatusEnum.AlgorithmStatusEnum.TIMEOUT.name());

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[VehicleRecognition] 写操作被拒绝：请求未携带有效登录态");
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

    /* ==================== 车牌脱敏 ==================== */

    /**
     * 车牌掩码：保留前 2 位与后 2 位，中间以 {@code ***} 代替。
     *
     * <p>车牌 + 时间 + 位置可拼出个人车辆轨迹，属个人信息，因此日志只打掩码。
     * 过短的车牌（异常数据）统一返回 {@code ***}，绝不回退成原值。</p>
     */
    private String maskPlate(String plateNo) {
        if (plateNo == null || plateNo.length() < 5) {
            return "***";
        }
        return plateNo.substring(0, 2) + "***" + plateNo.substring(plateNo.length() - 2);
    }

    /* ==================== 今日识别车辆 ==================== */

    @Override
    public List<TodayVehicleRespDTO> listTodayVehicle(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), VehicleRecognitionConstant.MAX_TODAY_VEHICLE_LIMIT);
        return repository.listTodayVehicle(resolveDataScope(), safeLimit);
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId) {
        if (pointId == null || pointId.isBlank()) {
            throw new CustomException("识别点位标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从点位列表中定位摄像头，同样受数据范围约束
        MonitorPointRespDTO point = repository.listPoints(scope).stream()
            .filter(p -> pointId.equals(p.getPointId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setPointId(pointId);

        if (point == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setProtocol(VehicleRecognitionStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("POINT_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setPlatformCameraId(point.getPlatformCameraId());
        dto.setProtocol(VehicleRecognitionStatusEnum.StreamProtocolEnum.MP4.name());

        if (!VehicleRecognitionStatusEnum.DeviceStatusEnum.ONLINE.name().equals(point.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
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

        // 日志不打印完整播放地址，也不涉及车牌
        log.info("[VehicleRecognition] 签发播放地址 pointId={} protocol={} canPlay={}",
            pointId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<VehicleEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), VehicleRecognitionConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<VehicleEventRespDTO> listEventByCondition(VehicleEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<VehicleEventRespDTO> listEventByConditionForExport(VehicleExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        VehicleEventQueryReqDTO query = new VehicleEventQueryReqDTO();
        query.setPlateNo(exportReq.getPlateNo());
        query.setVehicleType(exportReq.getVehicleType());
        query.setDirection(exportReq.getDirection());
        query.setGateName(exportReq.getGateName());
        query.setVerifyStatus(exportReq.getVerifyStatus());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setPointId(parseId(exportReq.getPointId(), "识别点位标识"));
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? VehicleRecognitionConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), VehicleRecognitionConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[VehicleRecognition] 导出识别记录 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<VehicleEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，核实状态按产品口径给出可读文案；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setVerifyStatusName(verifyStatusName(row.getVerifyStatus())));
        return rows;
    }

    @Override
    public VehicleEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("识别记录标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按记录 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        VehicleEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("识别记录不存在或无权查看");
        }

        List<ProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        // 算法任务随详情一次带回（弹窗不再单独请求），按开始时间倒序
        List<VehicleAlgorithmTaskRespDTO> taskViews = VehicleAlgorithmTaskConverter.toRespList(
            repository.listAlgorithmTasksByEvent(scope, eventId,
                VehicleRecognitionConstant.MAX_ALGORITHM_TASKS_IN_DETAIL));
        detail.setAlgorithmTasks(taskViews);
        detail.setLatestAlgorithmTask(taskViews.isEmpty() ? null : taskViews.get(0));

        if (detail.getEvidences() != null) {
            for (VehicleEvidenceRespDTO evidence : detail.getEvidences()) {
                grantEvidenceAddresses(evidence);
            }
        }

        // 详情日志只打记录标识与掩码车牌，不打完整车牌
        log.info("[VehicleRecognition] 查看识别记录详情 eventId={} plate={}",
            eventId, maskPlate(detail.getPlateNo()));
        return detail;
    }

    /**
     * 为证据签发短时地址。
     *
     * <p>数据库里存的是受控对象存储键，由 MediaGateway 换成短时地址后才返回前端。
     * 原始资源与算法标注资源分别签发，互不影响。</p>
     */
    private void grantEvidenceAddresses(VehicleEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), VehicleRecognitionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), VehicleRecognitionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), VehicleRecognitionConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
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

    /* ==================== 人工核实 ==================== */

    @Override
    public ActionResult<String, String> processEvent(String eventId, String verifyResult,
                                                     String verifyOpinion, Integer eventVersion) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("识别记录标识不能为空");
        }
        if (verifyResult == null || verifyResult.isBlank()) {
            throw new CustomException("核实结果不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        VehicleEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("识别记录不存在或无权处理");
        }

        VehicleRecognitionStatusEnum.VerifyStatusEnum current = parseStatus(existing.getVerifyStatus());
        if (current != VehicleRecognitionStatusEnum.VerifyStatusEnum.PENDING) {
            throw new CustomException("该记录已完成核实，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), verifyResult, verifyOpinion, processedBy);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[VehicleRecognition] 记录核实冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("记录已被其他用户处理，请刷新后重试");
        }

        log.info("[VehicleRecognition] 记录核实成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看识别记录后自动标记为已核实。
     *
     * <p>状态模型：PENDING = 待核实，VERIFIED = 已核实，REJECTED = 误报（预留）。
     * 因此只把 PENDING 写成 VERIFIED，不引入新枚举值、不改表结构。</p>
     *
     * <p><b>幂等</b>：已是其它状态时直接返回成功且不产生任何写入，
     * 避免每次查看都追加一条审计。</p>
     */
    @Override
    public ActionResult<String, String> markEventViewed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("识别记录标识不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        VehicleEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("识别记录不存在或无权查看");
        }

        final String pending = VehicleRecognitionStatusEnum.VerifyStatusEnum.PENDING.name();
        if (!pending.equals(existing.getVerifyStatus())) {
            return actionResultService.callBackResult("该记录已是核实状态");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), pending,
            VehicleRecognitionStatusEnum.VerifyStatusEnum.VERIFIED.name(),
            VehicleRecognitionConstant.AUTO_VIEW_OPINION, processedBy);

        if (affected != 1) {
            log.info("[VehicleRecognition] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("记录状态已变化，请刷新后重试");
        }

        // 只打记录标识与掩码车牌
        log.info("[VehicleRecognition] 查看后自动标记为已核实 eventId={} plate={} by={}",
            eventId, maskPlate(existing.getPlateNo()), processedBy);
        return actionResultService.callBackResult("已标记为已核实");
    }

    /* ==================== 参数解析 ==================== */

    private String verifyStatusName(String code) {
        try {
            return VehicleRecognitionStatusEnum.VerifyStatusEnum.valueOf(code).getLabel();
        } catch (IllegalArgumentException | NullPointerException e) {
            return code;
        }
    }

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private VehicleRecognitionStatusEnum.VerifyStatusEnum parseStatus(String value) {
        try {
            return VehicleRecognitionStatusEnum.VerifyStatusEnum.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[VehicleRecognition] 记录核实状态非法");
            throw new CustomException("记录核实状态异常，请联系管理员");
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
     * 用户以为导出的是筛过的时间段，实际拿到的是全量历史（最多 5000 条）。</p>
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
    public VehicleAlgorithmRerunRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("识别记录标识不能为空");
        }
        // 重跑是写路径：必须已认证，且记录在本人数据范围内
        AiVisionDataScope scope = requireAuthenticatedScope();
        VehicleEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("识别记录不存在或无权操作");
        }
        // 归属列一律以库里的点位/仓库记录为准，不接受前端拼装
        VehicleRecognitionAlgorithmTask ctx = repository.findAlgorithmTaskContext(scope, eventId);
        if (ctx == null) {
            throw new CustomException("识别记录不存在或无权操作");
        }

        String traceId = "vc-" + UUID.randomUUID().toString().replace("-", "");
        VehicleRecognitionAlgorithmTaskInputDTO input = new VehicleRecognitionAlgorithmTaskInputDTO();
        input.setEventId(eventId);
        input.setTaskType(VehicleRecognitionConstant.ALGORITHM_TASK_TYPE);
        input.setWarehouseId(String.valueOf(ctx.getWarehouseId()));
        input.setPointId(String.valueOf(ctx.getPointId()));
        input.setCameraId(ctx.getCameraId());
        input.setCameraCode(ctx.getCameraCode());
        input.setMediaType(VehicleRecognitionConstant.ALGORITHM_MEDIA_TYPE);
        // 受控内部引用，禁止透传签名后的短时 URL（第 24.2、29.3 节）
        input.setMediaRefs(List.of("mock://vehicle/" + eventId + "/full",
            "mock://vehicle/" + eventId + "/plate"));
        input.setNormalReferenceRef("mock://vehicle/" + eventId + "/full");
        input.setCapturedAt(event.getRecognizedAt());
        input.setConfidenceThreshold(VehicleRecognitionConstant.ALGORITHM_CONFIDENCE_THRESHOLD);
        input.setConfigVersion(VehicleRecognitionConstant.ALGORITHM_CONFIG_VERSION);
        input.setTraceId(traceId);

        final String taskNo = algorithmGateway.submitTask(input);

        VehicleRecognitionAlgorithmTask task = new VehicleRecognitionAlgorithmTask();
        task.setTaskNo(taskNo);
        task.setEventId(ctx.getEventId());
        task.setPointId(ctx.getPointId());
        task.setCameraId(ctx.getCameraId());
        task.setCameraCode(ctx.getCameraCode());
        task.setWarehouseId(ctx.getWarehouseId());
        task.setRegionId(ctx.getRegionId());
        task.setTaskType(VehicleRecognitionConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(VehicleRecognitionConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(VehicleAlgorithmTaskConverter.mediaRefsJson(input.getMediaRefs()));
        task.setNormalReferenceRef(input.getNormalReferenceRef());
        task.setExecutionStatus(VehicleRecognitionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        task.setConfidenceThreshold(VehicleRecognitionConstant.ALGORITHM_CONFIDENCE_THRESHOLD);
        task.setConfigVersion(VehicleRecognitionConstant.ALGORITHM_CONFIG_VERSION);
        task.setStartedAt(LocalDateTime.now());
        task.setTraceId(traceId);
        // 铁律 1：模拟结果必须显式 TRUE，页面据此显示「模拟」标识
        task.setIsSimulated(Boolean.TRUE);
        String operator = resolveCurrentUser(scope);
        task.setCreatedBy(operator);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedBy(operator);
        task.setUpdatedAt(LocalDateTime.now());
        task.setRetryCount(0);
        repository.insertAlgorithmTask(task);

        log.info("[VehicleRecognition] 已受理重新识别 eventId={} taskNo={} adapter={}",
            eventId, taskNo, algorithmGateway.adapterName());

        VehicleAlgorithmRerunRespDTO resp = new VehicleAlgorithmRerunRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(VehicleRecognitionStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        resp.setSimulated(Boolean.TRUE);
        return resp;
    }

    @Override
    public VehicleAlgorithmTaskRespDTO getAlgorithmTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new CustomException("算法任务编号不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        VehicleRecognitionAlgorithmTask task = repository.findAlgorithmTaskByTaskNo(scope, taskId);
        if (task == null) {
            throw new CustomException("算法任务不存在或无权查看");
        }
        // 未进终态才向网关取最新进度；终态任务直接回库内结果
        if (!ALGO_TERMINAL_STATUS.contains(task.getExecutionStatus())) {
            VehicleRecognitionAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskId);
            if (output != null && ALGO_TERMINAL_STATUS.contains(output.getExecutionStatus())) {
                persistTaskResult(scope, task, output);
                VehicleRecognitionAlgorithmTask refreshed =
                    repository.findAlgorithmTaskByTaskNo(scope, taskId);
                return VehicleAlgorithmTaskConverter.toResp(refreshed == null ? task : refreshed);
            }
        }
        return VehicleAlgorithmTaskConverter.toResp(task);
    }

    @Override
    public List<VehicleAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("识别记录标识不能为空");
        }
        List<VehicleRecognitionAlgorithmTask> tasks = repository.listAlgorithmTasksByEvent(
            resolveDataScope(), eventId, VehicleRecognitionConstant.MAX_ALGORITHM_TASKS_QUERY);
        return VehicleAlgorithmTaskConverter.toRespList(tasks);
    }

    /**
     * 把网关终态输出落库，并在成功时回写事件算法列。
     *
     * <p>失败/超时只更新任务行，<b>不回写事件</b>：事件旧算法结论仍是上一次成功取证的记录，
     * 不能被一次失败的尝试清空。回写只写 algorithm_status/confidence，不动 verify_*（第 23 节）。</p>
     */
    private void persistTaskResult(AiVisionDataScope scope, VehicleRecognitionAlgorithmTask task,
                                   VehicleRecognitionAlgorithmTaskOutputDTO output) {
        VehicleRecognitionAlgorithmTask entity = new VehicleRecognitionAlgorithmTask();
        entity.setId(task.getId());
        entity.setTaskNo(task.getTaskNo());
        entity.setExecutionStatus(output.getExecutionStatus());
        entity.setConclusion(output.getConclusion());
        entity.setVehicleType(output.getVehicleType());
        entity.setConfidence(output.getConfidence());
        entity.setModelName(output.getModelName());
        entity.setModelVersion(output.getModelVersion());
        entity.setAlgorithmVersion(output.getAlgorithmVersion());
        entity.setConfigVersion(output.getConfigVersion());
        entity.setStartedAt(parseAlgoTime(output.getStartedAt()));
        entity.setFinishedAt(parseAlgoTime(output.getFinishedAt()));
        entity.setInferenceDurationMs(output.getInferenceDurationMs());
        entity.setDetections(VehicleAlgorithmTaskConverter.detectionsJson(
            output, String.valueOf(task.getEventId())));
        entity.setEvidenceRefs(VehicleAlgorithmTaskConverter.evidenceRefsJson(output));
        entity.setRawResultRef(output.getRawResultRef());
        entity.setErrorCode(output.getErrorCode());
        entity.setErrorMessage(output.getErrorMessage());
        // 网关自身模拟标识为准；缺省按铁律 1 落 TRUE
        entity.setIsSimulated(output.getSimulated() == null ? Boolean.TRUE : output.getSimulated());
        entity.setUpdatedBy(resolveCurrentUserSafe(scope));
        entity.setUpdatedAt(LocalDateTime.now());

        int updated = repository.updateAlgorithmTask(scope, entity);
        if (updated == 0) {
            log.warn("[VehicleRecognition] 算法任务回填未命中 taskNo={}", task.getTaskNo());
            return;
        }
        boolean success = VehicleRecognitionStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
            .equals(output.getExecutionStatus());
        if (success && task.getEventId() != null) {
            int written = repository.writeBackEventAlgorithmResult(scope,
                String.valueOf(task.getEventId()), task.getId(), output.getExecutionStatus(),
                output.getConfidence(), resolveCurrentUserSafe(scope), LocalDateTime.now());
            if (written == 0) {
                log.info("[VehicleRecognition] 算法结果回写未生效（越权或记录已删除）eventId={}",
                    task.getEventId());
            }
        }
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
            log.warn("[VehicleRecognition] 算法输出时间格式异常，按空处理");
            return null;
        }
    }
}
