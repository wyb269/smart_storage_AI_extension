package com.chinaunicom.smartstorage.admin.service.impl.beltMachine;

import com.chinaunicom.smartstorage.admin.constant.BeltMachineConstant;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineStreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.beltMachine.BeltMachineAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.BeltMachineStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.admin.service.inter.beltMachine.BeltMachineRepository;
import com.chinaunicom.smartstorage.admin.service.inter.beltMachine.BeltMachineService;
import com.chinaunicom.smartstorage.admin.service.inter.beltMachine.algorithm.BeltMachineAlgorithmGateway;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 皮带机下站人监测主业务实现。
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
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Slf4j
@Service
public class BeltMachineServiceImpl implements BeltMachineService {

    @Resource
    private BeltMachineRepository repository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /**
     * 算法适配层网关。本期唯一实现是 Mock 适配器，真实算法到达后只换实现类，
     * 本类与页面接口都不动（AI_HANDOFF 第 24.5 节）。
     */
    @Resource
    private BeltMachineAlgorithmGateway algorithmGateway;

    /** 任务视图 detectedCount 由 detections JSON 数组长度派生，仅做解析不解释语义 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* ==================== 认证与数据范围 ==================== */

    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /** 解析已认证数据范围，用于写操作；未认证直接拒绝 */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            log.warn("[BeltMachine] 写操作被拒绝：请求未携带有效登录态");
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

    /* ==================== 皮带机点位状态 ==================== */

    @Override
    public List<BeltMachineMonitorPointRespDTO> listMonitorPoint() {
        return repository.listMonitorPoint(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<BeltMachineStreamInfoRespDTO, String> getStreamInfo(String pointId) {
        if (pointId == null || pointId.isBlank()) {
            throw new CustomException("监测点位标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从点位状态列表中定位皮带机，同样受数据范围约束
        BeltMachineMonitorPointRespDTO point = repository.listMonitorPoint(scope).stream()
            .filter(p -> pointId.equals(p.getPointId()))
            .findFirst()
            .orElse(null);

        BeltMachineStreamInfoRespDTO dto = new BeltMachineStreamInfoRespDTO();
        dto.setPointId(pointId);

        if (point == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在
            dto.setCanPlay(false);
            dto.setProtocol(BeltMachineStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("POINT_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setCameraId(point.getCameraId());
        dto.setCameraCode(point.getCameraCode());
        dto.setCameraName(point.getCameraName());
        dto.setProtocol(BeltMachineStatusEnum.StreamProtocolEnum.MP4.name());

        // 「未部署」的点位没有可播对象。这一条必须在设备状态判断之前：
        // 未绑摄像头的点位 device_status 也是 UNKNOWN，落到下一分支会得到
        // 「摄像头离线」，运维去查网络而实际要做的是装摄像头。
        if (point.getCameraId() == null || point.getCameraId().isBlank()) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_NOT_BOUND");
            return actionResultService.callBackResult(dto);
        }

        if (!BeltMachineStatusEnum.DeviceStatusEnum.ONLINE.name().equals(point.getDeviceStatus())) {
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
        log.info("[BeltMachine] 签发播放地址 pointId={} protocol={} canPlay={}",
            pointId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 事件查询 ==================== */

    @Override
    public List<BeltMachineEventRespDTO> listRecentEvent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), BeltMachineConstant.MAX_RECENT_EVENT_LIMIT);
        return repository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    @Override
    public List<BeltMachineEventRespDTO> listEventByCondition(BeltMachineEventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return repository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<BeltMachineEventRespDTO> listEventByConditionForExport(BeltMachineExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        BeltMachineEventQueryReqDTO query = new BeltMachineEventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setPointId(parseId(exportReq.getPointId(), "监测点位标识"));
        query.setDeviceName(exportReq.getDeviceName());
        query.setCameraCode(exportReq.getCameraCode());
        query.setConclusionType(exportReq.getConclusionType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        int limit = exportReq.getLimit() == null
            ? BeltMachineConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), BeltMachineConstant.EXPORT_MAX_ROWS);

        // 导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();
        log.info("[BeltMachine] 导出事件 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<BeltMachineEventRespDTO> rows = repository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态直接给三值中文文案；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(processStatusLabel(row.getProcessStatus())));
        return rows;
    }

    @Override
    public BeltMachineEventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 详情必须带数据范围校验，不能只按事件 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        BeltMachineEventDetailRespDTO detail = repository.getEventDetail(scope, eventId);
        if (detail == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        List<BeltMachineProcessRecordRespDTO> records = repository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        if (detail.getEvidences() != null) {
            for (BeltMachineEvidenceRespDTO evidence : detail.getEvidences()) {
                grantEvidenceAddresses(evidence);
            }
        }

        // 算法任务随详情一起返回：弹窗「算法任务」区直接用这份数据，不再单独发请求。
        // 注意必须在证据换发短时地址之前保持受控引用的原样——任务列表本身不含引用原文。
        List<BeltMachineAlgorithmTaskRespDTO> tasks = toTaskRespList(
            repository.listAlgorithmTasks(scope, eventId, BeltMachineConstant.DETAIL_ALGORITHM_TASK_LIMIT));
        detail.setAlgorithmTasks(tasks);
        detail.setLatestAlgorithmTask(tasks.isEmpty() ? null : tasks.get(0));

        return detail;
    }

    /**
     * 为证据签发短时地址。
     *
     * <p>数据库里存的是受控对象存储键，由 MediaGateway 换成短时地址后才返回前端。
     * 原始资源与算法标注资源分别签发，互不影响。</p>
     */
    private void grantEvidenceAddresses(BeltMachineEvidenceRespDTO evidence) {
        if (isControlledReference(evidence.getOriginalUrl())) {
            evidence.setOriginalUrl(mediaGateway.signEvidence(
                evidence.getOriginalUrl(), BeltMachineConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            evidence.setAnnotatedUrl(mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), BeltMachineConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
        }
        if (isControlledReference(evidence.getThumbnailUrl())) {
            evidence.setThumbnailUrl(mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), BeltMachineConstant.EVIDENCE_URL_EXPIRE_MINUTES).url());
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

        BeltMachineEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权处理");
        }

        BeltMachineStatusEnum.ProcessStatusEnum current = parseStatus(existing.getProcessStatus());
        if (!current.isProcessable()) {
            throw new CustomException("该事件已完成核实，无法重复处理");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(scope, eventId, eventVersion,
            current.name(), processResult, processOpinion, processedBy, BeltMachineConstant.SOURCE_WEB);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且不写审计
        if (affected != 1) {
            log.info("[BeltMachine] 事件处理冲突 eventId={} 提交版本={}", eventId, eventVersion);
            throw new CustomException("事件已被其他用户处理，请刷新后重试");
        }

        log.info("[BeltMachine] 事件处理成功 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>本功能处理状态三值：PENDING 未核实 / PROCESSING 处理中 / DONE 已处理。
     * 只把 PENDING 写成 DONE；PROCESSING 说明有人工流程在跑，
     * 不能被一次「查看」悄悄终结。</p>
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

        BeltMachineEventDetailRespDTO existing = repository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        final String currentStatus = existing.getProcessStatus();
        if (BeltMachineStatusEnum.ProcessStatusEnum.DONE.name().equals(currentStatus)) {
            return actionResultService.callBackResult("该事件已不在未核实状态");
        }

        String processedBy = resolveCurrentUser(scope);

        int affected = repository.processEvent(
            scope, eventId, existing.getEventVersion(), currentStatus,
            BeltMachineStatusEnum.ProcessStatusEnum.DONE.name(),
            BeltMachineConstant.AUTO_VIEW_OPINION, processedBy, BeltMachineConstant.SOURCE_SYSTEM);

        if (affected != 1) {
            log.info("[BeltMachine] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("事件状态已变化，请刷新后重试");
        }

        log.info("[BeltMachine] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /* ==================== 参数解析 ==================== */

    /** 处理状态三值转中文；未知编码原样返回，不让脏数据把导出打挂 */
    private String processStatusLabel(String code) {
        for (BeltMachineStatusEnum.ProcessStatusEnum value : BeltMachineStatusEnum.ProcessStatusEnum.values()) {
            if (value.name().equals(code)) {
                return value.getLabel();
            }
        }
        return code;
    }

    /** 存量脏数据不应导致 500，向前端给出可理解的提示 */
    private BeltMachineStatusEnum.ProcessStatusEnum parseStatus(String value) {
        try {
            return BeltMachineStatusEnum.ProcessStatusEnum.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[BeltMachine] 事件处理状态非法");
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

    /* ==================== 算法任务（重新识别链路） ==================== */

    /**
     * 发起「重新识别」。
     *
     * <p>链路与契约一致：<b>校验 → 组输入 → submitTask → 落 QUEUED/RUNNING 行 → 立即返回</b>。
     * 绝不在本请求里等待算法结果（AI_HANDOFF 第 24.1 节）。</p>
     *
     * <p>「未部署」点位（点位与事件快照都拿不到摄像头标识）直接拒绝：
     * 没有媒体输入却产出结论等于伪造，与取流接口 {@code CAMERA_NOT_BOUND} 同一语义。</p>
     */
    @Override
    public BeltMachineAlgorithmRerunRespDTO rerunAlgorithm(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        AiVisionDataScope scope = requireAuthenticatedScope();

        BeltMachineEventDetailRespDTO event = repository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("事件不存在或无权操作");
        }

        BeltMachineMonitorPointRespDTO point = repository.listMonitorPoint(scope).stream()
            .filter(p -> Objects.equals(p.getPointId(), event.getPointId()))
            .findFirst()
            .orElse(null);
        String cameraId = point == null ? null : point.getCameraId();
        boolean cameraMissing = (cameraId == null || cameraId.isBlank())
            && (event.getCameraCode() == null || event.getCameraCode().isBlank());
        if (cameraMissing) {
            throw new CustomException("该皮带机未绑定监测摄像头（CAMERA_NOT_BOUND），无法重新识别");
        }

        // 媒体输入只用库里的受控引用原文（repository 返回的证据未经换签），
        // 换发短时地址是读路径的事，算法输入里绝不出现 http 地址
        List<String> mediaRefs = new ArrayList<>();
        String normalReferenceRef = null;
        if (event.getEvidences() != null) {
            for (BeltMachineEvidenceRespDTO evidence : event.getEvidences()) {
                if (!isControlledReference(evidence.getOriginalUrl())) {
                    continue;
                }
                mediaRefs.add(evidence.getOriginalUrl());
                if (BeltMachineStatusEnum.EvidenceTypeEnum.NORMAL_REFERENCE.name()
                    .equals(evidence.getEvidenceType())) {
                    normalReferenceRef = evidence.getOriginalUrl();
                }
            }
        }

        String traceId = "belt-rerun-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        BeltMachineAlgorithmTaskInputDTO input = new BeltMachineAlgorithmTaskInputDTO();
        input.setEventId(event.getEventId());
        input.setTaskType(BeltMachineConstant.ALGORITHM_TASK_TYPE);
        input.setWarehouseId(event.getWarehouseId());
        input.setPointId(event.getPointId());
        input.setCameraId(cameraId);
        input.setCameraCode(event.getCameraCode());
        input.setDeviceName(event.getDeviceName());
        input.setMediaType(BeltMachineConstant.ALGORITHM_MEDIA_TYPE);
        input.setMediaRefs(mediaRefs);
        input.setCapturedAt(event.getOccurredAt());
        input.setNormalReferenceRef(normalReferenceRef);
        input.setRoi(BeltMachineConstant.ALGORITHM_ROI);
        input.setConfigVersion(BeltMachineConstant.ALGORITHM_CONFIG_VERSION);
        input.setTraceId(traceId);

        String taskNo = algorithmGateway.submitTask(input);
        log.info("[BeltMachine] 重新识别已受理 eventId={} taskNo={} adapter={}",
            eventId, taskNo, algorithmGateway.adapterName());

        // 立即取一次进度：多数情况是 QUEUED；Mock 延迟极短或进程重启后
        // 可能已经出结果，按实际状态落行，不写死 QUEUED
        BeltMachineAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskNo);

        BeltMachineAlgorithmTask task = newTaskFromInput(input, taskNo, event, scope);
        applyOutput(task, output);
        long rowId = repository.insertAlgorithmTask(task);
        task.setId(rowId);

        if (isSucceeded(task.getExecutionStatus())) {
            writeBackEvent(scope, task);
        }

        BeltMachineAlgorithmRerunRespDTO resp = new BeltMachineAlgorithmRerunRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(task.getExecutionStatus());
        resp.setSimulated(Boolean.TRUE.equals(task.getIsSimulated()));
        return resp;
    }

    /**
     * 轮询算法任务：先查库（带范围），未进终态时取网关最新结果落库，
     * 成功则回写事件算法列。
     */
    @Override
    public BeltMachineAlgorithmTaskRespDTO getAlgorithmTask(String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            throw new CustomException("算法任务标识不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();

        BeltMachineAlgorithmTask task = repository.getAlgorithmTaskByNo(scope, taskNo);
        if (task == null) {
            // 不区分「不存在」与「无权限」，避免探测任务编号
            throw new CustomException("算法任务不存在或无权查看");
        }

        if (!isTerminal(task.getExecutionStatus())) {
            BeltMachineAlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskNo);
            boolean advanced = output != null
                && !Objects.equals(task.getExecutionStatus(), output.getExecutionStatus());
            if (advanced) {
                applyOutput(task, output);
                repository.updateAlgorithmTask(task);
                if (isSucceeded(task.getExecutionStatus()) && task.getEventId() != null) {
                    writeBackEvent(scope, task);
                }
            }
        }
        return toTaskResp(task);
    }

    @Override
    public List<BeltMachineAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        return toTaskRespList(repository.listAlgorithmTasks(
            scope, eventId, BeltMachineConstant.DETAIL_ALGORITHM_TASK_LIMIT));
    }

    /* ---------- 算法任务私有辅助 ---------- */

    /** 用事件与输入组装任务行；执行结果列随后由 applyOutput 覆盖 */
    private BeltMachineAlgorithmTask newTaskFromInput(BeltMachineAlgorithmTaskInputDTO input,
                                                      String taskNo,
                                                      BeltMachineEventDetailRespDTO event,
                                                      AiVisionDataScope scope) {
        BeltMachineAlgorithmTask task = new BeltMachineAlgorithmTask();
        task.setTaskNo(taskNo);
        task.setEventId(parseLong(event.getEventId()));
        task.setPointId(parseLong(event.getPointId()));
        task.setCameraId(input.getCameraId());
        task.setCameraCode(event.getCameraCode());
        task.setWarehouseId(parseLong(event.getWarehouseId()));
        task.setTaskType(input.getTaskType());
        task.setMediaType(input.getMediaType());
        task.setMediaRefs(toJson(input.getMediaRefs()));
        task.setNormalReferenceRef(input.getNormalReferenceRef());
        task.setRoi(input.getRoi());
        task.setConfigVersion(input.getConfigVersion());
        task.setTraceId(input.getTraceId());
        task.setRetryCount(0);
        task.setIsSimulated(Boolean.TRUE);
        task.setCreatedBy(scope.isAuthenticated() ? resolveCurrentUser(scope) : "system");
        task.setUpdatedBy(task.getCreatedBy());
        return task;
    }

    /** 把网关输出覆盖到任务行；模拟适配器恒标 simulated=true */
    private void applyOutput(BeltMachineAlgorithmTask task, BeltMachineAlgorithmTaskOutputDTO output) {
        if (output == null) {
            return;
        }
        task.setExecutionStatus(output.getExecutionStatus());
        task.setConclusion(output.getConclusion());
        task.setConclusionType(output.getConclusionType());
        task.setConfidence(output.getConfidence());
        if (output.getModelName() != null) {
            task.setModelName(output.getModelName());
        }
        if (output.getModelVersion() != null) {
            task.setModelVersion(output.getModelVersion());
        }
        if (output.getAlgorithmVersion() != null) {
            task.setAlgorithmVersion(output.getAlgorithmVersion());
        }
        if (output.getConfigVersion() != null) {
            task.setConfigVersion(output.getConfigVersion());
        }
        task.setStartedAt(parseTimeOrEmpty(output.getStartedAt()));
        task.setFinishedAt(parseTimeOrEmpty(output.getFinishedAt()));
        task.setInferenceDurationMs(output.getInferenceDurationMs());
        task.setDetections(toJson(output.getDetections()));
        task.setEvidenceRefs(toJson(output.getEvidenceRefs()));
        task.setRawResultRef(output.getRawResultRef());
        task.setErrorCode(output.getErrorCode());
        task.setErrorMessage(output.getErrorMessage());
        task.setIsSimulated(!Boolean.FALSE.equals(output.getSimulated()));
    }

    /** 任务成功 → 回写事件算法列；失败与进行中不回写，事件保持上一次取证结果 */
    private void writeBackEvent(AiVisionDataScope scope, BeltMachineAlgorithmTask task) {
        String updatedBy = scope.isAuthenticated() ? resolveCurrentUser(scope) : "system";
        int affected = repository.writeBackEventAlgorithm(scope,
            String.valueOf(task.getEventId()), task.getId(),
            task.getExecutionStatus(), task.getConclusion(),
            task.getConclusionType(), task.getConfidence(), updatedBy);
        if (affected == 0) {
            // 任务行已落库，事件回写失败（并发删除/越权）只告警不回滚取证记录
            log.warn("[BeltMachine] 算法结果回写事件未生效 taskId={} eventId={}",
                task.getId(), task.getEventId());
        }
    }

    private List<BeltMachineAlgorithmTaskRespDTO> toTaskRespList(List<BeltMachineAlgorithmTask> tasks) {
        List<BeltMachineAlgorithmTaskRespDTO> list = new ArrayList<>();
        if (tasks == null) {
            return list;
        }
        for (BeltMachineAlgorithmTask task : tasks) {
            list.add(toTaskResp(task));
        }
        return list;
    }

    private BeltMachineAlgorithmTaskRespDTO toTaskResp(BeltMachineAlgorithmTask task) {
        BeltMachineAlgorithmTaskRespDTO resp = new BeltMachineAlgorithmTaskRespDTO();
        resp.setTaskId(task.getId() == null ? null : String.valueOf(task.getId()));
        resp.setTaskNo(task.getTaskNo());
        resp.setExecutionStatus(task.getExecutionStatus());
        resp.setConclusion(task.getConclusion());
        resp.setConclusionType(task.getConclusionType());
        resp.setConfidence(task.getConfidence());
        resp.setModelName(task.getModelName());
        resp.setModelVersion(task.getModelVersion());
        resp.setAlgorithmVersion(task.getAlgorithmVersion());
        resp.setConfigVersion(task.getConfigVersion());
        resp.setInferenceDurationMs(task.getInferenceDurationMs());
        resp.setStartedAt(task.getStartedAt() == null ? null : TS.format(task.getStartedAt()));
        resp.setFinishedAt(task.getFinishedAt() == null ? null : TS.format(task.getFinishedAt()));
        resp.setDetectedCount(countDetections(task.getDetections()));
        resp.setSimulated(Boolean.TRUE.equals(task.getIsSimulated()));
        resp.setErrorCode(task.getErrorCode());
        resp.setErrorMessage(task.getErrorMessage());
        return resp;
    }

    /** 执行是否已到终态：终态任务不再向网关取进度 */
    private boolean isTerminal(String status) {
        if (status == null) {
            return false;
        }
        return BeltMachineStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
            || BeltMachineStatusEnum.AlgorithmStatusEnum.FAILED.name().equals(status)
            || BeltMachineStatusEnum.AlgorithmStatusEnum.TIMEOUT.name().equals(status)
            || BeltMachineStatusEnum.AlgorithmStatusEnum.SKIPPED.name().equals(status)
            || BeltMachineStatusEnum.AlgorithmStatusEnum.MOCK.name().equals(status);
    }

    private boolean isSucceeded(String status) {
        return BeltMachineStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status);
    }

    /** detections JSON → 检出目标数；空或不可解析返回 null（页面显示「暂无」） */
    private Integer countDetections(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return node.isArray() ? node.size() : null;
        } catch (Exception e) {
            log.warn("[BeltMachine] detections JSON 解析失败，detectedCount 置空");
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[BeltMachine] 算法任务 JSON 字段序列化失败，按空存储");
            return null;
        }
    }

    private LocalDateTime parseTimeOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String value) {
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
