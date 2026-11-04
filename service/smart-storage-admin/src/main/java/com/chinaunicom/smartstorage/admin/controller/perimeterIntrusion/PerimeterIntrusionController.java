package com.chinaunicom.smartstorage.admin.controller.perimeterIntrusion;

import com.chinaunicom.smartstorage.admin.constant.PerimeterIntrusionConstant;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.RecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.StreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.PerimeterIntrusionService;
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
 * 周界入侵识别管理端接口。
 *
 * <p>全部使用 POST，沿用项目既有风格。分页方法由 {@link QueryByPage} 切面
 * 自动包装为 {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，
 * 不信任前端传入的处理人身份。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Slf4j
@Tag(name = "周界入侵识别")
@RestController
@RequestMapping("/perimeterIntrusion")
public class PerimeterIntrusionController extends BaseController {

    @Resource
    private PerimeterIntrusionService perimeterIntrusionService;

    /**
     * 查询当前用户可见的防区段状态。
     *
     * <p>返回<b>全量</b>防区段，防区下拉的筛选在前端本地完成：
     * 一次取全量才能让「切换筛选不打断当前取流、默认选中稳定」成立。</p>
     */
    @Operation(summary = "查询防区段状态列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = MonitorPointStatusRespDTO.class))))
    @PostMapping(path = "/listMonitorPoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<List<MonitorPointStatusRespDTO>, String> listMonitorPoint() {
        return actionResultService.callBackResult(perimeterIntrusionService.listMonitorPoint());
    }

    /**
     * 获取指定防区段绑定摄像头的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。</p>
     */
    @Operation(summary = "获取视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = StreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid StreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE
        if (streamInfoReqDTO == null || streamInfoReqDTO.getPointId() == null
                || streamInfoReqDTO.getPointId().isBlank()) {
            return actionResultService.callBackResultFail("防区段标识不能为空");
        }
        return perimeterIntrusionService.getStreamInfo(streamInfoReqDTO.getPointId());
    }

    /**
     * 查询首页最近事件。
     */
    @Operation(summary = "查询最近入侵事件")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PerimeterEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<List<PerimeterEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? PerimeterIntrusionConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(perimeterIntrusionService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询，默认按发生时间倒序。
     *
     * <p>比遮挡那一版多一个 {@code zone} 防区筛选条件。</p>
     */
    @Operation(summary = "入侵历史分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PerimeterEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public Object listEventByCondition(@RequestBody @Validated PerimeterEventQueryReqDTO query) {
        return perimeterIntrusionService.listEventByCondition(query);
    }

    /**
     * 查询事件详情、证据与处理记录。
     */
    @Operation(summary = "查询入侵事件详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PerimeterEventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<PerimeterEventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(perimeterIntrusionService.getEventDetail(eventId));
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>前端在打开事件详情后调用本接口。仅当事件处于未核实时写入，
     * 重复调用无副作用。处理人由后端按登录态记录。</p>
     *
     * <p>刻意不接受前端传入的处理人、状态与意见，参数只有事件标识。</p>
     */
    @Operation(summary = "查看后标记为已处理")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "周界入侵事件查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return perimeterIntrusionService.markEventViewed(eventId);
    }

    /**
     * 导出当前查询条件下的入侵事件。
     *
     * <p>与项目既有 ExcelExportController 一致，采用同步直出方案。</p>
     *
     * <p><b>模板 key 必须用 {@link PerimeterIntrusionConstant#EXPORT_BIZ_TYPE}，
     * 不能用 {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 HISTORY / RECENT），而 {@code ExcelExportConfig.config} 的 key 是报表模板标识；
     * 把 source 当模板 key 会得到 null 然后 NPE。</p>
     *
     * <p>⚠️ 本功能交付时<b>未修改</b> {@code ExcelExportConfig}（属禁改共享件），
     * 因此 {@code perimeterIntrusionEvent} 这个 key 尚未注册，
     * 上线前需由维护者按遮挡那一版的格式补一段，否则本接口会 NPE。</p>
     */
    @Operation(summary = "导出入侵记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "周界入侵记录导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid PerimeterExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = perimeterIntrusionService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, PerimeterIntrusionConstant.EXPORT_BIZ_TYPE);
    }

    /* ==================== 算法任务 / 重新识别 ==================== */

    /**
     * 对指定事件重新发起算法识别。
     *
     * <p>只创建任务并立即返回，不在请求内等待推理（契约 §1）。
     * 权限用 VIEW：重跑本质是重新取证，不产生人工结论；PROCESS 尚未开启。</p>
     */
    @Operation(summary = "重新识别（提交算法任务）")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "任务已受理",
            content = @Content(schema = @Schema(implementation = PerimeterAlgorithmRerunRespDTO.class))))
    @OptLog(title = "周界入侵重新识别", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<PerimeterAlgorithmRerunRespDTO, String> rerunAlgorithm(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(perimeterIntrusionService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务进度与结果。
     *
     * <p>前端按 800ms 轮询本接口直到终态；未进终态时服务端会向网关取最新进度。</p>
     */
    @Operation(summary = "查询算法任务")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PerimeterAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<PerimeterAlgorithmTaskRespDTO, String> algorithmTask(
            @Parameter(description = "算法任务编号")
            @NotBlank(message = "算法任务编号不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(perimeterIntrusionService.getAlgorithmTask(taskId));
    }

    /** 查询某事件的全部算法任务，按开始时间倒序。 */
    @Operation(summary = "查询事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PerimeterIntrusionConstant.Permission.FEATURE
            + "','" + PerimeterIntrusionConstant.Permission.VIEW + "')")
    public ActionResult<List<PerimeterAlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(
            perimeterIntrusionService.listAlgorithmTasks(eventId));
    }
}
