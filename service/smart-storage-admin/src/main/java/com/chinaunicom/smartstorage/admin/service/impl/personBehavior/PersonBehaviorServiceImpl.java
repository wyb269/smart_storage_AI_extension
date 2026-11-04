package com.chinaunicom.smartstorage.admin.service.impl.personBehavior;

import com.chinaunicom.smartstorage.admin.constant.PersonBehaviorConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.personBehavior.AiPersonAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.PersonBehaviorStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.personBehavior.PersonBehaviorRepository;
import com.chinaunicom.smartstorage.admin.service.inter.personBehavior.PersonBehaviorService;
import com.chinaunicom.smartstorage.admin.service.inter.personBehavior.algorithm.PersonBehaviorAlgorithmGateway;
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
import java.util.Objects;
import java.util.UUID;

/**
 * 人员行为监测主业务实现。
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
 * @author aiVision 功能族 · 人员行为监测
 */
@Slf4j
@Service
public class PersonBehaviorServiceImpl implements PersonBehaviorService {

    @Resource
    private PersonBehaviorRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /**
     * 算法适配层网关。
     *
     * <p>本页面的「重新识别」经它提交与查询，当前唯一实现是
     * {@code MockPersonBehaviorAlgorithmGateway}；真实算法到达时
     * 只换一个实现类，本类与页面接口都不动。</p>
     */
    @Resource
    private PersonBehaviorAlgorithmGateway algorithmGateway;

    /** detections / evidenceRefs 落库前的 JSON 序列化（不启新依赖，Spring 自带 Jackson） */
    private static final ObjectMapper ALGORITHM_JSON = new ObjectMapper();

    private static final DateTimeFormatter TASK_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[PersonBehavior] 写操作被拒绝：请求未携带有效登录态");
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

    /* ==================== 监测区域状态 ==================== */

    @Override
    public List<MonitorPointStatusRespDTO> listMonitorPoint() {
        return repository.listMonitorPoint(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId) {
        if (pointId == null || pointId.isBlank()) {
            throw new CustomException("监测区域标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从区域状态列表中定位区域，同样受数据范围约束
        MonitorPointStatusRespDTO point = repository.listMonitorPoint(scope).stream()
            .filter(p -> pointId.equals(p.getPointId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setPointId(pointId);

        if (point == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setProtocol(PersonBehaviorStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("POINT_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setCameraCode(point.getCameraCode());
        dto.setPlatformCameraId(point.getPlatformCameraId());
        dto.setProtocol(PersonBehaviorStatusEnum.StreamProtocolEnum.MP4.name());

        if (!PersonBehaviorStatusEnum.DeviceStatusEnum.ONLINE.name().equals(point.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
            return actionResultService.callBackResult(dto);
        }

        MediaGateway.PlaybackRequest request = new MediaGateway.PlaybackRequest(
            pointId, point.getCameraId(), point.getWarehouseId(), null, null, null);

        MediaGateway.PlaybackGrant grant = mediaGateway.signPlayback(request);
        dto.setCanPlay(grant.canPlay());
        dto.setPlayableUrl(grant.playableUrl());
        dto.setExpiresAt(grant.expiresAt());
        if (grant.protocol() != null) {
            dto.setProtocol(grant.protocol());
        }
        dto.setUnavailableReason(grant.unavailableReason());

        // 日志不打印完整播放地址
        log.info("[PersonBehavior] 签发播放地址 pointId={} protocol={} canPlay={}",
            pointId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<PersonEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), PersonBehaviorConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<PersonEventRespDTO> listEventByCondition(PersonEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<PersonEventRespDTO> listEventByConditionForExport(PersonExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        PersonEventQueryReqDTO query = new PersonEventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setPointId(parseId(exportReq.getPointId(), "监测区域标识"));
        query.setCameraCode(exportReq.getCameraCode());
        query.setBehaviorType(exportReq.getBehaviorType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? PersonBehaviorConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), PersonBehaviorConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[PersonBehavior] 导出事件 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<PersonEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态直接给可读文案；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(processStatusLabel(row.getProcessStatus())));
        return rows;
    }

    /** 处理状态编码 → 展示名；未知编码原样返回，不让导出因脏数据整批失败 */
    private String processStatusLabel(String processStatus) {
        if (PersonBehaviorStatusEnum.ProcessStatusEnum.PENDING.name().equals(processStatus)) {
            return "未核实";
        }
        if (PersonBehaviorStatusEnum.ProcessStatusEnum.PROCESSING.name().equals(processStatus)) {
            return "处理中";
        }
        if (PersonBehaviorStatusEnum.ProcessStatusEnum.DONE.name().equals(processStatus)) {
            return "已处理";
        }
        return processStatus == null ? "" : processStatus;
    }

    @Override
    public PersonEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按事件 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        PersonEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        List<ProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        // 算法任务随详情一次带回（弹窗不再单独请求）；无任务时为空列表，
        // 前端显示「暂无算法任务」。
        List<PersonAlgorithmTaskRespDTO> taskDtos =
            repository.listAlgorithmTasksByEventId(scope, eventId,
                    PersonBehaviorConstant.MAX_ALGORITHM_TASKS)
                .stream().map(this::toTaskResp).toList();
        detail.setAlgorithmTasks(taskDtos);
        detail.setLatestAlgorithmTask(taskDtos.isEmpty() ? null : taskDtos.get(0));

        if (detail.getEvidences() != null) {
            for (PersonEvidenceRespDTO evidence : detail.getEvidences()) {
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
    private void grantEvidenceAddresses(PersonEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), PersonBehaviorConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), PersonBehaviorConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), PersonBehaviorConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
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

        PersonEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权处理");
        }

        PersonBehaviorStatusEnum.ProcessStatusEnum current = parseStatus(existing.getProcessStatus());
        if (!current.isProcessable()) {
            throw new CustomException("该事件已完成核实，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), processResult, processOpinion, processedBy,
            PersonBehaviorConstant.PROCESS_SOURCE_WEB);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[PersonBehavior] 事件处理冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("事件已被其他用户处理，请刷新后重试");
        }

        log.info("[PersonBehavior] 事件处理成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>只把 PENDING（未核实）写成 DONE。PROCESSING（处理中）表示已有人工介入，
     * 不能被一次「查看」抢着终结，否则处理中的事件会被无声关掉。</p>
     *
     * <p><b>幂等</b>：非 PENDING 时直接返回成功且不产生任何写入，
     * 避免每次查看都追加一条审计。</p>
     */
    @Override
    public ActionResult<String, String> markEventViewed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        AiVisionDataScope scope = requireAuthenticatedScope();

        PersonEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        final String currentStatus = existing.getProcessStatus();
        if (PersonBehaviorStatusEnum.ProcessStatusEnum.DONE.name().equals(currentStatus)) {
            return actionResultService.callBackResult("该事件无需重复标记");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), currentStatus,
            PersonBehaviorStatusEnum.ProcessStatusEnum.DONE.name(),
            PersonBehaviorConstant.AUTO_VIEW_OPINION, processedBy,
            PersonBehaviorConstant.PROCESS_SOURCE_SYSTEM);

        if (affected != 1) {
            log.info("[PersonBehavior] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("事件状态已变化，请刷新后重试");
        }

        log.info("[PersonBehavior] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /* ==================== 参数解析 ==================== */

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private PersonBehaviorStatusEnum.ProcessStatusEnum parseStatus(String value) {
        try {
            return PersonBehaviorStatusEnum.ProcessStatusEnum.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[PersonBehavior] 事件处理状态非法");
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

    /** 终态：进入后不再向网关轮询，直接回库内结果（第 24.1 节）。 */
    private static final List<String> ALGO_TERMINAL_STATUS = List.of(
        PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name(),
        PersonBehaviorStatusEnum.AlgorithmStatusEnum.FAILED.name(),
        PersonBehaviorStatusEnum.AlgorithmStatusEnum.TIMEOUT.name());

    @Override
    public PersonAlgorithmRerunRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        // 重跑是写路径：必须已认证，且事件在本人数据范围内
        AiVisionDataScope scope = requireAuthenticatedScope();
        PersonEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("事件不存在或无权操作");
        }

        String traceId = "pb-" + UUID.randomUUID().toString().replace("-", "");
        PersonAlgorithmTaskInputDTO input = new PersonAlgorithmTaskInputDTO();
        input.setEventId(eventId);
        input.setTaskType(PersonBehaviorConstant.ALGORITHM_TASK_TYPE);
        input.setWarehouseId(event.getWarehouseId());
        input.setPointId(event.getPointId());
        input.setCameraCode(event.getCameraCode());
        input.setMediaType(PersonBehaviorConstant.ALGORITHM_MEDIA_TYPE);
        // 受控内部引用，禁止透传签名后的短时 URL（第 24.2、29.3 节）
        input.setMediaRefs(List.of("mock://person/" + eventId + "/behavior"));
        input.setNormalReferenceRef("mock://person/" + eventId + "/normal");
        input.setCapturedAt(event.getOccurredAt());
        input.setConfidenceThreshold(new BigDecimal("0.7500"));
        input.setConfigVersion(PersonBehaviorConstant.ALGORITHM_CONFIG_VERSION);
        input.setTraceId(traceId);

        final String taskNo = algorithmGateway.submitTask(input);

        AiPersonAlgorithmTask task = new AiPersonAlgorithmTask();
        task.setTaskNo(taskNo);
        task.setEventId(parseLongOrNull(eventId));
        task.setPointId(parseLongOrNull(event.getPointId()));
        task.setCameraCode(event.getCameraCode());
        task.setWarehouseId(parseLongOrNull(event.getWarehouseId()));
        task.setRegionId(scope.getRegionId());
        task.setOrgId(parseLongOrNull(scope.getOrgId()));
        task.setTaskType(PersonBehaviorConstant.ALGORITHM_TASK_TYPE);
        task.setMediaType(PersonBehaviorConstant.ALGORITHM_MEDIA_TYPE);
        task.setMediaRefs(toJsonArray(input.getMediaRefs()));
        task.setNormalReferenceRef(input.getNormalReferenceRef());
        task.setExecutionStatus(PersonBehaviorStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        task.setConfidenceThreshold(new BigDecimal("0.7500"));
        task.setConfigVersion(PersonBehaviorConstant.ALGORITHM_CONFIG_VERSION);
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

        log.info("[PersonBehavior] 已受理重新识别 eventId={} taskNo={} adapter={}",
            eventId, taskNo, algorithmGateway.adapterName());

        PersonAlgorithmRerunRespDTO resp = new PersonAlgorithmRerunRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(PersonBehaviorStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        resp.setSimulated(Boolean.TRUE);
        return resp;
    }

    @Override
    public PersonAlgorithmTaskRespDTO getAlgorithmTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new CustomException("算法任务编号不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        AiPersonAlgorithmTask task = repository.findAlgorithmTaskByTaskNo(scope, taskId);
        if (task == null) {
            throw new CustomException("算法任务不存在或无权查看");
        }
        // 未进终态才向网关取最新进度；终态任务直接回库内结果
        if (!ALGO_TERMINAL_STATUS.contains(task.getExecutionStatus())) {
            PersonAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskId);
            if (output != null && ALGO_TERMINAL_STATUS.contains(output.getExecutionStatus())) {
                persistTaskResult(scope, task, output);
            }
        }
        return toTaskResp(task);
    }

    @Override
    public List<PersonAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        return repository.listAlgorithmTasksByEventId(resolveDataScope(), eventId,
                PersonBehaviorConstant.MAX_ALGORITHM_TASKS)
            .stream().map(this::toTaskResp).toList();
    }

    /**
     * 把网关终态输出落库，并在成功时回写事件算法列。
     *
     * <p>失败/超时只更新任务行，<b>不回写事件</b>：事件旧算法结论仍是上一次成功取证的记录，
     * 不能被一次失败的尝试清空。回写只写 algorithm_* 列，不动 process_status（第 23 节）。</p>
     */
    private void persistTaskResult(AiVisionDataScope scope, AiPersonAlgorithmTask task,
                                   PersonAlgorithmTaskOutputDTO output) {
        task.setExecutionStatus(output.getExecutionStatus());
        task.setConclusion(output.getConclusion());
        task.setBehaviorType(output.getBehaviorType());
        task.setConfidence(output.getConfidence());
        task.setDetectedCount(output.getPersonCount());
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
        repository.updateAlgorithmTask(task);

        boolean success = PersonBehaviorStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
            .equals(output.getExecutionStatus());
        if (success && task.getEventId() != null) {
            int written = repository.backfillEventAlgorithmResult(scope, task.getEventId(),
                task.getId(), output.getExecutionStatus(), output.getConclusion(),
                output.getConfidence(), output.getBehaviorType(),
                resolveCurrentUserSafe(scope), LocalDateTime.now());
            if (written == 0) {
                log.info("[PersonBehavior] 算法结果回写未生效（越权或事件已删除）eventId={}",
                    task.getEventId());
            }
        }
    }

    /** 实体 → 前端任务视图。时间统一格式化为 {@code yyyy-MM-dd HH:mm:ss}。 */
    private PersonAlgorithmTaskRespDTO toTaskResp(AiPersonAlgorithmTask task) {
        if (task == null) {
            return null;
        }
        PersonAlgorithmTaskRespDTO dto = new PersonAlgorithmTaskRespDTO();
        dto.setTaskId(task.getTaskNo());
        dto.setTaskNo(task.getTaskNo());
        dto.setEventId(task.getEventId() == null ? null : String.valueOf(task.getEventId()));
        dto.setExecutionStatus(task.getExecutionStatus());
        dto.setConclusion(task.getConclusion());
        dto.setBehaviorType(task.getBehaviorType());
        dto.setConfidence(task.getConfidence());
        dto.setModelName(task.getModelName());
        dto.setModelVersion(task.getModelVersion());
        dto.setAlgorithmVersion(task.getAlgorithmVersion());
        dto.setConfigVersion(task.getConfigVersion());
        dto.setInferenceDurationMs(task.getInferenceDurationMs());
        dto.setStartedAt(task.getStartedAt() == null ? null : TASK_TS.format(task.getStartedAt()));
        dto.setFinishedAt(task.getFinishedAt() == null ? null : TASK_TS.format(task.getFinishedAt()));
        dto.setDetectedCount(task.getDetectedCount());
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
            return LocalDateTime.parse(value, TASK_TS);
        } catch (Exception e) {
            log.warn("[PersonBehavior] 算法输出时间格式异常，按空处理");
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

    private String toJsonArray(List<String> refs) {
        return toJson(refs == null ? List.of() : refs);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return ALGORITHM_JSON.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[PersonBehavior] 算法字段序列化失败，按空处理");
            return null;
        }
    }
}
