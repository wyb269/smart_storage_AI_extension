package com.chinaunicom.smartstorage.admin.controller.groundWater;

import com.chinaunicom.smartstorage.admin.constant.GroundWaterConstant;
import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.RecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.StreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.RerunAlgorithmRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.WaterAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.groundWater.GroundWaterService;
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
 * 地面积水监测管理端接口。
 *
 * <p>全部使用 POST，沿用项目既有风格。分页方法由 {@link QueryByPage} 切面
 * 自动包装为 {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，
 * 不信任前端传入的处理人身份。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Slf4j
@Tag(name = "地面积水监测")
@RestController
@RequestMapping("/groundWater")
public class GroundWaterController extends BaseController {

    @Resource
    private GroundWaterService groundWaterService;

    /**
     * 查询当前用户可见的监测点位状态。
     */
    @Operation(summary = "查询监测点位状态列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = MonitorPointStatusRespDTO.class))))
    @PostMapping(path = "/listMonitorPoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<List<MonitorPointStatusRespDTO>, String> listMonitorPoint() {
        return actionResultService.callBackResult(groundWaterService.listMonitorPoint());
    }

    /**
     * 获取指定监测点位绑定摄像头的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。</p>
     */
    @Operation(summary = "获取视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = StreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid StreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE
        if (streamInfoReqDTO == null || streamInfoReqDTO.getPointId() == null
                || streamInfoReqDTO.getPointId().isBlank()) {
            return actionResultService.callBackResultFail("监测点位标识不能为空");
        }
        return groundWaterService.getStreamInfo(streamInfoReqDTO.getPointId());
    }

    /**
     * 查询首页最近事件。
     */
    @Operation(summary = "查询最近积水事件")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = WaterEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<List<WaterEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? GroundWaterConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(groundWaterService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询，默认按发生时间倒序。
     */
    @Operation(summary = "积水历史分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = WaterEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public Object listEventByCondition(@RequestBody @Validated WaterEventQueryReqDTO query) {
        return groundWaterService.listEventByCondition(query);
    }

    /**
     * 查询事件详情、证据与处理记录。
     */
    @Operation(summary = "查询积水事件详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = WaterEventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<WaterEventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(groundWaterService.getEventDetail(eventId));
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
    @OptLog(title = "地面积水事件查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return groundWaterService.markEventViewed(eventId);
    }

    /* ==================== 算法任务（「重新识别」链路，契约 §1） ==================== */

    /**
     * 对指定事件重新提交算法识别（受理即返回）。
     *
     * <p><b>权限取舍：三个算法端点统一用 VIEW。</b>重跑本质是重新取证——
     * 只新增算法任务行、只回写事件算法侧字段（algorithm_* / confidence /
     * water_type），不产生任何人工结论、不触碰 process_status；
     * 而 PROCESS 权限位本期没有开通的业务，把「重新识别」挂在 PROCESS 上
     * 等于给一个只写算法数据的接口要求人工处置授权，语义错位。</p>
     *
     * <p><b>绝不同步等待算法</b>（AI_HANDOFF 第 24.1 节）：本接口校验事件
     * 在数据范围内后向网关提交、落一行 QUEUED 任务、立即返回任务编号；
     * 前端以 {@code algorithmTask/{taskId}} 轮询进度。</p>
     */
    @Operation(summary = "重新提交算法识别")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = RerunAlgorithmRespDTO.class))))
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<RerunAlgorithmRespDTO, String> rerunAlgorithm(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(groundWaterService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务最新状态（前端轮询端点）。
     *
     * <p>先查库；任务未进终态时向网关取最新产出并落库，成功则同事务回写
     * 事件的 algorithm_task_id / algorithm_status / algorithm_result /
     * confidence / water_type（process_status 不动）。权限口径同
     * {@code rerunAlgorithm}，取 VIEW。</p>
     */
    @Operation(summary = "查询算法任务状态")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = WaterAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<WaterAlgorithmTaskRespDTO, String> algorithmTask(
            @Parameter(description = "算法任务编号")
            @NotBlank(message = "算法任务标识不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(groundWaterService.getAlgorithmTask(taskId));
    }

    /**
     * 查询某事件的全部算法任务，按 started_at 倒序。
     *
     * <p>给详情弹窗的「算法任务」表使用；详情接口本身已内嵌最多 20 条，
     * 本端点用于任务数超过内嵌上限后的按需拉取。权限口径同前两处，取 VIEW。</p>
     */
    @Operation(summary = "查询事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = WaterAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.VIEW + "')")
    public ActionResult<List<WaterAlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(groundWaterService.listAlgorithmTasks(eventId));
    }

    /**
     * 导出当前查询条件下的积水事件。
     *
     * <p>与项目既有 ExcelExportController 一致，采用同步直出方案。</p>
     *
     * <p><b>模板 key 必须用 {@link GroundWaterConstant#EXPORT_BIZ_TYPE}，
     * 不能用 {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 HISTORY / RECENT），而 {@code ExcelExportConfig.config} 的 key 是报表模板标识；
     * 把 source 当模板 key 会得到 null 然后 NPE。</p>
     */
    @Operation(summary = "导出积水记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "地面积水记录导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + GroundWaterConstant.Permission.FEATURE
            + "','" + GroundWaterConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid WaterExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = groundWaterService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, GroundWaterConstant.EXPORT_BIZ_TYPE);
    }
}
