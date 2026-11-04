package com.chinaunicom.smartstorage.admin.service.impl.doorAnomaly;

import com.chinaunicom.smartstorage.admin.constant.DoorAnomalyConstant;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorAnomalyExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventProcessReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskOutputDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiAlgorithmTask;
import com.chinaunicom.smartstorage.admin.enumbean.DoorAnomalyStatusEnum;
import com.chinaunicom.smartstorage.admin.service.impl.aivision.AiVisionScopeResolver;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.DoorAnomalyRepository;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.DoorAnomalyService;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.algorithm.AlgorithmGateway;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * 仓门异动主业务实现。
 *
 * <p>依据 AI_HANDOFF.md 第 23 节分层：Controller 只做参数校验与统一响应，
 * 本类负责业务状态组合、事件查询、人工处理与并发控制。</p>
 *
 * <p><b>安全要求</b>：</p>
 * <ol>
 *   <li>处理人从登录态获取，不信任前端传入（第 20.6、29.4 节）。</li>
 *   <li>使用事件版本号防止并发覆盖，冲突时拒绝并提示
 *       「事件已被其他用户处理」（第 20.6、31 节）。</li>
 *   <li>算法结论不得被人工处理改写（第 23 节）。</li>
 *   <li>不返回摄像头凭据（第 29.1 节）。</li>
 *   <li><b>P0-6</b>：所有读写路径统一注入并校验
 *       {@link AiVisionDataScope} 数据范围，禁止跨组织越权。</li>
 *   <li><b>P1-8</b>：写操作必须要求已认证用户，
 *       无法解析登录态时<b>直接拒绝</b>，不回退 anonymous。</li>
 *   <li><b>P1-7</b>：状态更新与审计记录写入置于<b>同一事务</b>，
 *       避免出现「状态已变但无审计」。</li>
 * </ol>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Service
public class DoorAnomalyServiceImpl implements DoorAnomalyService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private DoorAnomalyRepository doorAnomalyRepository;

    @Resource
    private MediaGateway mediaGateway;

    @Resource
    private ActionResultService actionResultService;

    @Resource
    private AiVisionScopeResolver scopeResolver;

    /**
     * 算法适配层网关（当前唯一实现为 MockAlgorithmGateway）。
     *
     * <p>契约 §1 把预留的接缝接成真实可交互的「重新识别」：本类<b>只新增调用点与持久化</b>，
     * 不改网关算法逻辑本体；真实算法到达时仍只是换一个实现类（第 24.5 节）。</p>
     */
    @Resource
    private AlgorithmGateway algorithmGateway;

    /** 解析 detections JSON 求目标框数量；不引入新依赖，复用 Spring 自带的 Jackson。 */
    private static final ObjectMapper ALGO_JSON = new ObjectMapper();

    /* ==================== 认证与数据范围 ==================== */

    /**
     * 解析当前登录用户，构造数据范围。
     *
     * <p>授权来源与失败即拒绝语义由 {@link AiVisionScopeResolver} 统一实现，
     * 本类不再自行拼装范围，避免出现「结构到位但维度取不到值」的分支。</p>
     *
     * <p><b>读路径</b>：无法解析登录态时返回拒绝全部数据的范围，
     * 即 {@code authenticated=false} 且仓库集合为空。</p>
     *
     * <p><b>写路径</b>：必须通过 {@link #requireAuthenticatedScope()} 校验，
     * 匿名直接拒绝，不再回退 anonymous（P1-8）。</p>
     */
    private AiVisionDataScope resolveDataScope() {
        return scopeResolver.resolve();
    }

    /**
     * 解析**已认证**数据范围，用于写操作。
     *
     * <p>无法解析认证用户时直接拒绝，不再回退 anonymous（P1-8）。</p>
     *
     * @throws CustomException 未认证时抛出
     */
    private AiVisionDataScope requireAuthenticatedScope() {
        AiVisionDataScope scope = resolveDataScope();
        if (!scope.isAuthenticated()) {
            // 不输出登录 ID、token 等敏感信息
            log.warn("[DoorAnomaly] 写操作被拒绝：请求未携带有效登录态");
            throw new CustomException("未登录或登录已失效，请重新登录后再操作");
        }
        return scope;
    }

    /**
     * 获取当前登录用户标识（写路径）。
     *
     * <p>未认证时直接拒绝，绝不回退 anonymous（P1-8）。</p>
     */
    private String resolveCurrentUser(AiVisionDataScope scope) {
        if (scope == null || !scope.isAuthenticated()) {
            throw new CustomException("未登录或登录已失效，请重新登录后再操作");
        }
        if (scope.getUserName() != null && !scope.getUserName().isBlank()) {
            return scope.getUserName();
        }
        return scope.getUserId();
    }

    /* ==================== 仓门状态 ==================== */

    @Override
    public List<DoorStatusRespDTO> listDoorStatus() {
        // P0-6：注入数据范围，禁止空组织/区域条件查询全部仓门
        return doorAnomalyRepository.listDoorStatus(resolveDataScope());
    }

    /* ==================== 播放信息 ==================== */

    @Override
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(String doorId) {
        if (doorId == null || doorId.isBlank()) {
            throw new CustomException("仓门标识不能为空");
        }

        AiVisionDataScope scope = resolveDataScope();

        // 从仓门列表中定位摄像头映射（第27.2节），同样受数据范围约束
        DoorStatusRespDTO door = doorAnomalyRepository.listDoorStatus(scope).stream()
            .filter(d -> doorId.equals(d.getDoorId()))
            .findFirst()
            .orElse(null);

        StreamInfoRespDTO dto = new StreamInfoRespDTO();
        dto.setDoorId(doorId);

        if (door == null) {
            // 不区分「不存在」与「无权限」，避免泄露对象是否存在（第31节）
            dto.setCanPlay(false);
            dto.setProtocol(DoorAnomalyStatusEnum.StreamProtocolEnum.MP4.name());
            dto.setUnavailableReason("DOOR_NOT_FOUND");
            return actionResultService.callBackResult(dto);
        }

        dto.setCameraId(door.getCameraId());
        dto.setProtocol(DoorAnomalyStatusEnum.StreamProtocolEnum.MP4.name());

        // 设备离线时直接返回不可播放（第31节）
        if (!DoorAnomalyStatusEnum.DeviceStatusEnum.ONLINE.name().equals(door.getDeviceStatus())) {
            dto.setCanPlay(false);
            dto.setUnavailableReason("CAMERA_OFFLINE");
            return actionResultService.callBackResult(dto);
        }

        // 由媒体网关签发短时地址；此处不构造任何真实摄像头地址
        MediaGateway.PlaybackRequest request = new MediaGateway.PlaybackRequest(
            doorId, door.getCameraId(), door.getWarehouseId(), null, null, null);

        MediaGateway.PlaybackGrant grant = mediaGateway.signPlayback(request);
        dto.setCanPlay(grant.canPlay());
        dto.setPlayableUrl(grant.playableUrl());
        dto.setExpiresAt(grant.expiresAt());
        if (grant.protocol() != null) {
            dto.setProtocol(grant.protocol());
        }
        dto.setUnavailableReason(grant.unavailableReason());

        // 第29.1节：日志不得完整打印播放地址
        log.info("[DoorAnomaly] 签发播放地址 doorId={} protocol={} canPlay={}",
            doorId, dto.getProtocol(), dto.getCanPlay());

        return actionResultService.callBackResult(dto);
    }

    /* ==================== 最近事件 ==================== */

    @Override
    public List<AnomalyEventRespDTO> listRecentEvent(int limit) {
        // 首页建议 5 条（第16.5节），服务端限制上限防止被滥用
        int safeLimit = Math.min(Math.max(limit, 1), DoorAnomalyConstant.MAX_RECENT_EVENT_LIMIT);
        return doorAnomalyRepository.listRecentEvent(resolveDataScope(), safeLimit);
    }

    /* ==================== 历史分页 ==================== */

    @Override
    public List<AnomalyEventRespDTO> listEventByCondition(EventQueryReqDTO query) {
        // 分页由 @QueryByPage 切面处理，此处仅返回查询结果
        return doorAnomalyRepository.listEventByCondition(resolveDataScope(), query);
    }

    @Override
    public List<AnomalyEventRespDTO> listEventByConditionForExport(DoorAnomalyExportReqDTO exportReq) {
        if (exportReq == null) {
            throw new CustomException("导出条件不能为空");
        }

        EventQueryReqDTO query = new EventQueryReqDTO();
        query.setEventNo(exportReq.getEventNo());
        query.setWarehouseId(parseId(exportReq.getWarehouseId(), "仓库标识"));
        query.setDoorId(parseId(exportReq.getDoorId(), "仓门标识"));
        query.setCameraCode(exportReq.getCameraCode());
        query.setAnomalyType(exportReq.getAnomalyType());
        query.setProcessStatus(exportReq.getProcessStatus());
        query.setStartTime(parseTime(exportReq.getStartTime()));
        query.setEndTime(parseTime(exportReq.getEndTime()));

        // 单次导出条数限制（第29.5节）
        int limit = exportReq.getLimit() == null
            ? DoorAnomalyConstant.EXPORT_MAX_ROWS
            : Math.min(exportReq.getLimit(), DoorAnomalyConstant.EXPORT_MAX_ROWS);

        // P0-6：导出同样受数据范围约束，防止越权批量导出
        AiVisionDataScope scope = resolveDataScope();

        // 仅记录条数与来源，不记录筛选值中的敏感内容
        log.info("[DoorAnomaly] 导出事件 source={} limit={} authenticated={}",
            exportReq.getSource(), limit, scope.isAuthenticated());

        List<AnomalyEventRespDTO> rows = doorAnomalyRepository.listEventForExport(scope, query, limit);
        // 导出表面向业务人员，处理状态按产品口径折叠成两态；接口仍回原始编码由前端映射
        rows.forEach(row -> row.setProcessStatusName(
            DoorAnomalyStatusEnum.ProcessStatusEnum.PENDING.name().equals(row.getProcessStatus())
                ? "未处理" : "已处理"));
        return rows;
    }

    /* ==================== 事件详情 ==================== */

    @Override
    public EventDetailRespDTO getEventDetail(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // P0-6：详情必须带数据范围校验，不能只按事件 ID 裸查
        AiVisionDataScope scope = resolveDataScope();
        EventDetailRespDTO detail = doorAnomalyRepository.getEventDetail(scope, eventId);
        if (detail == null) {
            // 不区分不存在与无权限，避免泄露对象信息
            throw new CustomException("事件不存在或无权查看");
        }

        // 处理记录为追加式，返回全部历史（第17.6节）
        List<ProcessRecordRespDTO> records = doorAnomalyRepository.listProcessRecords(scope, eventId);
        detail.setProcessRecords(new ArrayList<>(records));

        // 证据地址通过媒体网关签发短时地址（第26.2节）
        if (detail.getEvidences() != null) {
            for (EventEvidenceRespDTO evidence : detail.getEvidences()) {
                grantEvidenceAddresses(evidence);
            }
        }

        // 算法任务历史（契约 §1）：随详情一次性返回，前端不再单独请求。
        attachAlgorithmTasks(detail, scope, eventId);

        return detail;
    }

    /**
     * 给详情对象挂上「算法任务」历史与最新任务摘要（契约 §1、§3）。
     *
     * <p>读失败不影响详情主体展示：算法区是附加信息，缺数据时以空数组兜底。</p>
     */
    private void attachAlgorithmTasks(EventDetailRespDTO detail, AiVisionDataScope scope, String eventId) {
        List<AiAlgorithmTask> tasks;
        try {
            tasks = doorAnomalyRepository.listAlgorithmTasksByEvent(
                eventId, scope, DoorAnomalyConstant.ALGORITHM_TASK_HISTORY_LIMIT);
        } catch (RuntimeException e) {
            // 内存兜底模式或数据源异常时不把详情整体打挂
            log.warn("[DoorAnomaly] 算法任务历史读取失败 eventId={}: {}", eventId, e.getMessage());
            tasks = List.of();
        }
        List<AlgorithmTaskRespDTO> views = new ArrayList<>();
        for (AiAlgorithmTask t : tasks) {
            views.add(toTaskView(t));
        }
        detail.setAlgorithmTasks(views);

        // 最新任务：优先取事件当前关联（algorithm_task_no），否则取列表首条（已按开始时间倒序）
        AlgorithmTaskRespDTO latest = null;
        if (!views.isEmpty()) {
            String linked = detail.getAlgorithmTaskNo();
            latest = views.stream()
                .filter(v -> linked != null && linked.equals(v.getTaskNo()))
                .findFirst()
                .orElse(views.get(0));
        }
        detail.setLatestAlgorithmTask(latest);
    }

    /**
     * 为证据签发短时地址。
     *
     * <p>数据库中存的是<b>受控对象存储键</b>（如 {@code mock://evidence/1006/normal}），
     * 由 {@link MediaGateway} 换成短时地址后才返回前端（第 26.2 节）。</p>
     *
     * <p>原始资源与算法标注资源<b>分别签发</b>，互不影响（第 17.4 节）。
     * 标注图不存在时保持为 null，不做任何占位。</p>
     */
    private void grantEvidenceAddresses(EventEvidenceRespDTO evidence) {
        // 原始资源
        if (isControlledReference(evidence.getOriginalUrl())) {
            MediaGateway.EvidenceGrant grant = mediaGateway.signEvidence(
                evidence.getOriginalUrl(), DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES);
            evidence.setOriginalUrl(grant.url());
        }

        // 算法标注资源：与原始资源分离，缺失时不补位
        if (isControlledReference(evidence.getAnnotatedUrl())) {
            MediaGateway.EvidenceGrant grant = mediaGateway.signEvidence(
                evidence.getAnnotatedUrl(), DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES);
            evidence.setAnnotatedUrl(grant.url());
        }

        // 缩略图
        if (isControlledReference(evidence.getThumbnailUrl())) {
            MediaGateway.EvidenceGrant grant = mediaGateway.signEvidence(
                evidence.getThumbnailUrl(), DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES);
            evidence.setThumbnailUrl(grant.url());
        }
    }

    /**
     * 判断是否为受控对象存储引用。
     *
     * <p>只接受受控协议前缀（如 {@code mock://}、{@code s3://}）；
     * <b>拒绝</b>任意 http(s) URL 与本地路径，防止越权访问
     * （第 29.3 节）。</p>
     */
    private boolean isControlledReference(String ref) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        String lower = ref.toLowerCase();
        // 明确拒绝任意外部地址
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return false;
        }
        // 拒绝本地绝对路径与路径穿越
        if (ref.contains("..") || ref.startsWith("/") || ref.contains("\\")) {
            return false;
        }
        return ref.contains("://");
    }

    /* ==================== 人工处理 ==================== */

    /**
     * 提交人工处理。
     *
     * <p><b>P1-7 原子性归属</b>：状态更新与审计记录必须同事务，
     * 该事务由 {@link DoorAnomalyRepository#processEvent} 在自己那一层声明并持有。
     * 事务边界放在仓储侧而不是这里，原因见下。</p>
     *
     * <p><b>本方法上刻意不加 {@code @Transactional}</b>：
     * 默认事务管理器绑定<b>主库</b>，而本方法读写的是独立库
     * {@code smart_storage_ai_extension}。跨事务管理器时内层不会加入外层事务，
     * 因此早先「Service 上一个 {@code @Transactional} 就算修复了 P1-7」的写法
     * 实际仍是两次独立提交；它还会在每次处理请求时白白占用一个主库连接
     * （连接池 {@code leak-detection-threshold} 为 60 秒）。
     * 也不能改成按名指定 {@code aiVisionTransactionManager}——该 Bean
     * 仅在 {@code enabled=true} 时存在，内存兜底模式下首次调用即抛
     * {@code NoSuchBeanDefinitionException}。</p>
     */
    @Override
    public ActionResult<String, String> processEvent(String eventId, EventProcessReqDTO req) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }
        if (req == null) {
            throw new CustomException("处理请求不能为空");
        }

        // P1-8：写操作必须已认证，匿名直接拒绝
        AiVisionDataScope scope = requireAuthenticatedScope();

        // P0-6：详情查询同样带数据范围，越权时按「不存在」处理
        EventDetailRespDTO existing = doorAnomalyRepository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权处理");
        }

        // 终态事件不允许重复处理（第16.5节）
        DoorAnomalyStatusEnum.ProcessStatusEnum current;
        try {
            current =
                DoorAnomalyStatusEnum.ProcessStatusEnum.valueOf(existing.getProcessStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            // 存量脏数据不应导致 500，向前端给出可理解的提示
            log.warn("[DoorAnomaly] 事件处理状态非法 eventId={}", eventId);
            throw new CustomException("事件处理状态异常，请联系管理员");
        }
        if (!current.isProcessable()) {
            throw new CustomException("该事件已完成核实，无法重复处理");
        }

        // 处理人从登录态获取，不信任前端传入（第20.6、29.4节）
        String processedBy = resolveCurrentUser(scope);

        // 乐观锁 + 数据范围条件更新 + 追加审计记录，三者在一个事务里（第20.6、28、17.6节）
        int affected = doorAnomalyRepository.processEvent(
            scope, eventId, req.getEventVersion(), current.name(), req, processedBy);

        // 影响行数必须恰为 1；非 1 一律按并发冲突处理且**不写审计**
        if (affected != 1) {
            log.info("[DoorAnomaly] 事件处理冲突 eventId={} 提交版本={} 影响行数={}",
                eventId, req.getEventVersion(), affected);
            throw new CustomException("事件已被其他用户处理，请刷新后重试");
        }

        log.info("[DoorAnomaly] 事件处理成功 eventId={} by={} result={}",
            eventId, processedBy, req.getProcessResult());

        return actionResultService.callBackResult("处理成功");
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>状态模型已收敛为两态：{@code PENDING} = 未处理，其余 = 已处理
     * （前端 {@code PROCESS_STATUS_LABEL} 本就是这么映射的）。
     * 因此这里只把 {@code PENDING} 写成 {@code CLOSED}（终态），
     * 不引入新枚举值、不改表结构，存量 5 态数据自动归入「已处理」。</p>
     *
     * <p>刻意不加 {@code @Transactional}：唯一的写入是仓储内部那一次
     * 已经自带事务的原子操作，在这里再套一层主库事务只会白占连接。</p>
     */
    @Override
    public ActionResult<String, String> markEventViewed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CustomException("事件标识不能为空");
        }

        // 与人工处理一致：必须已认证，且受数据范围约束
        AiVisionDataScope scope = requireAuthenticatedScope();

        EventDetailRespDTO existing = doorAnomalyRepository.getEventDetail(scope, eventId);
        if (existing == null) {
            throw new CustomException("事件不存在或无权查看");
        }

        // 幂等：已是「已处理」时直接返回，不追加审计记录
        final String pending = DoorAnomalyStatusEnum.ProcessStatusEnum.PENDING.name();
        if (!pending.equals(existing.getProcessStatus())) {
            return actionResultService.callBackResult("该事件已是已处理状态");
        }

        String processedBy = resolveCurrentUser(scope);

        EventProcessReqDTO req = new EventProcessReqDTO();
        req.setEventVersion(existing.getEventVersion());
        req.setProcessResult(DoorAnomalyStatusEnum.ProcessStatusEnum.CLOSED.name());
        req.setProcessOpinion(DoorAnomalyConstant.AUTO_VIEW_OPINION);

        int affected = doorAnomalyRepository.processEvent(
            scope, eventId, existing.getEventVersion(), pending, req, processedBy);

        if (affected != 1) {
            log.info("[DoorAnomaly] 查看自动标记未生效（并发或状态已变）eventId={}", eventId);
            throw new CustomException("事件状态已变化，请刷新后重试");
        }

        log.info("[DoorAnomaly] 查看后自动标记为已处理 eventId={} by={}", eventId, processedBy);
        return actionResultService.callBackResult("已标记为已处理");
    }

    /**
     * 把导出条件里的标识字符串转为数值。
     *
     * <p>{@code warehouse_id} / {@code door_id} 在库中是 BIGINT，
     * 非数值输入必须给出可理解的提示，而不是让 SQL 报类型错误变成 500。</p>
     *
     * @return 空输入返回 null（表示不参与筛选）
     * @throws CustomException 非空但不是合法数值
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
     * <p><b>解析失败必须报错，不能返回 null。</b>null 会让 {@code startTime}/
     * {@code endTime} 条件整体消失，用户以为导出的是筛选出的 7 天，
     * 实际拿到的是全量历史（最多 5000 条）——数据错了 yet 毫无提示。
     * 历史查询走的是 {@code LocalDateTime} 字段由框架严格解析，
     * 只有导出路径是字符串，所以只在这里收紧。</p>
     *
     * @return 空输入返回 null（表示不加时间筛选）
     * @throws CustomException 非空但格式不合法
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

    /**
     * 供测试与上层复用的空值判断工具。
     *
     * <p>避免在多处重复编写 null/blank 判断，保持实现一致。</p>
     */
    static boolean hasText(String value) {
        return !Objects.isNull(value) && !value.isBlank();
    }

    /* ==================== 算法任务 / 重新识别（契约 §1） ==================== */

    @Override
    public AlgorithmRerunRespDTO rerunAlgorithm(String eventId) {
        if (!hasText(eventId)) {
            throw new CustomException("事件标识不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();

        // 校验事件存在且在数据范围内；越权与不存在一律按「不存在」处理（第31节）
        EventDetailRespDTO event = doorAnomalyRepository.getEventDetail(scope, eventId);
        if (event == null) {
            throw new CustomException("事件不存在或无权操作");
        }

        // 组装统一算法输入 → 提交 → 立即落一行 QUEUED 任务（is_simulated=TRUE）
        AlgorithmTaskInputDTO input = buildTaskInput(event);
        final String taskNo = algorithmGateway.submitTask(input);

        final LocalDateTime now = LocalDateTime.now();
        AiAlgorithmTask row = new AiAlgorithmTask();
        row.setTaskNo(taskNo);
        row.setCandidateEventId(parseLong(event.getEventId()));
        row.setDoorId(parseLong(event.getDoorId()));
        row.setWarehouseId(parseLong(event.getWarehouseId()));
        row.setCameraId(event.getCameraId());
        row.setTaskType(DoorAnomalyConstant.ALGORITHM_TASK_TYPE);
        row.setMediaType("DUAL_IMAGE");
        row.setMediaRefs(toJsonArray(input.getMediaRefs()));
        row.setNormalReferenceRef(input.getNormalReferenceRef());
        row.setConfidenceThreshold(java.math.BigDecimal.valueOf(0.75));
        row.setConfigVersion(input.getConfigVersion());
        row.setMediaDigest("sha256-" + event.getEventNo());
        row.setTraceId(input.getTraceId());
        // 受理态：等待算法执行；真实结果由前端轮询 algorithmTask/{taskId} 落库
        row.setExecutionStatus(DoorAnomalyStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        row.setStartedAt(now);
        row.setRetryCount(0);
        row.setIsSimulated(Boolean.TRUE);
        row.setCreatedBy(resolveOperatorOrSystem(scope));
        row.setUpdatedBy(row.getCreatedBy());
        doorAnomalyRepository.insertAlgorithmTask(row);

        log.info("[DoorAnomaly] 已受理重新识别 eventId={} taskNo={} doorId={}",
            eventId, taskNo, event.getDoorId());

        AlgorithmRerunRespDTO resp = new AlgorithmRerunRespDTO();
        resp.setTaskId(taskNo);
        resp.setExecutionStatus(DoorAnomalyStatusEnum.AlgorithmStatusEnum.QUEUED.name());
        resp.setSimulated(Boolean.TRUE);
        return resp;
    }

    @Override
    public AlgorithmTaskRespDTO getAlgorithmTask(String taskId) {
        if (!hasText(taskId)) {
            throw new CustomException("算法任务标识不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        AiAlgorithmTask task = doorAnomalyRepository.getAlgorithmTaskByTaskNo(taskId, scope);
        if (task == null) {
            throw new CustomException("算法任务不存在或无权查看");
        }

        // 未进终态时向网关取最新进度并落库；已终态直接返回库存结果
        if (!isTerminal(task.getExecutionStatus())) {
            AlgorithmTaskOutputDTO output = algorithmGateway.queryTask(taskId);
            applyOutput(task, output);
            doorAnomalyRepository.updateAlgorithmTask(task);

            // 成功才回写事件算法维度，不动 process_status、不伪造结论（第23节）
            if (DoorAnomalyStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name()
                    .equals(task.getExecutionStatus()) && task.getCandidateEventId() != null) {
                doorAnomalyRepository.writebackEventAlgorithm(
                    scope, String.valueOf(task.getCandidateEventId()), task.getId(),
                    task.getExecutionStatus(), task.getConclusion(),
                    task.getConfidence(), task.getAnomalyType(), resolveOperatorOrSystem(scope));
                log.info("[DoorAnomaly] 重新识别成功并回写事件 eventId={} taskNo={} conclusion={}",
                    task.getCandidateEventId(), taskId, task.getConclusion());
            }
        }
        return toTaskView(task);
    }

    @Override
    public List<AlgorithmTaskRespDTO> listAlgorithmTasks(String eventId) {
        if (!hasText(eventId)) {
            throw new CustomException("事件标识不能为空");
        }
        AiVisionDataScope scope = resolveDataScope();
        List<AiAlgorithmTask> tasks = doorAnomalyRepository.listAlgorithmTasksByEvent(
            eventId, scope, DoorAnomalyConstant.ALGORITHM_TASK_HISTORY_LIMIT);
        List<AlgorithmTaskRespDTO> views = new ArrayList<>();
        for (AiAlgorithmTask t : tasks) {
            views.add(toTaskView(t));
        }
        return views;
    }

    /* ---------- 算法任务：装配与转换 ---------- */

    /**
     * 由事件构造统一算法输入。
     *
     * <p>媒体引用只使用受控内部键（{@code mock://}），禁止任意 URL（第29.3节）。
     * 本功能一期无实时抓帧能力，沿用事件既有证据引用作为算法输入。</p>
     */
    private AlgorithmTaskInputDTO buildTaskInput(EventDetailRespDTO event) {
        AlgorithmTaskInputDTO input = new AlgorithmTaskInputDTO();
        input.setTaskType(DoorAnomalyConstant.ALGORITHM_TASK_TYPE);
        input.setCandidateEventId(event.getEventId());
        input.setWarehouseId(event.getWarehouseId());
        input.setDoorId(event.getDoorId());
        input.setCameraId(event.getCameraId());
        input.setMediaType("DUAL_IMAGE");
        input.setConfidenceThreshold(java.math.BigDecimal.valueOf(0.75));
        input.setConfigVersion(DoorAnomalyConstant.ALGORITHM_CONFIG_VERSION);
        input.setCapturedAt(event.getOccurredAt());
        input.setTraceId("RERUN-" + event.getEventNo() + "-" + System.currentTimeMillis());

        String doorKey = event.getDoorId() == null ? "unknown" : event.getDoorId();
        input.setMediaRefs(List.of(
            "mock://media/" + doorKey + "/normal",
            "mock://media/" + doorKey + "/abnormal"));
        input.setNormalReferenceRef("mock://media/" + doorKey + "/normal");
        return input;
    }

    /**
     * 把网关输出落到任务行（只更新执行/结论维度列）。
     */
    private void applyOutput(AiAlgorithmTask task, AlgorithmTaskOutputDTO output) {
        if (output == null) {
            return;
        }
        task.setExecutionStatus(output.getExecutionStatus());
        task.setConclusion(output.getConclusion());
        task.setAnomalyType(output.getAnomalyType());
        task.setConfidence(output.getConfidence());
        if (hasText(output.getModelName())) {
            task.setModelName(output.getModelName());
        }
        if (hasText(output.getModelVersion())) {
            task.setModelVersion(output.getModelVersion());
        }
        if (hasText(output.getAlgorithmVersion())) {
            task.setAlgorithmVersion(output.getAlgorithmVersion());
        }
        if (hasText(output.getConfigVersion())) {
            task.setConfigVersion(output.getConfigVersion());
        }
        if (output.getInferenceDurationMs() != null) {
            task.setInferenceDurationMs(output.getInferenceDurationMs());
        }
        task.setStartedAt(parseTs(output.getStartedAt(), task.getStartedAt()));
        task.setFinishedAt(parseTs(output.getFinishedAt(), null));
        if (output.getDetections() != null && !output.getDetections().isEmpty()) {
            try {
                task.setDetections(ALGO_JSON.writeValueAsString(output.getDetections()));
            } catch (Exception e) {
                log.warn("[DoorAnomaly] 检测框序列化失败 taskNo={}: {}", task.getTaskNo(), e.getMessage());
            }
        }
        task.setErrorCode(output.getErrorCode());
        task.setErrorMessage(output.getErrorMessage());
    }

    /** 执行状态是否为终态（终态不再向网关取进度）。 */
    private boolean isTerminal(String status) {
        return DoorAnomalyStatusEnum.AlgorithmStatusEnum.SUCCEEDED.name().equals(status)
            || DoorAnomalyStatusEnum.AlgorithmStatusEnum.FAILED.name().equals(status)
            || DoorAnomalyStatusEnum.AlgorithmStatusEnum.TIMEOUT.name().equals(status)
            || DoorAnomalyStatusEnum.AlgorithmStatusEnum.SKIPPED.name().equals(status)
            || DoorAnomalyStatusEnum.AlgorithmStatusEnum.MOCK.name().equals(status);
    }

    /** 实体 → 对外视图（契约 §1 字段集）。 */
    private AlgorithmTaskRespDTO toTaskView(AiAlgorithmTask task) {
        AlgorithmTaskRespDTO v = new AlgorithmTaskRespDTO();
        v.setTaskId(task.getId() == null ? null : String.valueOf(task.getId()));
        v.setTaskNo(task.getTaskNo());
        v.setExecutionStatus(task.getExecutionStatus());
        v.setConclusion(task.getConclusion());
        v.setAnomalyType(task.getAnomalyType());
        v.setConfidence(task.getConfidence());
        v.setModelName(task.getModelName());
        v.setModelVersion(task.getModelVersion());
        v.setAlgorithmVersion(task.getAlgorithmVersion());
        v.setConfigVersion(task.getConfigVersion());
        v.setInferenceDurationMs(task.getInferenceDurationMs());
        v.setStartedAt(task.getStartedAt() == null ? null : task.getStartedAt().format(TS));
        v.setFinishedAt(task.getFinishedAt() == null ? null : task.getFinishedAt().format(TS));
        v.setDetectedCount(countDetections(task.getDetections()));
        v.setSimulated(Boolean.TRUE.equals(task.getIsSimulated()));
        v.setErrorCode(task.getErrorCode());
        v.setErrorMessage(task.getErrorMessage());
        return v;
    }

    /**
     * 从 detections JSON 求目标框数量；解析失败按 0，绝不抛给前端。
     *
     * <p>兼容 {@code null}/{@code []}/{@code [null]}：仅统计对象元素，
     * 与 07 补种脚本「不产出 null 数组元素」的口径一致。</p>
     */
    private int countDetections(String detections) {
        if (!hasText(detections)) {
            return 0;
        }
        try {
            JsonNode node = ALGO_JSON.readTree(detections);
            if (!node.isArray()) {
                return 0;
            }
            int count = 0;
            for (JsonNode el : node) {
                if (el != null && el.isObject()) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 字符串列表 → JSON 数组文本（媒体引用等受控键）。 */
    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return ALGO_JSON.writeValueAsString(values);
        } catch (Exception e) {
            return null;
        }
    }

    /** 时间解析，失败时回退到兜底值。 */
    private LocalDateTime parseTs(String text, LocalDateTime fallback) {
        if (!hasText(text)) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(text, TS);
        } catch (Exception e) {
            return fallback;
        }
    }

    private Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * VIEW 权限下的写操作记录人。
     *
     * <p>「重新识别」用 VIEW 权限（重跑本质是重新取证，不产生人工结论，见 Controller 注释），
     * 因此允许匿名/未完全解析登录态时调用；此处取登录用户，取不到时记为系统标识，
     * 不回退到前端传入的身份（第29.4节）。</p>
     */
    private String resolveOperatorOrSystem(AiVisionDataScope scope) {
        if (scope != null && scope.isAuthenticated()) {
            if (hasText(scope.getUserName())) {
                return scope.getUserName();
            }
            if (hasText(scope.getUserId())) {
                return scope.getUserId();
            }
        }
        return "algorithm-rerun";
    }
}