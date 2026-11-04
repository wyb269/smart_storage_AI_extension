package com.chinaunicom.smartstorage.admin.controller.pestDetection;

import com.chinaunicom.smartstorage.admin.constant.PestDetectionConstant;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.RecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.StreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.pestDetection.PestDetectionService;
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
 * 粮面虫害智能识别管理端接口。
 *
 * <p>全部使用 POST，沿用项目既有风格。分页方法由 {@link QueryByPage} 切面
 * 自动包装为 {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，
 * 不信任前端传入的处理人身份。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Slf4j
@Tag(name = "粮面虫害智能识别")
@RestController
@RequestMapping("/pestDetection")
public class PestDetectionController extends BaseController {

    @Resource
    private PestDetectionService pestDetectionService;

    /**
     * 查询当前用户可见的粮面点位状态。
     */
    @Operation(summary = "查询粮面点位状态列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = MonitorPointRespDTO.class))))
    @PostMapping(path = "/listMonitorPoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<List<MonitorPointRespDTO>, String> listMonitorPoint() {
        return actionResultService.callBackResult(pestDetectionService.listMonitorPoint());
    }

    /**
     * 获取指定粮面点位绑定摄像头的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。</p>
     */
    @Operation(summary = "获取视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = StreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<StreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid StreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE
        if (streamInfoReqDTO == null || streamInfoReqDTO.getPointId() == null
                || streamInfoReqDTO.getPointId().isBlank()) {
            return actionResultService.callBackResultFail("点位标识不能为空");
        }
        return pestDetectionService.getStreamInfo(streamInfoReqDTO.getPointId());
    }

    /**
     * 查询首页最近检测记录。
     */
    @Operation(summary = "查询最近检测记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PestEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<List<PestEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid RecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? PestDetectionConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(pestDetectionService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询，默认按采样时间倒序。
     */
    @Operation(summary = "虫害检测记录历史分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PestEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public Object listEventByCondition(@RequestBody @Validated PestEventQueryReqDTO query) {
        return pestDetectionService.listEventByCondition(query);
    }

    /**
     * 查询检测记录详情、证据与处理记录。
     */
    @Operation(summary = "查询检测记录详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PestEventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<PestEventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "记录标识")
            @NotBlank(message = "记录标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(pestDetectionService.getEventDetail(eventId));
    }

    /**
     * 查看检测记录后自动标记为已处理。
     *
     * <p>前端在打开详情后调用本接口。仅当事件处于 PENDING 时写入，重复调用无副作用。
     * 处理人由后端按登录态记录。</p>
     *
     * <p>刻意不接受前端传入的处理人、状态与意见，参数只有记录标识。</p>
     */
    @Operation(summary = "查看后标记为已处理")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "虫害检测记录查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "记录标识")
            @NotBlank(message = "记录标识不能为空") @PathVariable String eventId) {
        return pestDetectionService.markEventViewed(eventId);
    }

    /* ==================== 算法任务（「重新识别」） ==================== */

    /**
     * 对指定检测记录提交「重新识别」。
     *
     * <p><b>权限取舍</b>：三个算法端点一律用 VIEW，不开 PROCESS。
     * 重跑本质是<b>重新取证</b>：只追加算法任务行并刷新算法侧结论列，
     * 不产生任何人工结论、不推进 process_status；
     * 而 PROCESS 键当前没有对外开通（查看即标记的产品口径），
     * 用它挡住重新取证会把「补证据」这条合法路径堵死。</p>
     *
     * <p>异步语义：立即返回 taskId，结果由前端轮询 {@code algorithmTask/{taskId}}，
     * 请求线程内不同步等待推理。</p>
     */
    @Operation(summary = "提交重新识别")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PestAlgorithmRerunRespDTO.class))))
    @OptLog(title = "虫害检测记录重新识别", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<PestAlgorithmRerunRespDTO, String> rerunAlgorithm(
            @Parameter(description = "记录标识")
            @NotBlank(message = "记录标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(pestDetectionService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务最新状态（前端轮询用）。
     *
     * <p>未进终态时向后端算法网关取最新并落库；任务成功时同事务回写
     * 事件的算法列与结论列，不触碰 process_status。权限同 {@code rerunAlgorithm}，VIEW。</p>
     */
    @Operation(summary = "查询算法任务")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PestAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<PestAlgorithmTaskRespDTO, String> getAlgorithmTask(
            @Parameter(description = "算法任务编号")
            @NotBlank(message = "算法任务标识不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(pestDetectionService.getAlgorithmTask(taskId));
    }

    /**
     * 查询某事件的历史算法任务，按开始时间倒序。
     *
     * <p>详情弹窗任务表的数据已由 {@code getEventDetail} 一次带回；
     * 本接口保留给「查看更多历史任务」与联调核对。权限同上，VIEW。</p>
     */
    @Operation(summary = "查询事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = PestAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.VIEW + "')")
    public ActionResult<List<PestAlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "记录标识")
            @NotBlank(message = "记录标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(pestDetectionService.listAlgorithmTasks(eventId));
    }

    /**
     * 导出当前查询条件下的检测记录。
     *
     * <p>与项目既有 ExcelExportController 一致，采用同步直出方案。</p>
     *
     * <p><b>模板 key 必须用 {@link PestDetectionConstant#EXPORT_BIZ_TYPE}，
     * 不能用 {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 HISTORY / RECENT），而 {@code ExcelExportConfig.config} 的 key 是报表模板标识；
     * 把 source 当模板 key 会得到 null 然后 NPE。</p>
     */
    @Operation(summary = "导出检测记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "虫害检测记录导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + PestDetectionConstant.Permission.FEATURE
            + "','" + PestDetectionConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid PestExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = pestDetectionService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, PestDetectionConstant.EXPORT_BIZ_TYPE);
    }
}
