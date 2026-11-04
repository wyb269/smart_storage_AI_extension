package com.chinaunicom.smartstorage.admin.controller.doorAnomaly;

import com.chinaunicom.smartstorage.admin.constant.DoorAnomalyConstant;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorAnomalyExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventProcessReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.RecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.StreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly.DoorAnomalyService;
import com.chinaunicom.smartstorage.admin.utils.UniversalExcelUtil;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仓门异动监测管理端接口。
 *
 * <p>接口路径与用途严格对齐 AI_HANDOFF.md 第 21.1 节，未做改名。
 * 全部使用 POST，沿用项目既有风格（第 21 节）。</p>
 *
 * <p>分页方法由 {@link QueryByPage} 切面自动包装为
 * {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，
 * 不信任前端传入的处理人身份。</p>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Tag(name = "仓门异动监测")
@RestController
@RequestMapping("/doorAnomaly")
public class DoorAnomalyController extends BaseController {

    @Resource
    private DoorAnomalyService doorAnomalyService;

    /**
     * 查询当前用户可见的仓门状态。
     */
    @Operation(summary = "查询仓门状态列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = DoorStatusRespDTO.class))))
    @PostMapping(path = "/listDoorStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<List<DoorStatusRespDTO>, String> listDoorStatus() {
        return actionResultService.callBackResult(doorAnomalyService.listDoorStatus());
    }

    /**
     * 获取指定仓门的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。</p>
     */
    @Operation(summary = "获取仓门视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = StreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid StreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE（第21.1节验收标准）
        if (streamInfoReqDTO == null || streamInfoReqDTO.getDoorId() == null
                || streamInfoReqDTO.getDoorId().isBlank()) {
            return actionResultService.callBackResultFail("仓门标识不能为空");
        }
        return doorAnomalyService.getStreamInfo(streamInfoReqDTO.getDoorId());
    }

    /**
     * 查询首页最近事件。
     */
    @Operation(summary = "查询最近异动事件")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = AnomalyEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<List<AnomalyEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? DoorAnomalyConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(doorAnomalyService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询。
     *
     * <p>默认按发生时间倒序（第 18.3 节）。</p>
     */
    @Operation(summary = "历史记录分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = AnomalyEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public Object listEventByCondition(
            @RequestBody @Validated EventQueryReqDTO eventQueryReqDTO) {
        return doorAnomalyService.listEventByCondition(eventQueryReqDTO);
    }

    /**
     * 查询事件详情、证据与处理记录。
     */
    @Operation(summary = "查询事件详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = EventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<EventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(doorAnomalyService.getEventDetail(eventId));
    }

    /**
     * 人工核实事件。
     *
     * <p><b>当前前端已不再调用本接口</b>（2026-10-21 取消处理按钮，
     * 改为查看即标记，见 {@link #markEventViewed}）。
     * 保留接口与实现，供后续需要真实人工结论时复用；
     * 它仍然做完整的认证、数据范围与乐观锁校验。</p>
     */
    @Operation(summary = "人工核实事件")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "仓门异动事件处理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/processEvent/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.PROCESS + "')")
    public ActionResult<String, String> processEvent(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId,
            @RequestBody @Valid EventProcessReqDTO eventProcessReqDTO) {
        return doorAnomalyService.processEvent(eventId, eventProcessReqDTO);
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>前端在打开事件详情后调用本接口。仅当事件处于未处理时写入，
     * 重复调用无副作用。处理人由后端按登录态记录。</p>
     *
     * <p>刻意不接受前端传入的处理人、状态与意见，参数只有事件标识。</p>
     */
    @Operation(summary = "查看后标记为已处理")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "仓门异动事件查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return doorAnomalyService.markEventViewed(eventId);
    }

    /**
     * 导出当前查询条件下的事件。
     *
     * <p>已确认采用同步直出方案，与项目既有 ExcelExportController 保持一致。</p>
     *
     * <p><b>模板 key 必须用 {@link DoorAnomalyConstant#EXPORT_BIZ_TYPE}，不能用
     * {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 {@code HISTORY} / {@code RECENT}），而 {@code ExcelExportConfig.config}
     * 的 key 是报表模板标识；此前直接把 source 当模板 key，
     * {@code config.get("HISTORY")} 返回 null 后立刻 NPE，
     * 导出功能没有任何一次成功的可能。</p>
     */
    @Operation(summary = "导出事件记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "仓门异动事件导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid DoorAnomalyExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = doorAnomalyService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, DoorAnomalyConstant.EXPORT_BIZ_TYPE);
    }

    /* ==================== 算法任务 / 重新识别（契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §1） ====================
     * 权限取舍：三个端点统一用 VIEW。
     * 「重新识别」本质是对既有取证素材重新跑一次算法、刷新算法结论，
     * 不产生任何人工处理结论（人工处理状态 process_status 独立，第23节），
     * 因此归入「查看/取证」能力而非 PROCESS；且本期 PROCESS 权限尚未开放。
     * 写入的算法任务一律 is_simulated=TRUE（第24.4节），页面必须显示「模拟」标识。
     */

    /**
     * 对某事件发起「重新识别」：提交算法任务、落一行 QUEUED 记录后立即返回。
     *
     * <p>算法不在请求里同步长等（第24.1节），由前端轮询
     * {@link #getAlgorithmTask(String)} 获取进度。</p>
     */
    @Operation(summary = "重新识别（提交算法任务）")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = AlgorithmRerunRespDTO.class))))
    @OptLog(title = "仓门异动重新识别", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<AlgorithmRerunRespDTO, String> rerunAlgorithm(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(doorAnomalyService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务最新进度（前端轮询入口）。
     */
    @Operation(summary = "查询算法任务")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = AlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<AlgorithmTaskRespDTO, String> getAlgorithmTask(
            @Parameter(description = "算法任务编号")
            @NotBlank(message = "算法任务标识不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(doorAnomalyService.getAlgorithmTask(taskId));
    }

    /**
     * 列出某事件的全部算法任务（按开始时间倒序）。
     */
    @Operation(summary = "查询事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = AlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + DoorAnomalyConstant.Permission.FEATURE
            + "','" + DoorAnomalyConstant.Permission.VIEW + "')")
    public ActionResult<List<AlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(doorAnomalyService.listAlgorithmTasks(eventId));
    }
}
