package com.chinaunicom.smartstorage.admin.controller.videoOcclusion;

import com.chinaunicom.smartstorage.admin.constant.VideoOcclusionConstant;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.RecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.StreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.RerunAlgorithmRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion.VideoOcclusionService;
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
 * 视频遮挡检测管理端接口。
 *
 * <p>全部使用 POST，沿用项目既有风格。分页方法由 {@link QueryByPage} 切面
 * 自动包装为 {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，
 * 不信任前端传入的处理人身份。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Slf4j
@Tag(name = "视频遮挡检测")
@RestController
@RequestMapping("/videoOcclusion")
public class VideoOcclusionController extends BaseController {

    @Resource
    private VideoOcclusionService videoOcclusionService;

    /**
     * 查询当前用户可见的摄像头状态。
     */
    @Operation(summary = "查询摄像头状态列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = CameraStatusRespDTO.class))))
    @PostMapping(path = "/listCameraStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<List<CameraStatusRespDTO>, String> listCameraStatus() {
        return actionResultService.callBackResult(videoOcclusionService.listCameraStatus());
    }

    /**
     * 获取指定摄像头的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。</p>
     */
    @Operation(summary = "获取视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = StreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid StreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE
        if (streamInfoReqDTO == null || streamInfoReqDTO.getCameraId() == null
                || streamInfoReqDTO.getCameraId().isBlank()) {
            return actionResultService.callBackResultFail("摄像头标识不能为空");
        }
        return videoOcclusionService.getStreamInfo(streamInfoReqDTO.getCameraId());
    }

    /**
     * 查询首页最近事件。
     */
    @Operation(summary = "查询最近遮挡事件")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = OcclusionEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<List<OcclusionEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? VideoOcclusionConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(videoOcclusionService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询，默认按发生时间倒序。
     */
    @Operation(summary = "遮挡历史分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = OcclusionEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public Object listEventByCondition(@RequestBody @Validated OcclusionEventQueryReqDTO query) {
        return videoOcclusionService.listEventByCondition(query);
    }

    /**
     * 查询事件详情、证据与处理记录。
     */
    @Operation(summary = "查询遮挡事件详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = OcclusionEventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<OcclusionEventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(videoOcclusionService.getEventDetail(eventId));
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
    @OptLog(title = "视频遮挡事件查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return videoOcclusionService.markEventViewed(eventId);
    }

    /**
     * 导出当前查询条件下的遮挡事件。
     *
     * <p>与项目既有 ExcelExportController 一致，采用同步直出方案。</p>
     *
     * <p><b>模板 key 必须用 {@link VideoOcclusionConstant#EXPORT_BIZ_TYPE}，
     * 不能用 {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 HISTORY / RECENT），而 {@code ExcelExportConfig.config} 的 key 是报表模板标识；
     * 把 source 当模板 key 会得到 null 然后 NPE。</p>
     */
    @Operation(summary = "导出遮挡记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "视频遮挡记录导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid OcclusionExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = videoOcclusionService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, VideoOcclusionConstant.EXPORT_BIZ_TYPE);
    }

    /* ==================== 算法任务 / 重新识别（契约 §1） ==================== */
    // 归入「查看/取证」能力而非 PROCESS：重跑只刷新算法结论，不产生人工处理结论。
    // 写入的算法任务一律 is_simulated=TRUE，页面必须显示「模拟」标识。

    /**
     * 对某事件发起「重新识别」：提交算法任务、落一行受理记录后立即返回，
     * 由前端轮询 {@link #getAlgorithmTask(String)} 获取进度（不同步长等）。
     */
    @Operation(summary = "重新识别（提交算法任务）")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = RerunAlgorithmRespDTO.class))))
    @OptLog(title = "视频遮挡重新识别", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<RerunAlgorithmRespDTO, String> rerunAlgorithm(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(videoOcclusionService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务最新进度（前端轮询入口）。
     */
    @Operation(summary = "查询算法任务")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = OcclusionAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<OcclusionAlgorithmTaskRespDTO, String> getAlgorithmTask(
            @Parameter(description = "算法任务编号")
            @NotBlank(message = "算法任务标识不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(videoOcclusionService.getAlgorithmTask(taskId));
    }

    /**
     * 列出某事件的全部算法任务（按开始时间倒序）。
     */
    @Operation(summary = "查询事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = OcclusionAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + VideoOcclusionConstant.Permission.FEATURE
            + "','" + VideoOcclusionConstant.Permission.VIEW + "')")
    public ActionResult<List<OcclusionAlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(videoOcclusionService.listAlgorithmTasks(eventId));
    }
}
