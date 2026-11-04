package com.chinaunicom.smartstorage.admin.controller.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.constant.VehicleRecognitionConstant;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.RecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.StreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition.VehicleRecognitionService;
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
 * 车辆智能识别管理端接口。
 *
 * <p>全部使用 POST，沿用项目既有风格。分页方法由 {@link QueryByPage} 切面
 * 自动包装为 {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，不信任前端传入的核实人身份；
 * 车牌属个人信息，接口原样返回但日志严禁打印完整值（见 Service 掩码说明）。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Slf4j
@Tag(name = "车辆智能识别")
@RestController
@RequestMapping("/vehicleRecognition")
public class VehicleRecognitionController extends BaseController {

    @Resource
    private VehicleRecognitionService vehicleRecognitionService;

    /**
     * 查询当前用户可见的今日识别车辆（左侧列表）。
     *
     * <p>对应遮挡功能里 listCameraStatus 的位置，但车辆功能左侧不是设备状态网格，
     * 而是「今日识别车辆」，故接口名为 listTodayVehicle。</p>
     */
    @Operation(summary = "查询今日识别车辆列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = TodayVehicleRespDTO.class))))
    @PostMapping(path = "/listTodayVehicle", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<List<TodayVehicleRespDTO>, String> listTodayVehicle(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? VehicleRecognitionConstant.DEFAULT_TODAY_VEHICLE_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(vehicleRecognitionService.listTodayVehicle(limit));
    }

    /**
     * 获取指定识别点位摄像头的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。入参为选中识别记录所属的点位标识。</p>
     */
    @Operation(summary = "获取视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = StreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid StreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE
        if (streamInfoReqDTO == null || streamInfoReqDTO.getPointId() == null
                || streamInfoReqDTO.getPointId().isBlank()) {
            return actionResultService.callBackResultFail("识别点位标识不能为空");
        }
        return vehicleRecognitionService.getStreamInfo(streamInfoReqDTO.getPointId());
    }

    /**
     * 查询首页最近识别记录。
     */
    @Operation(summary = "查询最近识别记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = VehicleEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<List<VehicleEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? VehicleRecognitionConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(vehicleRecognitionService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询，默认按识别时间倒序。
     */
    @Operation(summary = "出入事件记录分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = VehicleEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public Object listEventByCondition(@RequestBody @Validated VehicleEventQueryReqDTO query) {
        return vehicleRecognitionService.listEventByCondition(query);
    }

    /**
     * 查询识别记录详情、证据与核实记录。
     */
    @Operation(summary = "查询识别记录详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = VehicleEventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<VehicleEventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "识别记录标识")
            @NotBlank(message = "识别记录标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(vehicleRecognitionService.getEventDetail(eventId));
    }

    /**
     * 查看识别记录后自动标记为已核实。
     *
     * <p>前端在打开记录详情后调用本接口。仅当记录处于待核实时写入，
     * 重复调用无副作用。核实人由后端按登录态记录。</p>
     *
     * <p>刻意不接受前端传入的核实人、状态与意见，参数只有记录标识。</p>
     */
    @Operation(summary = "查看后标记为已核实")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "车辆识别记录查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "识别记录标识")
            @NotBlank(message = "识别记录标识不能为空") @PathVariable String eventId) {
        return vehicleRecognitionService.markEventViewed(eventId);
    }

    /**
     * 导出当前查询条件下的识别记录。
     *
     * <p>与项目既有 ExcelExportController 一致，采用同步直出方案。</p>
     *
     * <p><b>模板 key 必须用 {@link VehicleRecognitionConstant#EXPORT_BIZ_TYPE}，
     * 不能用 {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 HISTORY / RECENT），而 {@code ExcelExportConfig.config} 的 key 是报表模板标识；
     * 把 source 当模板 key 会得到 null 然后 NPE。</p>
     */
    @Operation(summary = "导出识别记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "车辆识别记录导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid VehicleExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = vehicleRecognitionService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, VehicleRecognitionConstant.EXPORT_BIZ_TYPE);
    }

    /* ==================== 算法任务 / 重新识别（契约 §1） ==================== */
    // 归入「查看/取证」能力而非 PROCESS：重跑只刷新算法结论，不产生人工核实结论。
    // 写入任务一律 is_simulated=TRUE，页面必须显示「模拟」标识。

    /**
     * 对某识别记录发起「重新识别」：提交算法任务、落一行受理记录后立即返回，
     * 由前端轮询 {@link #getAlgorithmTask(String)} 获取进度（不同步长等）。
     */
    @Operation(summary = "重新识别（提交算法任务）")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = VehicleAlgorithmRerunRespDTO.class))))
    @OptLog(title = "车辆识别重新识别", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<VehicleAlgorithmRerunRespDTO, String> rerunAlgorithm(
            @Parameter(description = "识别记录标识")
            @NotBlank(message = "识别记录标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(vehicleRecognitionService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务最新进度（前端轮询入口）。
     */
    @Operation(summary = "查询算法任务")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = VehicleAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<VehicleAlgorithmTaskRespDTO, String> getAlgorithmTask(
            @Parameter(description = "算法任务编号")
            @NotBlank(message = "算法任务标识不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(vehicleRecognitionService.getAlgorithmTask(taskId));
    }

    /**
     * 列出某识别记录的全部算法任务（按开始时间倒序）。
     */
    @Operation(summary = "查询事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = VehicleAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VehicleRecognitionConstant.Permission.FEATURE
            + "','" + VehicleRecognitionConstant.Permission.VIEW + "')")
    public ActionResult<List<VehicleAlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "识别记录标识")
            @NotBlank(message = "识别记录标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(vehicleRecognitionService.listAlgorithmTasks(eventId));
    }
}
